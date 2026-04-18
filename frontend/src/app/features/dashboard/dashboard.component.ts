import { Component, OnInit, OnDestroy, signal, computed, inject, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { Subscription, timer, switchMap } from 'rxjs';
import { ProviderService } from '../../core/services/provider.service';
import { MatchLogService } from '../../core/services/match-log.service';
import { Provider } from '../../core/models/provider.model';
import { MatchLog } from '../../core/models/match-log.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterLink, MatIconModule, MatButtonModule],
  template: `
    <div class="page-container">

      <!-- Header -->
      <div class="page-header">
        <div class="header-left">
          <h1>Dashboard</h1>
          <p>Real-time dispatch platform overview</p>
        </div>
        <div class="header-live">
          <span class="live-dot"></span>
          <span class="live-label">Live</span>
        </div>
      </div>

      <!-- Stat cards -->
      <div class="card-grid">
        <div class="stat-card">
          <mat-icon class="stat-icon" style="color:#F59E0B">swap_horiz</mat-icon>
          <div class="stat-value">{{ totalMatches() | number }}</div>
          <div class="stat-label">Total Matches</div>
        </div>

        <div class="stat-card">
          <mat-icon class="stat-icon" style="color:#22C55E">attach_money</mat-icon>
          <div class="stat-value">\${{ totalEarnings() | number:'1.0-0' }}</div>
          <div class="stat-label">Total Earnings</div>
        </div>

        <div class="stat-card">
          <mat-icon class="stat-icon" style="color:#00D4AA">business</mat-icon>
          <div class="stat-value">{{ activeCompanies() }}</div>
          <div class="stat-label">Active Companies</div>
        </div>

        <div class="stat-card">
          <mat-icon class="stat-icon" style="color:#F59E0B">timer</mat-icon>
          <div class="stat-value">{{ avgLatency() | number:'1.0-0' }}<span class="stat-unit">ms</span></div>
          <div class="stat-label">Avg Match Latency</div>
        </div>
      </div>

      <!-- Two-column section -->
      <div class="two-col">

        <!-- Company capacity -->
        <div class="panel">
          <div class="panel-header">
            <div class="panel-title-block">
              <span class="panel-title">Company Capacity</span>
              <span class="panel-subtitle">Daily contract slots used</span>
            </div>
          </div>
          <div class="panel-body">
            <div *ngFor="let p of companies()" class="capacity-row">
              <div class="capacity-info">
                <span class="capacity-name">{{ p.name }}</span>
                <span class="capacity-count">{{ p.totalActiveJobs }}<span class="capacity-sep">/</span>{{ p.dailyJobCapacity }}</span>
              </div>
              <div class="capacity-bar-track">
                <div class="capacity-bar-fill"
                     [class]="fillClass(p)"
                     [style.width.%]="(p.totalActiveJobs / p.dailyJobCapacity) * 100">
                </div>
              </div>
              <div class="capacity-footer">
                <span class="capacity-skills">{{ p.skillCategories.join(' · ') }}</span>
                <span class="capacity-pct" [class]="fillClass(p)">{{ (p.totalActiveJobs / p.dailyJobCapacity * 100) | number:'1.0-0' }}%</span>
              </div>
            </div>
            <div *ngIf="!companies().length" class="empty-state">
              <mat-icon>hourglass_empty</mat-icon>
              <span>Loading companies...</span>
            </div>
          </div>
        </div>

        <!-- Win rate panel -->
        <div class="panel">
          <div class="panel-header">
            <div class="panel-title-block">
              <span class="panel-title">Auction Performance</span>
              <span class="panel-subtitle">Based on last {{ recentLogs().length }} auctions</span>
            </div>
          </div>
          <div class="panel-body">
            <div class="perf-grid">
              <div class="perf-metric">
                <div class="perf-value" style="color:#22C55E">{{ winRate() | number:'1.1-1' }}<span class="perf-unit">%</span></div>
                <div class="perf-label">Match Rate</div>
              </div>
              <div class="perf-metric">
                <div class="perf-value" style="color:#F59E0B">{{ avgLatency() | number:'1.0-0' }}<span class="perf-unit">ms</span></div>
                <div class="perf-label">Avg Latency</div>
              </div>
              <div class="perf-metric">
                <div class="perf-value" style="color:#00D4AA">{{ avgPrice() | number:'1.0-0' }}<span class="perf-unit"> $</span></div>
                <div class="perf-label">Avg Clearing Price</div>
              </div>
              <div class="perf-metric">
                <div class="perf-value" style="color:#EF4444">{{ noMatchRate() | number:'1.1-1' }}<span class="perf-unit">%</span></div>
                <div class="perf-label">No Match Rate</div>
              </div>
            </div>

            <!-- Latency benchmark bar -->
            <div class="bench-section">
              <div class="bench-label">
                <span class="muted-text">p99 target</span>
                <span class="terminal-number" style="color:#F59E0B">200ms</span>
              </div>
              <div class="capacity-bar-track">
                <div class="capacity-bar-fill"
                     [class]="avgLatency() < 100 ? 'fill-ok' : avgLatency() < 200 ? 'fill-medium' : 'fill-high'"
                     [style.width.%]="Math.min((avgLatency() / 200) * 100, 100)">
                </div>
              </div>
              <div class="bench-hint muted-text">{{ avgLatency() < 200 ? 'Within target' : 'Exceeds target — investigate' }}</div>
            </div>
          </div>
        </div>

      </div>

      <!-- Recent auctions table -->
      <div class="panel">
        <div class="panel-header">
          <div class="panel-title-block">
            <span class="panel-title">Recent Auctions</span>
            <span class="panel-subtitle">Last 5 completed dispatches</span>
          </div>
          <a class="panel-action" routerLink="/match-log">View all →</a>
        </div>
        <div class="panel-body" style="padding:0">
          <table class="data-table" *ngIf="recentLogs().length; else noLogs">
            <thead>
              <tr>
                <th>Category</th>
                <th>Winner</th>
                <th>Clearing Price</th>
                <th>Latency</th>
                <th>Outcome</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let log of recentLogs()">
                <td><span class="cat-badge">{{ log.jobCategory }}</span></td>
                <td class="terminal-number">{{ log.winnerProviderKey || '—' }}</td>
                <td class="terminal-number" style="color:#22C55E">
                  {{ log.clearingPriceCents ? ('$' + (log.clearingPriceCents / 100 | number:'1.2-2')) : '—' }}
                </td>
                <td>
                  <span class="terminal-number latency-cell"
                        [style.color]="log.durationMs < 100 ? '#22C55E' : log.durationMs < 150 ? '#F59E0B' : '#EF4444'">
                    {{ log.durationMs }}ms
                  </span>
                </td>
                <td>
                  <span class="outcome-chip"
                        [class]="'chip-' + log.outcome.toLowerCase().replace('_', '-')">
                    {{ log.outcome }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
          <ng-template #noLogs>
            <div class="empty-state" style="padding:32px">
              <mat-icon>hourglass_empty</mat-icon>
              <span>No auctions yet. Post to /api/v1/matches to trigger dispatch.</span>
            </div>
          </ng-template>
        </div>
      </div>

    </div>
  `,
  styles: [`
    .header-left { flex: 1; }
    .page-header {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
    }
    .header-live {
      display: flex;
      align-items: center;
      gap: 6px;
      background: rgba(34,197,94,0.1);
      border: 1px solid rgba(34,197,94,0.25);
      padding: 6px 12px;
      border-radius: 20px;
      margin-top: 4px;
    }
    .live-label {
      font-size: 0.75rem;
      color: #22C55E;
      font-family: 'Outfit', sans-serif;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.08em;
    }

    /* ── Panels ── */
    .two-col {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 16px;
      margin-bottom: 16px;
    }

    @media (max-width: 900px) {
      .two-col { grid-template-columns: 1fr; }
    }

    .panel {
      background: #0F1829;
      border: 1px solid #1E2D45;
      border-radius: 14px;
      margin-bottom: 16px;
      overflow: hidden;
    }

    .panel-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 16px 20px 12px;
      border-bottom: 1px solid #1E2D45;
    }

    .panel-title-block {
      display: flex;
      flex-direction: column;
      gap: 2px;
    }

    .panel-title {
      font-family: 'Syne', sans-serif;
      font-size: 0.95rem;
      font-weight: 600;
      color: #E8EDF5;
      letter-spacing: -0.01em;
    }

    .panel-subtitle {
      font-size: 0.75rem;
      color: #5A7090;
      font-family: 'Outfit', sans-serif;
    }

    .panel-action {
      font-size: 0.8rem;
      color: #F59E0B;
      text-decoration: none;
      font-family: 'Outfit', sans-serif;
      font-weight: 500;
      &:hover { color: #FBB840; }
    }

    .panel-body { padding: 16px 20px; }

    /* ── Capacity rows ── */
    .capacity-row {
      margin-bottom: 16px;
      &:last-child { margin-bottom: 0; }
    }

    .capacity-info {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 5px;
    }

    .capacity-name {
      font-family: 'Outfit', sans-serif;
      font-size: 0.85rem;
      font-weight: 500;
      color: #A8B8CC;
    }

    .capacity-count {
      font-family: 'JetBrains Mono', monospace;
      font-size: 0.8rem;
      color: #E8EDF5;
    }

    .capacity-sep { color: #5A7090; margin: 0 2px; }

    .capacity-footer {
      display: flex;
      justify-content: space-between;
      margin-top: 4px;
    }

    .capacity-skills {
      font-size: 0.7rem;
      color: #5A7090;
      font-family: 'Outfit', sans-serif;
    }

    .capacity-pct {
      font-family: 'JetBrains Mono', monospace;
      font-size: 0.7rem;
    }

    .fill-ok     { background: #00D4AA !important; color: #00D4AA; }
    .fill-medium { background: #F59E0B !important; color: #F59E0B; }
    .fill-high   { background: #EF4444 !important; color: #EF4444; }

    /* ── Performance grid ── */
    .perf-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 12px;
      margin-bottom: 20px;
    }

    .perf-metric {
      background: #162035;
      border: 1px solid #1E2D45;
      border-radius: 8px;
      padding: 12px 14px;
    }

    .perf-value {
      font-family: 'JetBrains Mono', monospace;
      font-size: 1.4rem;
      font-weight: 600;
      line-height: 1;
    }

    .perf-unit {
      font-size: 0.75rem;
      opacity: 0.7;
    }

    .perf-label {
      font-size: 0.7rem;
      color: #5A7090;
      margin-top: 5px;
      text-transform: uppercase;
      letter-spacing: 0.07em;
      font-family: 'Outfit', sans-serif;
    }

    .bench-section { margin-top: 4px; }

    .bench-label {
      display: flex;
      justify-content: space-between;
      font-size: 0.75rem;
      margin-bottom: 5px;
    }

    .bench-hint {
      font-size: 0.72rem;
      margin-top: 4px;
      font-family: 'Outfit', sans-serif;
    }

    /* ── Data table ── */
    .data-table {
      width: 100%;
      border-collapse: collapse;

      th {
        padding: 10px 16px;
        text-align: left;
        font-size: 0.7rem;
        font-weight: 600;
        color: #5A7090;
        text-transform: uppercase;
        letter-spacing: 0.08em;
        background: #162035;
        font-family: 'Outfit', sans-serif;
        border-bottom: 1px solid #1E2D45;
      }

      td {
        padding: 11px 16px;
        font-size: 0.85rem;
        color: #A8B8CC;
        border-bottom: 1px solid #1E2D45;
        font-family: 'Outfit', sans-serif;
      }

      tr:last-child td { border-bottom: none; }
      tr:hover td { background: #162035; }
    }

    .cat-badge {
      background: rgba(245,158,11,0.1);
      border: 1px solid rgba(245,158,11,0.2);
      color: #F59E0B;
      padding: 2px 8px;
      border-radius: 4px;
      font-size: 0.75rem;
      font-family: 'Outfit', sans-serif;
      font-weight: 500;
    }

    .outcome-chip {
      padding: 3px 10px;
      border-radius: 20px;
      font-size: 0.7rem;
      font-weight: 600;
      font-family: 'JetBrains Mono', monospace;
    }

    .empty-state {
      display: flex;
      align-items: center;
      gap: 8px;
      color: #5A7090;
      font-size: 0.85rem;
      font-family: 'Outfit', sans-serif;
      mat-icon { font-size: 20px; width: 20px; height: 20px; }
    }
  `]
})
export class DashboardComponent implements OnInit, OnDestroy {
  protected readonly Math = Math;

