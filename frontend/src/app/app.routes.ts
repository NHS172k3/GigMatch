import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent)
  },
  {
    path: 'providers',
    loadComponent: () =>
      import('./features/providers/provider-list/provider-list.component').then(m => m.ProviderListComponent)
  },
  {
    path: 'providers/new',
    loadComponent: () =>
      import('./features/providers/provider-form/provider-form.component').then(m => m.ProviderFormComponent)
  },
  {
    path: 'providers/:id',
    loadComponent: () =>
      import('./features/providers/provider-detail/provider-detail.component').then(m => m.ProviderDetailComponent)
  },
  {
    path: 'match-log',
    loadComponent: () =>
      import('./features/match-log/match-log.component').then(m => m.MatchLogComponent)
  },
];
