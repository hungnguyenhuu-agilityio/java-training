import { Component } from '@angular/core';
import { RouterLink, Routes } from '@angular/router';

@Component({
  selector: 'app-products-placeholder',
  template: `
    <section aria-labelledby="products-title">
      <h1 id="products-title">Products</h1>
      <p>The product catalog will be available in the next delivery slice.</p>
    </section>
  `,
})
export class ProductsPlaceholder {}

@Component({
  selector: 'app-login-placeholder',
  template: `
    <section aria-labelledby="login-title">
      <h1 id="login-title">Log in</h1>
      <p>Authentication will be available in a later delivery slice.</p>
    </section>
  `,
})
export class LoginPlaceholder {}

@Component({
  selector: 'app-not-found-placeholder',
  template: `
    <section aria-labelledby="not-found-title">
      <h1 id="not-found-title">Page not found</h1>
      <p>The requested page does not exist.</p>
      <a routerLink="/products">Return to products</a>
    </section>
  `,
  imports: [RouterLink],
})
export class NotFoundPlaceholder {}

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'products' },
  { path: 'products', component: ProductsPlaceholder, title: 'Products | E-Commerce' },
  { path: 'login', component: LoginPlaceholder, title: 'Log in | E-Commerce' },
  { path: '**', component: NotFoundPlaceholder, title: 'Page not found | E-Commerce' },
];
