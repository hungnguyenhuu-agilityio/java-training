import { Component, ElementRef, ViewChild } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatListModule } from '@angular/material/list';
import { MatSidenav, MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { RouterLink, RouterOutlet } from '@angular/router';

@Component({
  imports: [
    MatButtonModule,
    MatListModule,
    MatSidenavModule,
    MatToolbarModule,
    RouterLink,
    RouterOutlet,
  ],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App {
  @ViewChild('menuButton', { read: ElementRef })
  private menuButton!: ElementRef<HTMLButtonElement>;

  skipToMainContent(event: Event, mainContent: HTMLElement): void {
    event.preventDefault();
    mainContent.focus();
  }

  async closeCompactNavigation(
    drawer: MatSidenav,
    event: Event,
  ): Promise<void> {
    if (!drawer.opened) {
      return;
    }

    event.preventDefault();
    event.stopPropagation();
    await drawer.close();
    this.menuButton.nativeElement.focus();
  }
}
