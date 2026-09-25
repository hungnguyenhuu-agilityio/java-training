export interface Category {
  id: number;
  name: string;
  slug: string;
}

export interface ProductSummary {
  id: number;
  name: string;
  slug: string;
  price: number;
  currency: string;
  category: Pick<Category, 'id' | 'name'>;
}

export interface ProductDetail extends ProductSummary {
  description: string | null;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export type ProductSort = 'name_asc' | 'name_desc' | 'price_asc' | 'price_desc' | 'newest';

export interface ProductQuery {
  q?: string;
  categoryId?: number;
  sort?: ProductSort;
  page?: number;
  size?: number;
}

export interface ProblemDetailsViolation {
  field: string;
  message: string;
}

export interface ProblemDetails {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  violations?: ProblemDetailsViolation[];
}
