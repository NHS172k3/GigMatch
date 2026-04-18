import { Component, OnInit, OnDestroy, signal, inject, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { Subscription } from 'rxjs';
import { MatchLogService } from '../../core/services/match-log.service';
import { MatchLog } from '../../core/models/match-log.model';

@Component({
  selector: 'app-match-log',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, MatIconModule, MatPaginatorModule],
  template: `
    <div class="page-container">

      <!-- Header -->
      <div class="page-header" style="display:flex;justify-content:space-between;align-items:flex-start">
        <div>
          <h1>Auction Log</h1>
          <p>Live dispatch results — refreshes every 3 seconds</p>
        </div>
        <div class="live-badge">
          <span class="live-dot"></span>
          <span class="live-badge-text">Live</span>
        </div>
      </div>

      <!-- Table panel -->
      <div class="panel">
        <div class="panel-body" style="padding:0">
          <table class="data-table" *ngIf="logs().length; else noLogs">
            <thead>
              <tr>
                <th>Time</th>
                <th>Category</th>
                <th>Winner</th>
                <th>Clearing Price</th>
                <th>Success Rate</th>
                <th>Latency</th>
                <th>Outcome</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let l of logs()">
                <td class="terminal-number muted-text" style="white-space:nowrap;font-size:0.78rem">
                  {{ l.createdAt | date:'HH:mm:ss' }}
                </td>
                <td>
                  <span class="cat-badge">{{ l.jobCategory }}</span>
                  <div class="skills-hint muted-text">{{ l.requiredSkills?.join(', ') || '—' }}</div>
                </td>
                <td class="terminal-number">{{ l.winnerProviderKey || '—' }}</td>
                <td class="terminal-number" [style.color]="l.clearingPriceCents ? '#22C55E' : '#5A7090'">
                  {{ l.clearingPriceCents != null ? ('$' + (l.clearingPriceCents / 100 | number:'1.2-2')) : '—' }}
                </td>
                <td class="terminal-number muted-text">
                  {{ l.predictedSuccessRate != null ? ((l.predictedSuccessRate * 100) | number:'1.1-1') + '%' : '—' }}
                </td>
                <td>
                  <span class="terminal-number latency-val"
                        [style.color]="latencyColor(l.durationMs)">
                    {{ l.durationMs }}ms
                  </span>
                </td>
                <td>
                  <span class="outcome-chip" [class]="outcomeClass(l.outcome)">
                    {{ l.outcome }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>

          <ng-template #noLogs>
            <div class="empty-state">
              <mat-icon>hourglass_empty</mat-icon>
              <span>No auctions yet. POST to /api/v1/matches to trigger a dispatch.</span>
            </div>
          </ng-template>
        </div>

        <!-- Paginator -->
        <div class="paginator-row">
          <mat-paginator
            [length]="totalElements()"
            [pageSize]="pageSize"
            [pageSizeOptions]="[20, 50, 100]"
            (page)="onPage($event)">
          </mat-paginator>
        </div>
      </div>

    </div>
  `,
  styles: [`
    .live-badge {
      display: flex;
      align-items: center;
      gap: 6px;
      background: rgba(34,197,94,0.1);
      border: 1px solid rgba(34,197,94,0.25);
      padding: 7px 14px;
      border-radius: 20px;
      margin-top: 4px;
    }

    .live-badge-text {
      font-size: 0.75rem;
      color: #22C55E;
      font-family: 'Outfit', sans-serif;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.08em;
    }

    .panel {
      background: #0F1829;
      border: 1px solid #1E2D45;
      border-radius: 14px;
      overflow: hidden;
    }

    .data-table {
      width: 100%;
      border-collapse: collapse;

      th {
        padding: 10px 14px;
        text-align: left;
        font-size: 0.68rem;
        font-weight: 600;
        color: #5A7090;
        text-transform: uppercase;
        letter-spacing: 0.08em;
        background: #162035;
        font-family: 'Outfit', sans-serif;
        border-bottom: 1px solid #1E2D45;
        position: sticky;
        top: 0;
        z-index: 1;
      }

      td {
        padding: 10px 14px;
        font-size: 0.83rem;
        color: #A8B8CC;
        border-bottom: 1px solid #1E2D45;
        font-family: 'Outfit', sans-serif;
        vertical-align: middle;
      }

      tr:last-child td { border-bottom: none; }
      tr:hover td { background: #162035; }
    }

    .cat-badge {
      background: rgba(245,158,11,0.1);
      border: 1px solid rgba(245,158,11,0.2);
      color: #F59E0B;
      padding: 2px 7px;
      border-radius: 4px;
      font-size: 0.73rem;
      font-family: 'Outfit', sans-serif;
      font-weight: 500;
    }

    .skills-hint {
      font-size: 0.7rem;
      margin-top: 3px;
      font-family: 'Outfit', sans-serif;
    }

    .latency-val {
      font-size: 0.82rem;
      font-weight: 500;
    }

    .outcome-chip {
      padding: 3px 9px;
      border-radius: 20px;
      font-size: 0.68rem;
      font-weight: 700;
      font-family: 'JetBrains Mono', monospace;
      letter-spacing: 0.03em;
    }

    .chip-matched {
      background: rgba(34,197,94,0.12) !important;
      color: #22C55E !important;
      border: 1px solid rgba(34,197,94,0.25) !important;
    }

    .chip-no-match {
      background: rgba(239,68,68,0.12) !important;
      color: #EF4444 !important;
      border: 1px solid rgba(239,68,68,0.25) !important;
    }

    .chip-timeout {
      background: rgba(249,115,22,0.12) !important;
      color: #F97316 !important;
      border: 1px solid rgba(249,115,22,0.25) !important;
    }

    .paginator-row {
      border-top: 1px solid #1E2D45;
    }

    .empty-state {
      display: flex;
      align-items: center;
      gap: 10px;
      color: #5A7090;
      font-family: 'Outfit', sans-serif;
      padding: 48px 24px;
      justify-content: center;
      mat-icon { font-size: 28px; width: 28px; height: 28px; }
    }
  `]
})
export class MatchLogComponent implements OnInit, OnDestroy {
  private matchLogService = inject(MatchLogService);

  logs          = signal<MatchLog[]>([]);
  totalElements = signal(0);
  pageSize      = 20;
  currentPage   = 0;

  private pollSub?: Subscription;

  ngOnInit() {
    this.pollSub = this.matchLogService.pollLogs(3000, this.pageSize).subscribe(logs => {
      this.logs.set(logs);
    });
    this.matchLogService.getLogs(0, this.pageSize).subscribe(page => {
      this.totalElements.set(page.totalElements);
    });
  }

  ngOnDestroy() { this.pollSub?.unsubscribe(); }

  onPage(event: PageEvent) {
    this.currentPage = event.pageIndex;
    this.pageSize    = event.pageSize;
    this.pollSub?.unsubscribe();
    this.matchLogService.getLogs(this.currentPage, this.pageSize).subscribe(page => {
      this.logs.set(page.content);
      this.totalElements.set(page.totalElements);
    });
  }

  latencyColor(ms: number): string {
    if (ms < 100) return '#22C55E';   // green — well within target
    if (ms < 150) return '#F59E0B';   // amber — acceptable
    return '#EF4444';                  // red — approaching/exceeding p99 target
  }

  outcomeClass(outcome: string): string {
    if (outcome === 'MATCHED')  return 'outcome-chip chip-matched';
    if (outcome === 'TIMEOUT')  return 'outcome-chip chip-timeout';
    return 'outcome-chip chip-no-match';
  }
}
