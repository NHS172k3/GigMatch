import { Component, OnInit, signal, inject, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { switchMap } from 'rxjs';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { ProviderService } from '../../../core/services/provider.service';
import { MatchLogService } from '../../../core/services/match-log.service';
import { Provider, ProviderStats } from '../../../core/models/provider.model';
import { MatchLog } from '../../../core/models/match-log.model';

@Component({
  selector: 'app-provider-detail',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterLink, MatIconModule, MatButtonModule, MatTableModule],
  template: `
    <div class="page-container" *ngIf="provider() as p">

      <!-- Header -->
      <div class="detail-header">
        <div class="detail-header-left">
          <div class="detail-avatar">{{ p.name.charAt(0) }}</div>
          <div>
            <h1 style="margin:0 0 4px;font-family:'Syne',sans-serif;font-size:1.6rem;font-weight:700;color:#E8EDF5">
              {{ p.name }}
            </h1>
            <div style="display:flex;align-items:center;gap:10px">
              <span class="terminal-number" style="color:#F59E0B;font-size:0.8rem">{{ p.providerKey }}</span>
              <span class="status-badge" [class]="'chip-' + p.status.toLowerCase()">{{ p.status }}</span>
            </div>
          </div>
        </div>
        <a mat-button routerLink="/providers" style="color:#5A7090">
          <mat-icon>arrow_back</mat-icon> Companies
        </a>
      </div>

      <!-- Stat cards -->
      <div class="card-grid" *ngIf="stats() as s">
        <div class="stat-card">
          <mat-icon class="stat-icon" style="color:#F59E0B">swap_horiz</mat-icon>
          <div class="stat-value">{{ s.totalMatches | number }}</div>
          <div class="stat-label">Total Matches</div>
        </div>
        <div class="stat-card">
          <mat-icon class="stat-icon" style="color:#22C55E">attach_money</mat-icon>
          <div class="stat-value">\${{ s.totalEarningsCents / 100 | number:'1.0-0' }}</div>
          <div class="stat-label">Total Earnings</div>
        </div>
        <div class="stat-card">
          <mat-icon class="stat-icon" style="color:#00D4AA">emoji_events</mat-icon>
          <div class="stat-value">{{ s.winRatePct | number:'1.1-1' }}<span style="font-size:1rem">%</span></div>
          <div class="stat-label">Win Rate</div>
        </div>
        <div class="stat-card">
          <mat-icon class="stat-icon" style="color:#F59E0B">price_check</mat-icon>
          <div class="stat-value">\${{ s.avgClearingPriceCents / 100 | number:'1.0-0' }}</div>
          <div class="stat-label">Avg Clearing Price</div>
        </div>
      </div>

      <!-- Profile + Portfolio -->
      <div class="two-col" style="margin-bottom:16px">

        <!-- Profile panel -->
        <div class="panel">
          <div class="panel-header">
            <div class="panel-title-block">
              <span class="panel-title">Company Profile</span>
              <span class="panel-subtitle">Dispatch system configuration</span>
            </div>
          </div>
          <div class="panel-body">
            <div class="profile-row">
              <span class="profile-key">Rating</span>
              <span class="profile-val terminal-number">
                <mat-icon style="font-size:14px;color:#F59E0B;vertical-align:middle">star</mat-icon>
                {{ p.avgRating | number:'1.1-1' }}/5.0
              </span>
            </div>
            <div class="profile-row">
              <span class="profile-key">Completion Rate</span>
              <span class="profile-val terminal-number" style="color:#22C55E">{{ (p.completionRate * 100) | number:'1.1-1' }}%</span>
            </div>
            <div class="profile-row">
              <span class="profile-key">Daily Contract Capacity</span>
              <span class="profile-val terminal-number">{{ p.dailyJobCapacity }} contracts/day</span>
            </div>
            <div class="profile-row">
              <span class="profile-key">Active Contracts</span>
              <span class="profile-val terminal-number">{{ p.totalActiveJobs }}</span>
            </div>
            <div class="profile-skills-section">
              <span class="profile-key">Specialisations</span>
              <div class="skills-wrap">
                <span class="skill-chip" *ngFor="let s of p.skillCategories">{{ s }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Portfolio panel -->
        <div class="panel">
          <div class="panel-header">
            <div class="panel-title-block">
              <span class="panel-title">Portfolio</span>
              <span class="panel-subtitle">Work samples from this company</span>
            </div>
          </div>
          <div class="panel-body">
            <div *ngFor="let port of p.portfolios" class="portfolio-item">
              <img [src]="port.sampleUrl" [alt]="port.title"
                   class="portfolio-thumb">
              <div class="portfolio-info">
                <div class="portfolio-title">{{ port.title }}</div>
                <div class="portfolio-cat skill-chip" style="display:inline-block">{{ port.category }}</div>
              </div>
            </div>
            <div *ngIf="!p.portfolios.length" class="empty-state">
              <mat-icon>image_not_supported</mat-icon>
              <span>No portfolio items yet.</span>
            </div>
          </div>
        </div>

      </div>

      <!-- Recent auction wins -->
      <div class="panel">
        <div class="panel-header">
          <div class="panel-title-block">
            <span class="panel-title">Recent Auction Wins</span>
            <span class="panel-subtitle">Contracts awarded to this company</span>
          </div>
        </div>
        <div class="panel-body" style="padding:0">
          <table class="data-table" *ngIf="recentLogs().length; else noWins">
            <thead>
              <tr>
                <th>Category</th>
                <th>Clearing Price</th>
                <th>Latency</th>
                <th>Date</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let l of recentLogs()">
                <td><span class="cat-badge">{{ l.jobCategory }}</span></td>
                <td class="terminal-number" style="color:#22C55E">
                  \${{ (l.clearingPriceCents || 0) / 100 | number:'1.2-2' }}
                </td>
                <td>
                  <span class="terminal-number"
                        [style.color]="l.durationMs < 100 ? '#22C55E' : l.durationMs < 150 ? '#F59E0B' : '#EF4444'">
                    {{ l.durationMs }}ms
                  </span>
                </td>
                <td class="muted-text">{{ l.createdAt | date:'short' }}</td>
              </tr>
            </tbody>
          </table>
          <ng-template #noWins>
            <div class="empty-state" style="padding:24px">
              <mat-icon>hourglass_empty</mat-icon>
              <span>No auction wins recorded yet.</span>
            </div>
          </ng-template>
        </div>
      </div>

    </div>

    <!-- Loading state -->
    <div *ngIf="!provider()" class="page-container loading-state">
      <mat-icon>hourglass_empty</mat-icon>
      <p>Loading company...</p>
    </div>
  `,
  styles: [`
    .detail-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 24px;
    }

    .detail-header-left {
      display: flex;
      align-items: center;
      gap: 16px;
    }

    .detail-avatar {
      width: 52px;
      height: 52px;
      border-radius: 12px;
      background: rgba(245,158,11,0.12);
      border: 1px solid rgba(245,158,11,0.3);
      color: #F59E0B;
      font-family: 'Syne', sans-serif;
      font-size: 1.4rem;
      font-weight: 800;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .status-badge {
      padding: 3px 10px;
      border-radius: 20px;
      font-size: 0.72rem;
      font-weight: 600;
    }

    .two-col {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 16px;
    }

    @media (max-width: 900px) { .two-col { grid-template-columns: 1fr; } }

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
      padding: 14px 20px 12px;
      border-bottom: 1px solid #1E2D45;
    }

    .panel-title-block { display: flex; flex-direction: column; gap: 2px; }
    .panel-title { font-family: 'Syne', sans-serif; font-size: 0.9rem; font-weight: 600; color: #E8EDF5; }
    .panel-subtitle { font-size: 0.72rem; color: #5A7090; font-family: 'Outfit', sans-serif; }
    .panel-body { padding: 16px 20px; }

    /* Profile rows */
    .profile-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 9px 0;
      border-bottom: 1px solid #1E2D45;
      &:last-of-type { border-bottom: none; }
    }

    .profile-key {
      font-size: 0.8rem;
      color: #5A7090;
      font-family: 'Outfit', sans-serif;
    }

    .profile-val {
      font-size: 0.85rem;
      color: #E8EDF5;
    }

    .profile-skills-section {
      padding-top: 12px;
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .skills-wrap {
      display: flex;
      flex-wrap: wrap;
      gap: 5px;
    }

    .skill-chip {
      background: rgba(90,112,144,0.15);
      border: 1px solid rgba(90,112,144,0.25);
      color: #A8B8CC;
      padding: 3px 9px;
      border-radius: 4px;
      font-size: 0.72rem;
      font-family: 'Outfit', sans-serif;
    }

    /* Portfolio */
    .portfolio-item {
      display: flex;
      gap: 12px;
      align-items: center;
      margin-bottom: 12px;
      &:last-child { margin-bottom: 0; }
    }

    .portfolio-thumb {
      width: 72px;
      height: 52px;
      object-fit: cover;
      border-radius: 6px;
      border: 1px solid #1E2D45;
      flex-shrink: 0;
    }

    .portfolio-title {
      font-weight: 500;
      color: #E8EDF5;
      font-family: 'Outfit', sans-serif;
      font-size: 0.88rem;
      margin-bottom: 5px;
    }

    /* Data table */
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
    }

    /* Empty / loading */
    .empty-state {
      display: flex;
      align-items: center;
      gap: 8px;
      color: #5A7090;
      font-size: 0.85rem;
      font-family: 'Outfit', sans-serif;
      mat-icon { font-size: 18px; width: 18px; height: 18px; }
    }

    .loading-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 80px;
      color: #5A7090;
      gap: 12px;
      mat-icon { font-size: 48px; width: 48px; height: 48px; }
      p { font-family: 'Outfit', sans-serif; }
    }
  `]
})
export class ProviderDetailComponent implements OnInit {
  private route           = inject(ActivatedRoute);
  private providerService = inject(ProviderService);
  private matchLogService = inject(MatchLogService);

  provider   = signal<Provider | null>(null);
  stats      = signal<ProviderStats | null>(null);
  recentLogs = signal<MatchLog[]>([]);

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.providerService.getById(id).subscribe(p => this.provider.set(p));
    this.providerService.getStats(id).subscribe(s => this.stats.set(s));
    this.matchLogService.getLogs(0, 10, id).subscribe(page => this.recentLogs.set(page.content));
  }
}
