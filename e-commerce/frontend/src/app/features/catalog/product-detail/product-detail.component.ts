import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CatalogService } from '../catalog.service';
import { ProductDetail } from '../models/catalog.model';

type ViewState = 'loading' | 'ready' | 'not-found' | 'server-failure';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, MatButtonModule],
  templateUrl: './product-detail.component.html',
  styleUrl: './product-detail.component.css',
})
export class ProductDetailComponent implements OnInit {
  readonly state = signal<ViewState>('loading');
  readonly product = signal<ProductDetail | null>(null);

  constructor(
    private readonly catalogService: CatalogService,
    private readonly route: ActivatedRoute,
  ) {}

  private currentId: number | null = null;

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      const id = Number(params.get('id'));
      this.currentId = id;
      this.loadProduct(id);
    });
  }

  retry(): void {
    if (this.currentId != null) {
      this.loadProduct(this.currentId);
    }
  }

  private loadProduct(id: number): void {
    this.state.set('loading');

    this.catalogService.getProduct(id).subscribe({
      next: (product) => {
        this.product.set(product);
        this.state.set('ready');
      },
      error: (error: HttpErrorResponse) => {
        if (error.status === 404 || error.status === 400) {
          this.state.set('not-found');
        } else {
          this.state.set('server-failure');
        }
      },
    });
  }
}
