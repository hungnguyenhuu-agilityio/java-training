import { Routes } from '@angular/router';

export const catalogRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./product-list/product-list.component').then((m) => m.ProductListComponent),
    title: 'Products | E-Commerce',
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./product-detail/product-detail.component').then((m) => m.ProductDetailComponent),
    title: 'Product | E-Commerce',
  },
];
