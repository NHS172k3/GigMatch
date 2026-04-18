import { Component, OnInit, signal, inject, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { ProviderService } from '../../../core/services/provider.service';
import { Provider } from '../../../core/models/provider.model';

@Component({
  selector: 'app-provider-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterLink, MatIconModule, MatButtonModule, MatChipsModule],
  template: `
    <div class="page-container">

      <div class="page-header" style="display:flex;justify-content:space-between;align-items:flex-start">
        <div>
          <h1>Service Companies</h1>
          <p>Pre-vetted companies with integrated dispatch APIs</p>
        </div>
        <a class="register-btn" routerLink="/providers/new">
          <mat-icon>add</mat-icon>
          Register Company
        </a>
      </div>

      <!-- Company card grid -->
      <div class="company-card-grid" *ngIf="providers().length; else loading">
        <a class="company-card" *ngFor="let p of providers()"
           [routerLink]="['/providers', p.id]">

          <!-- Header row -->
          <div class="cc-header">
            <div class="cc-avatar">{{ p.name.charAt(0) }}</div>
            <div class="cc-title-block">
              <div class="company-name">{{ p.name }}</div>
              <div class="company-key">{{ p.providerKey }}</div>
            </div>
            <span class="cc-status" [class]="'chip-' + p.status.toLowerCase()">{{ p.status }}</span>
          </div>

          <!-- Metrics row -->
          <div class="company-meta">
            <div class="meta-item">
              <mat-icon style="color:#F59E0B">star</mat-icon>
              <span class="value">{{ p.avgRating | number:'1.1-1' }}</span>
              <span>rating</span>
            </div>
            <div class="meta-item">
              <mat-icon style="color:#22C55E">check_circle</mat-icon>
              <span class="value">{{ (p.completionRate * 100) | number:'1.0-0' }}%</span>
              <span>completion</span>
            </div>
            <div class="meta-item">
              <mat-icon style="color:#00D4AA">work</mat-icon>
              <span class="value">{{ p.dailyJobCapacity }}</span>
              <span>slots/day</span>
            </div>
          </div>

          <!-- Capacity bar -->
          <div class="cc-capacity">
            <div class="cc-cap-row">
              <span class="muted-text" style="font-size:0.72rem">Capacity</span>
              <span class="terminal-number" style="font-size:0.75rem;color:#A8B8CC">
                {{ p.totalActiveJobs }}/{{ p.dailyJobCapacity }}
              </span>
            </div>
            <div class="capacity-bar-track">
              <div class="capacity-bar-fill"
                   [class]="fillClass(p)"
                   [style.width.%]="(p.totalActiveJobs / p.dailyJobCapacity) * 100 || 0">
              </div>
            </div>
          </div>

          <!-- Skills -->
          <div class="cc-skills">
            <span class="skill-chip" *ngFor="let s of p.skillCategories.slice(0,3)">{{ s }}</span>
            <span class="skill-chip skill-chip--more" *ngIf="p.skillCategories.length > 3">
              +{{ p.skillCategories.length - 3 }}
            </span>
          </div>

          <div class="cc-footer">
            <span class="cc-view">View details →</span>
          </div>

        </a>
      </div>

      <ng-template #loading>
        <div class="empty-panel">
          <mat-icon>hourglass_empty</mat-icon>
          <span>Loading companies...</span>
        </div>
      </ng-template>

    </div>
  `,
  styles: [`
    .register-btn {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      background: #F59E0B;
      color: #080E1A;
      padding: 9px 18px;
      border-radius: 8px;
      text-decoration: none;
      font-family: 'Outfit', sans-serif;
      font-size: 0.85rem;
      font-weight: 600;
      transition: background 0.15s;
      mat-icon { font-size: 18px; width: 18px; height: 18px; }
      &:hover { background: #FBB840; }
    }

    .company-card-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
      gap: 16px;
    }

    .company-card {
      background: #0F1829;
      border: 1px solid #1E2D45;
      border-radius: 14px;
      padding: 20px;
      display: flex;
      flex-direction: column;
      gap: 14px;
      text-decoration: none;
      cursor: pointer;
      transition: border-color 0.2s, transform 0.15s;

      &:hover {
        border-color: #243450;
        transform: translateY(-2px);
      }
    }

    /* Header */
    .cc-header {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .cc-avatar {
      width: 38px;
      height: 38px;
      border-radius: 8px;
      background: rgba(245,158,11,0.12);
      border: 1px solid rgba(245,158,11,0.25);
      color: #F59E0B;
      font-family: 'Syne', sans-serif;
      font-size: 1rem;
      font-weight: 700;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }

    .cc-title-block { flex: 1; min-width: 0; }

    .company-name {
      font-family: 'Syne', sans-serif;
      font-size: 0.95rem;
      font-weight: 700;
      color: #E8EDF5;
      letter-spacing: -0.01em;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .company-key {
      font-family: 'JetBrains Mono', monospace;
      font-size: 0.7rem;
      color: #F59E0B;
      margin-top: 1px;
    }

    .cc-status {
      padding: 3px 9px;
      border-radius: 20px;
      font-size: 0.68rem;
      font-weight: 600;
      flex-shrink: 0;
    }

    /* Metrics */
    .company-meta {
      display: flex;
      gap: 12px;
    }

    .meta-item {
      display: flex;
      align-items: center;
      gap: 4px;
      font-size: 0.78rem;
      color: #5A7090;
      font-family: 'Outfit', sans-serif;

      mat-icon { font-size: 14px; width: 14px; height: 14px; }

      .value {
        font-family: 'JetBrains Mono', monospace;
        color: #E8EDF5;
        font-size: 0.8rem;
        font-weight: 500;
      }
    }

    /* Capacity */
    .cc-capacity {}

    .cc-cap-row {
      display: flex;
      justify-content: space-between;
      margin-bottom: 4px;
    }

    .fill-ok     { background: #00D4AA !important; }
    .fill-medium { background: #F59E0B !important; }
    .fill-high   { background: #EF4444 !important; }

    /* Skills */
    .cc-skills {
      display: flex;
      flex-wrap: wrap;
      gap: 5px;
    }

    .skill-chip {
      background: rgba(90,112,144,0.15);
      border: 1px solid rgba(90,112,144,0.25);
      color: #A8B8CC;
      padding: 2px 8px;
      border-radius: 4px;
      font-size: 0.7rem;
      font-family: 'Outfit', sans-serif;
      font-weight: 500;
    }

    .skill-chip--more {
      background: rgba(245,158,11,0.08);
      border-color: rgba(245,158,11,0.2);
      color: #F59E0B;
    }

    /* Footer */
    .cc-footer {
      border-top: 1px solid #1E2D45;
      padding-top: 10px;
      margin-top: 2px;
    }

    .cc-view {
      font-size: 0.78rem;
      color: #5A7090;
      font-family: 'Outfit', sans-serif;
      transition: color 0.15s;
    }

    .company-card:hover .cc-view { color: #F59E0B; }

    /* Empty */
    .empty-panel {
      background: #0F1829;
      border: 1px solid #1E2D45;
      border-radius: 14px;
      padding: 48px;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 10px;
      color: #5A7090;
      font-family: 'Outfit', sans-serif;
      mat-icon { font-size: 24px; width: 24px; height: 24px; }
    }
  `]
})
export class ProviderListComponent implements OnInit {
  private providerService = inject(ProviderService);
  providers = signal<Provider[]>([]);

  ngOnInit() {
    this.providerService.getAll().subscribe(p => this.providers.set(p));
  }

  fillClass(p: Provider): string {
    const ratio = p.totalActiveJobs / p.dailyJobCapacity;
    if (ratio >= 0.9) return 'fill-high';
    if (ratio >= 0.6) return 'fill-medium';
    return 'fill-ok';
  }
}
