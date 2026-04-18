import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, timer } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { MatchLog, PagedMatchLogs } from '../models/match-log.model';

@Injectable({ providedIn: 'root' })
export class MatchLogService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/api/v1`;

  getLogs(page = 0, size = 20, providerId?: number): Observable<PagedMatchLogs> {
    let url = `${this.base}/match-logs?page=${page}&size=${size}`;
    if (providerId) url += `&providerId=${providerId}`;
    return this.http.get<PagedMatchLogs>(url);
  }

  /** Observable that polls for the latest match logs every intervalMs. */
  pollLogs(intervalMs = 3000, size = 30): Observable<MatchLog[]> {
    return timer(0, intervalMs).pipe(
      switchMap(() => this.getLogs(0, size)),
      map(page => page.content)
    );
  }
}
