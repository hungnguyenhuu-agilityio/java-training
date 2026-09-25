import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { catchError, of, switchMap } from 'rxjs';
import { CatalogService } from '../catalog.service';
import { Category, Page, ProblemDetails, ProductSort, ProductSummary } from '../models/catalog.model';

const DEFAULT_SIZE = 12;

const SORT_OPTIONS: { value: ProductSort; label: string }[] = [
  { value: 'name_asc', label: 'Name (A to Z)' },
  { value: 'name_desc', label: 'Name (Z to A)' },
  { value: 'price_asc', label: 'Price (low to high)' },
  { value: 'price_desc', label: 'Price (high to low)' },
  { value: 'newest', label: 'Newest' },
];

type ViewState = 'loading' | 'ready' | 'invalid-filter' | 'server-failure';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule,
  ],
  templateUrl: './product-list.component.html',
  styleUrl: './product-list.component.css',
})
export class ProductListComponent implements OnInit {
  readonly sortOptions = SORT_OPTIONS;

  readonly searchControl = new FormControl('', { nonNullable: true });
  readonly categoryControl = new FormControl<number | null>(null);
  readonly sortControl = new FormControl<ProductSort>('name_asc', { nonNullable: true });

  readonly categories = signal<Category[]>([]);
  readonly page = signal<Page<ProductSummary> | null>(null);
  readonly state = signal<ViewState>('loading');
  readonly errorDetail = signal('');
  readonly violations = signal<{ field: string; message: string }[]>([]);
  readonly hasAppliedFilters = signal(false);

  private currentPage = 0;
  private appliedQuery = '';
  private appliedCategoryId: number | null = null;
  private appliedSort: ProductSort = 'name_asc';

  constructor(
    private readonly catalogService: CatalogService,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
  ) {}

  ngOnInit(): void {
    this.catalogService.listCategories().subscribe((categories) => {
      this.categories.set(categories);
    });

    this.route.queryParamMap
      .pipe(
        switchMap((params) => {
          const q = params.get('q') ?? '';
          const categoryId = params.get('categoryId');
          const sort = (params.get('sort') as ProductSort | null) ?? 'name_asc';
          const page = params.get('page');

          this.searchControl.setValue(q);
          this.categoryControl.setValue(categoryId ? Number(categoryId) : null);
          this.sortControl.setValue(sort);

          this.appliedQuery = q;
          this.appliedCategoryId = categoryId ? Number(categoryId) : null;
          this.appliedSort = sort;
          this.currentPage = page ? Number(page) : 0;
          this.hasAppliedFilters.set(Boolean(q || categoryId));

          return this.fetchProducts();
        }),
      )
      .subscribe((page) => {
        if (page) {
          this.page.set(page);
          this.state.set('ready');
        }
      });
  }

  pageNumberLabel(): string {
    const page = this.page();
    if (!page) {
      return '';
    }
    const totalPages = Math.max(page.totalPages, 1);
    return `Page ${page.page + 1} of ${totalPages}`;
  }

  isFirstPage(): boolean {
    return this.currentPage <= 0;
  }

  isLastPage(): boolean {
    const page = this.page();
    return !page || this.currentPage >= page.totalPages - 1;
  }

  applyFilters(): void {
    this.appliedQuery = this.searchControl.value.trim();
    this.appliedCategoryId = this.categoryControl.value;
    this.appliedSort = this.sortControl.value;
    this.currentPage = 0;
    this.hasAppliedFilters.set(Boolean(this.appliedQuery || this.appliedCategoryId));
    this.navigateToCurrentQuery();
  }

  goToPreviousPage(): void {
    if (this.isFirstPage()) {
      return;
    }
    this.currentPage -= 1;
    this.navigateToCurrentQuery();
  }

  goToNextPage(): void {
    if (this.isLastPage()) {
      return;
    }
    this.currentPage += 1;
    this.navigateToCurrentQuery();
  }

  retry(): void {
    this.fetchProducts().subscribe((page) => {
      if (page) {
        this.page.set(page);
        this.state.set('ready');
      }
    });
  }

  private navigateToCurrentQuery(): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        q: this.appliedQuery || null,
        categoryId: this.appliedCategoryId,
        sort: this.appliedSort,
        page: this.currentPage,
      },
      queryParamsHandling: 'merge',
    });
  }

  private fetchProducts() {
    this.state.set('loading');
    this.violations.set([]);

    return this.catalogService
      .listProducts({
        q: this.appliedQuery || undefined,
        categoryId: this.appliedCategoryId ?? undefined,
        sort: this.appliedSort,
        page: this.currentPage,
        size: DEFAULT_SIZE,
      })
      .pipe(
        catchError((error: HttpErrorResponse) => {
          const problem = error.error as ProblemDetails | undefined;
          if (error.status === 400) {
            this.state.set('invalid-filter');
            this.violations.set(problem?.violations ?? []);
            this.errorDetail.set(problem?.detail ?? 'The requested filters are invalid.');
          } else {
            this.state.set('server-failure');
            this.errorDetail.set('Something went wrong loading the catalog. Please try again.');
          }
          return of(null);
        }),
      );
  }
}
