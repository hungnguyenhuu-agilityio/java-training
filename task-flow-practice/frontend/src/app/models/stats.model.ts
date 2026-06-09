export interface DashboardStats {
  byStatus: Record<string, number>;
  byPriority: Record<string, number>;
}
