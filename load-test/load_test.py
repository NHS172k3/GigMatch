"""
load_test.py — GigMatch load testing harness.

Fires synthetic job-match requests at configurable RPS and reports
p50/p95/p99 latency statistics in real time.

Usage:
  pip install -r requirements.txt
  python load_test.py --rps 200 --duration 30 --url http://localhost:8080
"""

import asyncio
import time
import uuid
import random
import click
import numpy as np
import aiohttp
from rich.console import Console
from rich.table import Table
from rich.live import Live
from rich.progress import Progress, SpinnerColumn, BarColumn, TimeElapsedColumn, TimeRemainingColumn

console = Console()

JOB_CATEGORIES = [
    ("software-dev", ["backend", "api"]),
    ("software-dev", ["backend", "java"]),
    ("design",       ["branding", "ui-ux"]),
    ("data-science", ["ml", "analytics"]),
    ("writing",      ["content", "copywriting"]),
    ("general",      []),
]

BUDGETS_CENTS = [15000, 20000, 25000, 30000, 35000, 40000, 50000, 60000]
URGENCY = ["LOW", "MEDIUM", "HIGH"]

def make_request() -> dict:
    category, skills = random.choice(JOB_CATEGORIES)
    return {
        "requestId":      str(uuid.uuid4()),
        "clientId":       f"load-test-client-{random.randint(1, 500)}",
        "jobTitle":       f"Load test job [{category}]",
        "jobCategory":    category,
        "requiredSkills": skills,
        "budgetCents":    random.choice(BUDGETS_CENTS),
        "urgencyLevel":   random.choice(URGENCY),
    }


async def send_one(session: aiohttp.ClientSession, url: str) -> tuple[float, int, str]:
    """Send one match request. Returns (latency_ms, status_code, outcome)."""
    payload = make_request()
    t0 = time.perf_counter()
    try:
        async with session.post(f"{url}/api/v1/matches", json=payload, timeout=aiohttp.ClientTimeout(total=5)) as resp:
            latency_ms = (time.perf_counter() - t0) * 1000
            body = await resp.json()
            outcome = body.get("outcome", "ERROR") if resp.status == 200 else "HTTP_ERR"
            return latency_ms, resp.status, outcome
    except Exception as e:
        latency_ms = (time.perf_counter() - t0) * 1000
        return latency_ms, 0, "CONN_ERR"


def build_stats_table(latencies: list[float], outcomes: dict, elapsed: float, rps: int) -> Table:
    table = Table(title=f"GigMatch Load Test — {rps} target RPS", show_header=True)
    table.add_column("Metric",    style="bold", width=28)
    table.add_column("Value",     justify="right", width=16)

    if latencies:
        arr = np.array(latencies)
        table.add_row("Requests sent",      str(len(latencies)))
        table.add_row("Actual RPS",         f"{len(latencies)/elapsed:.1f}")
        p95 = np.percentile(arr, 95)
        p99 = np.percentile(arr, 99)
        table.add_row("p50 latency",        f"{np.percentile(arr, 50):.1f}ms")
        table.add_row("p95 latency",
                      f"[yellow]{p95:.1f}ms[/yellow]" if p95 > 150 else f"{p95:.1f}ms")
        table.add_row("p99 latency",
                      f"[red]{p99:.1f}ms[/red]" if p99 > 200 else f"{p99:.1f}ms")
        table.add_row("Max latency",        f"{arr.max():.1f}ms")
        table.add_section()
        for outcome, count in sorted(outcomes.items(), key=lambda x: -x[1]):
            table.add_row(f"  {outcome}", str(count))
        errors = outcomes.get("CONN_ERR", 0) + outcomes.get("HTTP_ERR", 0)
        table.add_row("Error rate",         f"{errors/len(latencies)*100:.1f}%")
    else:
        table.add_row("Requests sent", "0")

    return table


@click.command()
@click.option("--rps",      default=100,                        help="Target requests per second")
@click.option("--duration", default=30,                         help="Test duration in seconds")
@click.option("--url",      default="http://localhost:8080",    help="Backend base URL")
@click.option("--concurrency", default=50,                      help="Max concurrent connections")
def run(rps: int, duration: int, url: str, concurrency: int):
    """GigMatch load test — sends synthetic job-match requests at target RPS."""
    asyncio.run(_run(rps, duration, url, concurrency))


async def _run(rps: int, duration: int, url: str, concurrency: int):
    console.print(f"\n[bold green]GigMatch Load Test[/bold green]")
    console.print(f"  URL:         {url}")
    console.print(f"  Target RPS:  {rps}")
    console.print(f"  Duration:    {duration}s")
    console.print(f"  Concurrency: {concurrency}\n")

    latencies: list[float] = []
    outcomes: dict[str, int] = {}
    semaphore = asyncio.Semaphore(concurrency)
    interval  = 1.0 / rps
    start     = time.perf_counter()

    async def bounded_send(session):
        async with semaphore:
            lat, status, outcome = await send_one(session, url)
            latencies.append(lat)
            outcomes[outcome] = outcomes.get(outcome, 0) + 1

    with Progress(
        SpinnerColumn(),
        BarColumn(),
        "[progress.percentage]{task.percentage:>3.0f}%",
        TimeElapsedColumn(),
        TimeRemainingColumn(),
        console=console
    ) as progress:
        task = progress.add_task("Running...", total=duration * rps)

        async with aiohttp.ClientSession() as session:
            tasks = []
            deadline = start + duration
            while time.perf_counter() < deadline:
                loop_start = time.perf_counter()
                t = asyncio.create_task(bounded_send(session))
                tasks.append(t)
                progress.advance(task)
                elapsed = time.perf_counter() - loop_start
                sleep_for = interval - elapsed
                if sleep_for > 0:
                    await asyncio.sleep(sleep_for)
            await asyncio.gather(*tasks, return_exceptions=True)

    total_elapsed = time.perf_counter() - start
    console.print()
    console.print(build_stats_table(latencies, outcomes, total_elapsed, rps))

    # Final verdict
    if latencies:
        arr   = np.array(latencies)
        p99   = np.percentile(arr, 99)
        if p99 < 200:
            console.print(f"\n[bold green]✓ p99 {p99:.0f}ms < 200ms target — PASS[/bold green]")
        else:
            console.print(f"\n[bold red]✗ p99 {p99:.0f}ms > 200ms target — INVESTIGATE[/bold red]")


if __name__ == "__main__":
    run()
