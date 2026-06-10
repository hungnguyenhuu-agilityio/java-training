import { Component, OnInit, OnDestroy, Output, EventEmitter, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { ProjectService } from '../../services/project.service';
import { Project } from '../../models/project.model';

export interface FilterParams {
  status?: string;
  priority?: string;
  projectId?: number;
  searchTerm?: string;
}

@Component({
  selector: 'app-filter-bar',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './filter-bar.component.html',
  styleUrls: ['./filter-bar.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FilterBarComponent implements OnInit, OnDestroy {
  private projectService = inject(ProjectService);

  @Output() filtersChanged = new EventEmitter<FilterParams>();

  // Two-way bound via [(ngModel)] — plain properties work fine with OnPush
  // because ngModel event handling marks the view dirty automatically.
  status = '';
  priority = '';
  projectId: number | '' = '';
  searchTerm = '';

  readonly projects = signal<Project[]>([]);

  private apiFilters$ = new Subject<FilterParams>();
  private sub = new Subscription();

  ngOnInit(): void {
    this.projectService.getProjects().subscribe({
      next: projects => this.projects.set(projects),
      error: () => this.projects.set([])
    });

    this.sub.add(
      this.apiFilters$.pipe(debounceTime(300), distinctUntilChanged((a, b) =>
        a.status === b.status && a.priority === b.priority && a.projectId === b.projectId
      )).subscribe(filters => {
        this.filtersChanged.emit({ ...filters, searchTerm: this.searchTerm });
      })
    );
  }

  ngOnDestroy(): void {
    this.sub.unsubscribe();
  }

  onDropdownChange(): void {
    const filters: FilterParams = {};
    if (this.status) filters.status = this.status;
    if (this.priority) filters.priority = this.priority;
    if (this.projectId !== '') filters.projectId = +this.projectId;
    this.apiFilters$.next(filters);
  }

  onSearchChange(): void {
    const filters: FilterParams = {};
    if (this.status) filters.status = this.status;
    if (this.priority) filters.priority = this.priority;
    if (this.projectId !== '') filters.projectId = +this.projectId;
    filters.searchTerm = this.searchTerm;
    this.filtersChanged.emit(filters);
  }

  clearFilters(): void {
    this.status = '';
    this.priority = '';
    this.projectId = '';
    this.searchTerm = '';
    this.filtersChanged.emit({});
  }
}
