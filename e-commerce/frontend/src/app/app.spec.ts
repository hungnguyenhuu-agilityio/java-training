import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS } from '@angular/material/core';
import { provideRouter, Router } from '@angular/router';
import { App } from './app';
import { routes } from './app.routes';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter(routes),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
      ],
    }).compileComponents();
  });

  function openCompactNavigation() {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const menuButton = compiled.querySelector(
      'button[aria-label="Open navigation menu"]',
    ) as HTMLButtonElement;
    const compactNavigation = compiled.querySelector(
      '[data-testid="compact-navigation"]',
    ) as HTMLElement;
    return { fixture, menuButton, compactNavigation };
  }

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('shows only public navigation actions to an anonymous visitor', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.querySelector('mat-toolbar')).toBeTruthy();
    expect(compiled.querySelector('[data-testid="brand"]')?.textContent).toContain('E-Commerce');
    const productLinks = [...compiled.querySelectorAll('a[href="/products"]')];
    expect(productLinks.some((link) => link.textContent?.includes('Products'))).toBe(true);
    expect(compiled.querySelector('a[href="/login"]')?.textContent).toContain('Log in');
    expect(compiled.textContent).not.toContain('Cart');
    expect(compiled.textContent).not.toContain('Orders');
    expect(compiled.textContent).not.toContain('Admin');
  });

  it('redirects the root route to the Products placeholder', async () => {
    const fixture = TestBed.createComponent(App);
    const router = TestBed.inject(Router);

    await router.navigateByUrl('/');
    fixture.detectChanges();
    await fixture.whenStable();

    expect(router.url).toBe('/products');
    expect(fixture.nativeElement.querySelector('h1')?.textContent).toContain('Products');
    expect(fixture.nativeElement.textContent).not.toContain('Congratulations');
  });

  it('resolves the anonymous Log in link to a neutral placeholder route', async () => {
    const fixture = TestBed.createComponent(App);
    const router = TestBed.inject(Router);

    await router.navigateByUrl('/login');
    fixture.detectChanges();
    await fixture.whenStable();

    expect(router.url).toBe('/login');
    expect(fixture.nativeElement.querySelector('h1')?.textContent).toContain('Log in');
    expect(fixture.nativeElement.querySelector('form')).toBeNull();
  });

  it('moves focus without navigating when the skip link is activated', async () => {
    const fixture = TestBed.createComponent(App);
    const router = TestBed.inject(Router);
    await router.navigateByUrl('/login');
    fixture.detectChanges();

    const firstLink = fixture.nativeElement.querySelector('a') as HTMLAnchorElement;
    const main = fixture.nativeElement.querySelector('main') as HTMLElement;

    expect(firstLink.textContent).toContain('Skip to content');
    expect(firstLink.getAttribute('href')).toBe('#main-content');
    expect(main.id).toBe('main-content');
    expect(main.getAttribute('tabindex')).toBe('-1');

    const click = new MouseEvent('click', { bubbles: true, cancelable: true });
    firstLink.dispatchEvent(click);

    expect(click.defaultPrevented).toBe(true);
    expect(router.url).toBe('/login');
    expect(document.activeElement).toBe(main);
  });

  it('renders a useful page for an unknown route', async () => {
    const fixture = TestBed.createComponent(App);
    const router = TestBed.inject(Router);

    await router.navigateByUrl('/nope');
    fixture.detectChanges();
    await fixture.whenStable();

    expect(router.url).toBe('/nope');
    expect(fixture.nativeElement.querySelector('h1')?.textContent).toContain('Page not found');
    expect(document.title).toBe('Page not found | E-Commerce');
  });

  it('opens compact anonymous navigation from an accessible menu control', async () => {
    const { fixture, menuButton, compactNavigation } = openCompactNavigation();

    expect(menuButton).toBeTruthy();
    expect(compactNavigation.tagName).toBe('MAT-SIDENAV');
    expect(menuButton.getAttribute('aria-controls')).toBe(compactNavigation.id);
    expect(menuButton.getAttribute('aria-expanded')).toBe('false');
    expect(compactNavigation.classList).not.toContain('mat-drawer-opened');

    menuButton.click();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(menuButton.getAttribute('aria-expanded')).toBe('true');
    expect(compactNavigation.classList).toContain('mat-drawer-opened');
    expect(compactNavigation.textContent).toContain('Products');
    expect(compactNavigation.textContent).toContain('Log in');
    expect(compactNavigation.textContent).not.toContain('Cart');
    expect(compactNavigation.textContent).not.toContain('Orders');
    expect(compactNavigation.textContent).not.toContain('Admin');
  });

  it('closes compact navigation with Escape and returns focus to the menu control', async () => {
    const { fixture, menuButton, compactNavigation } = openCompactNavigation();

    menuButton.focus();
    menuButton.click();
    fixture.detectChanges();
    await fixture.whenStable();
    expect(compactNavigation.classList).toContain('mat-drawer-opened');

    const escape = new KeyboardEvent('keydown', {
      key: 'Escape',
      keyCode: 27,
      bubbles: true,
      cancelable: true,
    });
    compactNavigation.dispatchEvent(
      escape,
    );
    fixture.detectChanges();
    await fixture.whenStable();

    expect(compactNavigation.classList).not.toContain('mat-drawer-opened');
    expect(menuButton.getAttribute('aria-expanded')).toBe('false');
    expect(document.activeElement).toBe(menuButton);
  });

  it('leaves Escape alone when compact navigation is already closed', async () => {
    const { fixture, menuButton } = openCompactNavigation();
    const mainContent: HTMLElement = fixture.nativeElement.querySelector('#main-content');

    mainContent.focus();
    const escape = new KeyboardEvent('keydown', {
      key: 'Escape',
      keyCode: 27,
      bubbles: true,
      cancelable: true,
    });
    mainContent.dispatchEvent(escape);
    fixture.detectChanges();
    await fixture.whenStable();

    expect(escape.defaultPrevented).toBe(false);
    expect(document.activeElement).not.toBe(menuButton);
  });
});
