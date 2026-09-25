import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { CatalogService } from './catalog.service';
import { Category, Page, ProductDetail, ProductSummary } from './models/catalog.model';

describe('CatalogService', () => {
  let service: CatalogService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CatalogService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('requests the products page with default query parameters', () => {
    const page: Page<ProductSummary> = {
      content: [],
      page: 0,
      size: 12,
      totalElements: 0,
      totalPages: 0,
    };

    service.listProducts({}).subscribe();

    const req = httpMock.expectOne(
      (request) => request.url === '/api/catalog/products',
    );
    expect(req.request.method).toBe('GET');
    expect(req.request.params.has('q')).toBe(false);
    expect(req.request.params.has('categoryId')).toBe(false);
    expect(req.request.params.has('sort')).toBe(false);
    expect(req.request.params.has('page')).toBe(false);
    expect(req.request.params.has('size')).toBe(false);
    req.flush(page);
  });

  it('sends provided search, category, sort, and pagination parameters', () => {
    service
      .listProducts({ q: 'shoe', categoryId: 3, sort: 'price_asc', page: 2, size: 24 })
      .subscribe();

    const req = httpMock.expectOne(
      (request) => request.url === '/api/catalog/products',
    );
    expect(req.request.params.get('q')).toBe('shoe');
    expect(req.request.params.get('categoryId')).toBe('3');
    expect(req.request.params.get('sort')).toBe('price_asc');
    expect(req.request.params.get('page')).toBe('2');
    expect(req.request.params.get('size')).toBe('24');
    req.flush({ content: [], page: 2, size: 24, totalElements: 0, totalPages: 0 });
  });

  it('fetches a product detail by id', () => {
    const detail: ProductDetail = {
      id: 1,
      name: 'Widget',
      slug: 'widget',
      price: 9.99,
      currency: 'USD',
      category: { id: 1, name: 'Tools' },
      description: 'A useful widget.',
    };

    service.getProduct(1).subscribe((result) => {
      expect(result).toEqual(detail);
    });

    const req = httpMock.expectOne('/api/catalog/products/1');
    expect(req.request.method).toBe('GET');
    req.flush(detail);
  });

  it('fetches active categories', () => {
    const categories: Category[] = [{ id: 1, name: 'Tools', slug: 'tools' }];

    service.listCategories().subscribe((result) => {
      expect(result).toEqual(categories);
    });

    const req = httpMock.expectOne('/api/catalog/categories');
    expect(req.request.method).toBe('GET');
    req.flush(categories);
  });
});
