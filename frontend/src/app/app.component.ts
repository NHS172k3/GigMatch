import { Component } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatRippleModule } from '@angular/material/core';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatIconModule, MatSidenavModule, MatRippleModule],
  template: `
    <mat-sidenav-container class="shell">
      <mat-sidenav mode="side" opened class="sidebar">

        <!-- Logo -->
        <div class="sidebar-logo">
          <div class="logo-mark">
            <mat-icon class="logo-bolt">bolt</mat-icon>
          </div>
          <div class="logo-text-block">
            <span class="logo-name">GigMatch</span>
            <span class="logo-tagline">Dispatch Platform</span>
          </div>
        </div>

        <div class="sidebar-divider"></div>

        <!-- Nav -->
        <nav class="sidebar-nav">
          <span class="nav-section-label">Navigation</span>
          <a class="nav-item" routerLink="/dashboard" routerLinkActive="nav-item--active" matRipple>
            <mat-icon class="nav-icon">dashboard</mat-icon>
            <span class="nav-label">Dashboard</span>
          </a>
          <a class="nav-item" routerLink="/providers" routerLinkActive="nav-item--active" matRipple>
            <mat-icon class="nav-icon">business</mat-icon>
            <span class="nav-label">Companies</span>
          </a>
          <a class="nav-item" routerLink="/match-log" routerLinkActive="nav-item--active" matRipple>
            <mat-icon class="nav-icon">receipt_long</mat-icon>
            <span class="nav-label">Auction Log</span>
          </a>
        </nav>

        <!-- Footer -->
        <div class="sidebar-footer">
          <div class="sidebar-status">
            <span class="live-dot"></span>
            <span class="status-text">Live</span>
          </div>
          <span class="sidebar-version">v1.0.0</span>
        </div>

      </mat-sidenav>

      <mat-sidenav-content class="main-content">
        <router-outlet />
      </mat-sidenav-content>
    </mat-sidenav-container>
  `,
  styles: [`
    .shell { height: 100vh; }

    /* ── Sidebar ── */
    .sidebar {
      width: 240px;
      background: #080E1A;
      border-right: 1px solid #1E2D45 !important;
      display: flex;
      flex-direction: column;
    }

    .sidebar-logo {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 22px 20px 18px;
    }

    .logo-mark {
      width: 36px;
      height: 36px;
      background: rgba(245,158,11,0.15);
      border: 1px solid rgba(245,158,11,0.35);
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .logo-bolt {
      color: #F59E0B;
      font-size: 20px;
      width: 20px;
      height: 20px;
    }

    .logo-text-block {
      display: flex;
      flex-direction: column;
      gap: 1px;
    }

    .logo-name {
      font-family: 'Syne', sans-serif;
      font-size: 1.1rem;
      font-weight: 800;
      color: #E8EDF5;
      letter-spacing: -0.02em;
      line-height: 1;
    }

    .logo-tagline {
      font-size: 0.65rem;
      color: #5A7090;
      text-transform: uppercase;
      letter-spacing: 0.1em;
      font-family: 'Outfit', sans-serif;
    }

    .sidebar-divider {
      height: 1px;
      background: #1E2D45;
      margin: 0 16px 16px;
    }

    /* ── Navigation ── */
    .sidebar-nav {
      flex: 1;
      padding: 0 12px;
      display: flex;
      flex-direction: column;
      gap: 2px;
    }

    .nav-section-label {
      font-size: 0.65rem;
      font-weight: 600;
      color: #5A7090;
      text-transform: uppercase;
      letter-spacing: 0.12em;
      padding: 0 8px 10px;
      font-family: 'Outfit', sans-serif;
    }

    .nav-item {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 10px 12px;
      border-radius: 8px;
      text-decoration: none;
      color: #5A7090;
      transition: background 0.15s, color 0.15s;
      cursor: pointer;
    }

    .nav-item:hover {
      background: #162035;
      color: #A8B8CC;
    }

    .nav-item--active {
      background: rgba(245,158,11,0.1) !important;
      color: #F59E0B !important;
    }

    .nav-item--active .nav-icon {
      color: #F59E0B !important;
    }

    .nav-icon {
      font-size: 18px;
      width: 18px;
      height: 18px;
      color: inherit;
    }

    .nav-label {
      font-family: 'Outfit', sans-serif;
      font-size: 0.88rem;
      font-weight: 500;
    }

    /* ── Footer ── */
    .sidebar-footer {
      padding: 16px 20px;
      border-top: 1px solid #1E2D45;
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .sidebar-status {
      display: flex;
      align-items: center;
      gap: 6px;
    }

    .status-text {
      font-size: 0.75rem;
      color: #22C55E;
      font-family: 'Outfit', sans-serif;
      font-weight: 500;
    }

    .sidebar-version {
      font-size: 0.7rem;
      color: #5A7090;
      font-family: 'JetBrains Mono', monospace;
    }

    /* ── Main content ── */
    .main-content {
      background: #080E1A;
      overflow-y: auto;
    }
  `]
})
export class AppComponent {}
