import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { MATERIAL_ANIMATIONS } from '@angular/material/core';
import { ActivatedRoute, provideRouter, Router } from '@angular/router';
import { TestBed } from '@angular/core/testing';
import { BehaviorSubject, of } from 'rxjs';
import { ProductListComponent } from './product-list.component';
import { Category, Page, ProductSummary } from '../models/catalog.model';

function buildPage(content: ProductSummary[], overrides: Partial<Page<ProductSummary>> = {}): Page<ProductSummary> {
  return {
    content,
    page: 0,
    size: 12,
    totalElements: content.length,
    totalPages: content.length > 0 ? 1 : 0,
    ...overrides,
  };
}

const CATEGORIES: Category[] = [
  { id: 1, name: 'Tools', slug: 'tools' },
  { id: 2, name: 'Kitchen', slug: 'kitchen' },
];

const PRODUCT: ProductSummary = {
  id: 10,
  name: 'Hammer',
  slug: 'hammer',
  price: 19.99,
  currency: 'USD',
  category: { id: 1, name: 'Tools' },
};

describe('ProductListComponent', () => {
  let httpMock: HttpTestingController;

  function setup(initialParams: Record<string, string> = {}) {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [ProductListComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
        {
          provide: ActivatedRoute,
          useValue: { queryParamMap: of(new Map(Object.entries(initialParams))) },
        },
      ],
    });

    httpMock = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(ProductListComponent);
    return fixture;
  }

  /**
   * Setup with a controllable `queryParamMap` (a `BehaviorSubject`) and a fake `Router.navigate`
   * that merges the requested query params back into that subject — simulating real navigation
   * closing the loop through `ActivatedRoute.queryParamMap`, the way the component relies on.
   */
  function setupNavigable(initialParams: Record<string, string> = {}) {
    const paramsSubject = new BehaviorSubject<Map<string, string>>(new Map(Object.entries(initialParams)));
    const navigate = vi.fn((_commands: unknown[], extras: { queryParams: Record<string, unknown> }) => {
      const merged = new Map(paramsSubject.value);
      for (const [key, value] of Object.entries(extras.queryParams)) {
        if (value === null || value === undefined) {
          merged.delete(key);
        } else {
          merged.set(key, String(value));
        }
      }
      paramsSubject.next(merged);
      return Promise.resolve(true);
    });

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [ProductListComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
        { provide: ActivatedRoute, useValue: { queryParamMap: paramsSubject.asObservable() } },
        { provide: Router, useValue: { navigate } },
      ],
    });

    httpMock = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(ProductListComponent);
    return { fixture, paramsSubject, navigate };
  }

  function flushCategories() {
    const req = httpMock.expectOne('/api/catalog/categories');
    req.flush(CATEGORIES);
  }

  afterEach(() => {
    httpMock.verify();
  });

  it('shows a loading indicator before the products request resolves', () => {
    const fixture = setup();
    fixture.detectChanges();
    flushCategories();
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('[data-testid="catalog-loading"]')).toBeTruthy();

    httpMock.expectOne((r) => r.url === '/api/catalog/products').flush(buildPage([PRODUCT]));
  });

  it('renders product cards with name, price, currency, and a View details action', async () => {
    const fixture = setup();
    fixture.detectChanges();
    flushCategories();
    fixture.detectChanges();

    httpMock.expectOne((r) => r.url === '/api/catalog/products').flush(buildPage([PRODUCT]));
    fixture.detectChanges();
    await fixture.whenStable();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('[data-testid="catalog-loading"]')).toBeFalsy();
    expect(compiled.textContent).toContain('Hammer');
    expect(compiled.textContent).toContain('View details');
    expect(compiled.querySelector('img')).toBeFalsy();
    const placeholder = compiled.querySelector('.thumbnail');
    expect(placeholder?.tagName).toBe('DIV');
    expect(placeholder?.getAttribute('aria-hidden')).toBe('true');
  });

  it('shows the initial-empty state when the catalog has no products and no filters applied', async () => {
    const fixture = setup();
    fixture.detectChanges();
    flushCategories();
    fixture.detectChanges();

    httpMock.expectOne((r) => r.url === '/api/catalog/products').flush(buildPage([]));
    fixture.detectChanges();
    await fixture.whenStable();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('[data-testid="catalog-empty"]')).toBeTruthy();
  });

  it('shows the filtered-empty state when a search query returns no products', async () => {
    const fixture = setup({ q: 'nomatch' });
    fixture.detectChanges();
    flushCategories();
    fixture.detectChanges();

    httpMock.expectOne((r) => r.url === '/api/catalog/products').flush(buildPage([]));
    fixture.detectChanges();
    await fixture.whenStable();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('[data-testid="catalog-filtered-empty"]')).toBeTruthy();
  });

  it('shows a page-level alert on server failure', async () => {
    const fixture = setup();
    fixture.detectChanges();
    flushCategories();
    fixture.detectChanges();

    httpMock
      .expectOne((r) => r.url === '/api/catalog/products')
      .flush({ title: 'Server error' }, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();
    await fixture.whenStable();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('[data-testid="catalog-server-failure"]')).toBeTruthy();
  });

  it('shows returned validation violations on an invalid filter without leaking internals', async () => {
    const fixture = setup();
    fixture.detectChanges();
    flushCategories();
    fixture.detectChanges();

    httpMock.expectOne((r) => r.url === '/api/catalog/products').flush(
      {
        title: 'Invalid request',
        status: 400,
        violations: [{ field: 'page', message: 'must be zero or greater' }],
      },
      { status: 400, statusText: 'Bad Request' },
    );
    fixture.detectChanges();
    await fixture.whenStable();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('[data-testid="catalog-invalid-filter"]')).toBeTruthy();
    expect(compiled.textContent).toContain('must be zero or greater');
  });

  it('applies filters and updates the URL query params only when Apply filters is activated', async () => {
    const { fixture, navigate } = setupNavigable();
    fixture.detectChanges();
    flushCategories();
    fixture.detectChanges();
    httpMock.expectOne((r) => r.url === '/api/catalog/products').flush(buildPage([PRODUCT]));
    fixture.detectChanges();
    await fixture.whenStable();

    const component = fixture.componentInstance;
    component.searchControl.setValue('hammer');
    component.categoryControl.setValue(1);
    component.sortControl.setValue('price_asc');

    const compiled = fixture.nativeElement as HTMLElement;
    const applyButton = compiled.querySelector('[data-testid="apply-filters"]') as HTMLButtonElement;
    applyButton.click();
    fixture.detectChanges();

    expect(navigate).toHaveBeenCalledWith(
      [],
      expect.objectContaining({
        queryParams: expect.objectContaining({
          q: 'hammer',
          categoryId: 1,
          sort: 'price_asc',
          page: 0,
        }),
      }),
    );

    httpMock.expectOne((r) => r.url === '/api/catalog/products').flush(buildPage([PRODUCT]));
  });

  it('issues exactly one products request per Apply filters / pagination action, via the queryParamMap stream', async () => {
    const { fixture } = setupNavigable();
    fixture.detectChanges();
    flushCategories();
    fixture.detectChanges();
    httpMock
      .expectOne((r) => r.url === '/api/catalog/products')
      .flush(buildPage([PRODUCT], { totalPages: 3 }));
    fixture.detectChanges();
    await fixture.whenStable();

    const compiled = fixture.nativeElement as HTMLElement;
    const applyButton = compiled.querySelector('[data-testid="apply-filters"]') as HTMLButtonElement;
    applyButton.click();
    fixture.detectChanges();

    let matched = httpMock.match((r) => r.url === '/api/catalog/products');
    expect(matched.length).toBe(1);
    matched[0].flush(buildPage([PRODUCT], { totalPages: 3 }));
    fixture.detectChanges();
    await fixture.whenStable();

    const nextButton = Array.from(compiled.querySelectorAll('button')).find(
      (button) => button.textContent?.trim() === 'Next',
    ) as HTMLButtonElement;
    nextButton.click();
    fixture.detectChanges();

    matched = httpMock.match((r) => r.url === '/api/catalog/products');
    expect(matched.length).toBe(1);
    matched[0].flush(buildPage([PRODUCT], { page: 1, totalPages: 3 }));
  });

  it('cancels a superseded products request when filters change before the previous one resolves', async () => {
    const { fixture, paramsSubject } = setupNavigable();
    fixture.detectChanges();
    flushCategories();
    fixture.detectChanges();

    const firstRequest = httpMock.expectOne((r) => r.url === '/api/catalog/products');

    paramsSubject.next(new Map([['q', 'hammer']]));
    fixture.detectChanges();

    expect(firstRequest.cancelled).toBeTruthy();

    const secondRequest = httpMock.expectOne((r) => r.url === '/api/catalog/products');
    secondRequest.flush(buildPage([PRODUCT]));
    fixture.detectChanges();
    await fixture.whenStable();

    expect(fixture.componentInstance.state()).toBe('ready');
  });

  it('renders the current page as 1-based "Page N of M"', async () => {
    const fixture = setup({ page: '1' });
    fixture.detectChanges();
    flushCategories();
    fixture.detectChanges();

    httpMock
      .expectOne((r) => r.url === '/api/catalog/products')
      .flush(buildPage([PRODUCT], { page: 1, totalPages: 3 }));
    fixture.detectChanges();
    await fixture.whenStable();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Page 2 of 3');
  });
});
