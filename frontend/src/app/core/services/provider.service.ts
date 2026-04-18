import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Provider, ProviderStats, CreateProviderForm } from '../models/provider.model';

@Injectable({ providedIn: 'root' })
export class ProviderService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/api/v1/providers`;

  getAll(): Observable<Provider[]> {
    return this.http.get<Provider[]>(this.base);
  }

  getById(id: number): Observable<Provider> {
    return this.http.get<Provider>(`${this.base}/${id}`);
  }

  create(form: CreateProviderForm): Observable<Provider> {
    return this.http.post<Provider>(this.base, form);
  }

  update(id: number, form: CreateProviderForm): Observable<Provider> {
    return this.http.put<Provider>(`${this.base}/${id}`, form);
  }

  getStats(id: number): Observable<ProviderStats> {
    return this.http.get<ProviderStats>(`${this.base}/${id}/stats`);
  }

  getSummary(): Observable<{ totalMatches: number; totalEarnings: number; activeProviders: number }> {
    return this.http.get<any>(`${this.base}/summary`);
  }

  addPortfolio(providerId: number, title: string, description: string, category: string): Observable<any> {
    const params = new URLSearchParams({ title, description, category }).toString();
    return this.http.post<any>(`${this.base}/${providerId}/portfolios?${params}`, null);
  }
}
