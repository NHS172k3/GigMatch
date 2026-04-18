export interface MatchLog {
  id: number;
  requestId: string;
  clientId: string;
  jobCategory: string;
  requiredSkills: string[];
  budgetCents: number;
  urgencyLevel: string;
  winnerProviderKey: string | null;
  winnerProviderId: number | null;
  clearingPriceCents: number | null;
  predictedSuccessRate: number | null;
  effectiveScore: number | null;
  durationMs: number;
  outcome: 'MATCHED' | 'NO_MATCH' | 'TIMEOUT';
  participatingProviders: string[];
  createdAt: string;
}

export interface PagedMatchLogs {
  content: MatchLog[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
