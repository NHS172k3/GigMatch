import { Component, signal, inject, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { COMMA, ENTER } from '@angular/cdk/keycodes';
import { MatChipInputEvent } from '@angular/material/chips';
import { ProviderService } from '../../../core/services/provider.service';

@Component({
  selector: 'app-provider-form',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, ReactiveFormsModule, RouterLink,
    MatCardModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatIconModule, MatChipsModule
  ],
  template: `
    <div class="page-container" style="max-width:700px">
      <div class="page-header">
        <h1>Register Company</h1>
        <p>Integrate a new service company into the dispatch network</p>
      </div>

      <mat-card>
        <mat-card-content>
          <form [formGroup]="form" (ngSubmit)="submit()" style="display:flex;flex-direction:column;gap:16px;padding:4px 0">

            <mat-form-field class="full-width" appearance="fill">
              <mat-label>Company Name</mat-label>
              <input matInput formControlName="name" placeholder="e.g. Acme Dev Studio">
              <mat-error *ngIf="form.get('name')?.hasError('required')">Name is required</mat-error>
            </mat-form-field>

            <mat-form-field class="full-width" appearance="fill">
              <mat-label>Company ID</mat-label>
              <input matInput formControlName="providerKey" placeholder="e.g. acme-dev">
              <mat-hint>Short identifier used in auction results (e.g. acme-dev)</mat-hint>
            </mat-form-field>

            <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px">
              <mat-form-field appearance="fill">
                <mat-label>Average Rating (1–5)</mat-label>
                <input matInput type="number" formControlName="avgRating" min="1" max="5" step="0.1">
              </mat-form-field>

              <mat-form-field appearance="fill">
                <mat-label>Completion Rate (0–1)</mat-label>
                <input matInput type="number" formControlName="completionRate" min="0" max="1" step="0.01">
                <mat-hint>e.g. 0.95 = 95%</mat-hint>
              </mat-form-field>
            </div>

            <mat-form-field class="full-width" appearance="fill">
              <mat-label>Daily Contract Capacity</mat-label>
              <input matInput type="number" formControlName="dailyJobCapacity" min="1">
              <mat-hint>Maximum new contracts this company accepts per day</mat-hint>
            </mat-form-field>

            <!-- Skill categories -->
            <mat-form-field class="full-width" appearance="fill">
              <mat-label>Skill Categories</mat-label>
              <mat-chip-grid #chipGrid>
                <mat-chip-row *ngFor="let skill of skills()" (removed)="removeSkill(skill)">
                  {{ skill }}
                  <button matChipRemove><mat-icon>cancel</mat-icon></button>
                </mat-chip-row>
              </mat-chip-grid>
              <input placeholder="Add category (press Enter)..."
                     [matChipInputFor]="chipGrid"
                     [matChipInputSeparatorKeyCodes]="separatorKeys"
                     (matChipInputTokenEnd)="addSkill($event)">
              <mat-hint>e.g. software-dev, design, data-science</mat-hint>
            </mat-form-field>

            <div *ngIf="error()" class="form-error">{{ error() }}</div>

            <div style="display:flex;gap:12px;justify-content:flex-end;margin-top:4px">
              <a mat-button routerLink="/providers" style="color:#5A7090">Cancel</a>
              <button mat-raised-button color="accent" type="submit"
                      [disabled]="form.invalid || saving()">
                <mat-icon>add_business</mat-icon>
                {{ saving() ? 'Registering...' : 'Register Company' }}
              </button>
            </div>

          </form>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .form-error {
      background: rgba(239,68,68,0.1);
      border: 1px solid rgba(239,68,68,0.3);
      color: #EF4444;
      border-radius: 6px;
      padding: 10px 14px;
      font-size: 0.85rem;
      font-family: 'Outfit', sans-serif;
    }
  `]
})
export class ProviderFormComponent {
  private fb              = inject(FormBuilder);
  private providerService = inject(ProviderService);
  private router          = inject(Router);

  separatorKeys = [ENTER, COMMA];
  skills  = signal<string[]>([]);
  saving  = signal(false);
  error   = signal<string | null>(null);

  form = this.fb.group({
    name:             ['', [Validators.required, Validators.minLength(3)]],
    providerKey:      ['', Validators.required],
    avgRating:        [4.5, [Validators.required, Validators.min(1), Validators.max(5)]],
    completionRate:   [0.90, [Validators.required, Validators.min(0), Validators.max(1)]],
    dailyJobCapacity: [10, [Validators.required, Validators.min(1)]],
  });

  addSkill(event: MatChipInputEvent) {
    const value = (event.value || '').trim().toLowerCase();
    if (value) this.skills.update(s => [...s, value]);
    event.chipInput.clear();
  }

  removeSkill(skill: string) {
    this.skills.update(s => s.filter(x => x !== skill));
  }

  submit() {
    if (this.form.invalid) return;
    this.saving.set(true);
    this.error.set(null);
    const payload = { ...this.form.value, skillCategories: this.skills() } as any;
    this.providerService.create(payload).subscribe({
      next: (p) => this.router.navigate(['/providers', p.id]),
      error: (err) => {
        this.error.set(err.error?.detail || 'Failed to register company');
        this.saving.set(false);
      }
    });
  }
}
