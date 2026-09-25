import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Category, Page, ProductDetail, ProductQuery, ProductSummary } from './models/catalog.model';

const PRODUCTS_URL = '/api/catalog/products';
const CATEGORIES_URL = '/api/catalog/categories';

@Injectable({ providedIn: 'root' })
export class CatalogService {
  constructor(private readonly http: HttpClient) {}

  listProducts(query: ProductQuery): Observable<Page<ProductSummary>> {
    let params = new HttpParams();

    if (query.q) {
      params = params.set('q', query.q);
    }
    if (query.categoryId != null) {
      params = params.set('categoryId', query.categoryId);
    }
    if (query.sort) {
      params = params.set('sort', query.sort);
    }
    if (query.page != null) {
      params = params.set('page', query.page);
    }
    if (query.size != null) {
      params = params.set('size', query.size);
    }

    return this.http.get<Page<ProductSummary>>(PRODUCTS_URL, { params });
  }

  getProduct(id: number): Observable<ProductDetail> {
    return this.http.get<ProductDetail>(`${PRODUCTS_URL}/${id}`);
  }

  listCategories(): Observable<Category[]> {
    return this.http.get<Category[]>(CATEGORIES_URL);
  }
}
