export interface PortfolioSummary {
  id: number;
  title: string;
  sampleUrl: string;
  category: string;
}

export interface Provider {
  id: number;
  name: string;
  providerKey: string;
  avgRating: number;
  completionRate: number;
  dailyJobCapacity: number;
  totalActiveJobs: number;
  status: string;
  skillCategories: string[];
  portfolios: PortfolioSummary[];
}

export interface ProviderStats {
  providerId: number;
  providerKey: string;
  name: string;
  totalMatches: number;
  totalEarningsCents: number;
  winRatePct: number;
  avgClearingPriceCents: number;
  capacityRemaining: number;
  dailyJobCapacity: number;
}

export interface CreateProviderForm {
  name: string;
  providerKey: string;
  avgRating: number;
  completionRate: number;
  dailyJobCapacity: number;
  skillCategories: string[];
}
