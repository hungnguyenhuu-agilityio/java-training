import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { MATERIAL_ANIMATIONS } from '@angular/material/core';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { ProductDetailComponent } from './product-detail.component';
import { ProductDetail } from '../models/catalog.model';

const DETAIL: ProductDetail = {
  id: 5,
  name: 'Hammer',
  slug: 'hammer',
  price: 19.99,
  currency: 'USD',
  category: { id: 1, name: 'Tools' },
  description: 'A sturdy hammer.',
};

describe('ProductDetailComponent', () => {
  let httpMock: HttpTestingController;

  function setup(id = '5') {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [ProductDetailComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
        {
          provide: ActivatedRoute,
          useValue: { paramMap: of(new Map([['id', id]])) },
        },
      ],
    });

    httpMock = TestBed.inject(HttpTestingController);
    return TestBed.createComponent(ProductDetailComponent);
  }

  afterEach(() => {
    httpMock.verify();
  });

  it('shows a loading indicator before the product resolves', () => {
    const fixture = setup();
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('[data-testid="detail-loading"]')).toBeTruthy();

    httpMock.expectOne('/api/catalog/products/5').flush(DETAIL);
  });

  it('renders the breadcrumb, name, price, currency, and description', async () => {
    const fixture = setup();
    fixture.detectChanges();

    httpMock.expectOne('/api/catalog/products/5').flush(DETAIL);
    fixture.detectChanges();
    await fixture.whenStable();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('[data-testid="detail-loading"]')).toBeFalsy();
    expect(compiled.querySelector('nav')?.textContent).toContain('Products');
    expect(compiled.querySelector('nav')?.textContent).toContain('Hammer');
    expect(compiled.textContent).toContain('A sturdy hammer.');
    expect(compiled.textContent).not.toContain('Add to cart');
    expect(compiled.querySelector('img')).toBeFalsy();
    const placeholder = compiled.querySelector('.thumbnail');
    expect(placeholder?.tagName).toBe('DIV');
    expect(placeholder?.getAttribute('aria-hidden')).toBe('true');
  });

  it('shows a not-found state for a 404 (inactive or unknown product)', async () => {
    const fixture = setup();
    fixture.detectChanges();

    httpMock
      .expectOne('/api/catalog/products/5')
      .flush({ title: 'Not found' }, { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();
    await fixture.whenStable();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('[data-testid="detail-not-found"]')).toBeTruthy();
  });

  it('shows a server-failure state for a 500', async () => {
    const fixture = setup();
    fixture.detectChanges();

    httpMock
      .expectOne('/api/catalog/products/5')
      .flush({ title: 'Server error' }, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();
    await fixture.whenStable();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('[data-testid="detail-server-failure"]')).toBeTruthy();
  });

  it('retries with the route id after the first load fails, and succeeds', async () => {
    const fixture = setup();
    fixture.detectChanges();

    httpMock
      .expectOne('/api/catalog/products/5')
      .flush({ title: 'Server error' }, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();
    await fixture.whenStable();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('[data-testid="detail-server-failure"]')).toBeTruthy();

    const retryButton = compiled.querySelector('button') as HTMLButtonElement;
    retryButton.click();
    fixture.detectChanges();

    httpMock.expectOne('/api/catalog/products/5').flush(DETAIL);
    fixture.detectChanges();
    await fixture.whenStable();

    expect(compiled.querySelector('[data-testid="detail-server-failure"]')).toBeFalsy();
    expect(compiled.textContent).toContain('Hammer');
  });
});