  private providerService = inject(ProviderService);
  private matchLogService = inject(MatchLogService);

  companies          = signal<Provider[]>([]);
  recentLogs         = signal<MatchLog[]>([]);
  totalMatches       = signal(0);
  totalEarningsCents = signal(0);
  activeCompanies    = signal(0);
  avgLatency         = signal(0);

  totalEarnings = computed(() => this.totalEarningsCents() / 100);

  winRate = computed(() => {
    const logs = this.recentLogs();
    if (!logs.length) return 0;
    return (logs.filter(l => l.outcome === 'MATCHED').length / logs.length) * 100;
  });

  noMatchRate = computed(() => {
    const logs = this.recentLogs();
    if (!logs.length) return 0;
    return (logs.filter(l => l.outcome !== 'MATCHED').length / logs.length) * 100;
  });

  avgPrice = computed(() => {
    const matched = this.recentLogs().filter(l => l.outcome === 'MATCHED' && l.clearingPriceCents);
    if (!matched.length) return 0;
    return matched.reduce((s, l) => s + (l.clearingPriceCents || 0), 0) / matched.length / 100;
  });

  private subs = new Subscription();

  ngOnInit() {
    this.subs.add(this.providerService.getAll().subscribe(p => this.companies.set(p)));

    this.subs.add(
      timer(0, 10_000).pipe(switchMap(() => this.providerService.getSummary()))
        .subscribe(s => {
          this.totalMatches.set(s.totalMatches);
          this.totalEarningsCents.set(s.totalEarnings);
          this.activeCompanies.set(s.activeProviders);
        })
    );

    this.subs.add(
      timer(0, 5_000).pipe(switchMap(() => this.matchLogService.getLogs(0, 5)))
        .subscribe(page => {
          this.recentLogs.set(page.content);
          const matched = page.content.filter(l => l.outcome === 'MATCHED');
          if (matched.length) {
            const avg = matched.reduce((a, b) => a + b.durationMs, 0) / matched.length;
            this.avgLatency.set(avg);
          }
        })
    );
  }

  ngOnDestroy() { this.subs.unsubscribe(); }

  fillClass(p: Provider): string {
    const ratio = p.totalActiveJobs / p.dailyJobCapacity;
    if (ratio >= 0.9) return 'fill-high';
    if (ratio >= 0.6) return 'fill-medium';
    return 'fill-ok';
  }
}
