export interface Pagination {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface PageResponse<T> {
  data: T[];
  pagination: Pagination;
}
