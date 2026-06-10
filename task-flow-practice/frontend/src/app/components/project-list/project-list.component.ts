import { Component, OnInit, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ProjectService } from '../../services/project.service';
import { Project } from '../../models/project.model';

@Component({
  selector: 'app-project-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './project-list.component.html',
  styleUrls: ['./project-list.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProjectListComponent implements OnInit {
  private projectService = inject(ProjectService);
  private fb = inject(FormBuilder);

  readonly projects = signal<Project[]>([]);
  readonly loadError = signal('');
  readonly showForm = signal(false);
  readonly editingProject = signal<Project | null>(null);
  readonly deleteErrors = signal<Record<number, string>>({});
  readonly formError = signal('');

  form!: FormGroup;

  ngOnInit(): void {
    this.initForm();
    this.loadProjects();
  }

  private initForm(project?: Project): void {
    this.form = this.fb.group({
      name: [project?.name ?? '', Validators.required],
      description: [project?.description ?? '']
    });
  }

  private loadProjects(): void {
    this.projectService.getProjects().subscribe({
      next: (data) => this.projects.set(data),
      error: () => this.loadError.set('Failed to load projects.')
    });
  }

  openCreate(): void {
    this.editingProject.set(null);
    this.initForm();
    this.showForm.set(true);
  }

  openEdit(project: Project): void {
    this.editingProject.set(project);
    this.initForm(project);
    this.showForm.set(true);
  }

  cancelForm(): void {
    this.showForm.set(false);
    this.editingProject.set(null);
    this.formError.set('');
  }

  onSubmit(): void {
    if (this.form.invalid) return;
    this.formError.set('');
    const value = this.form.value;
    const payload = {
      name: value.name.trim(),
      description: value.description || undefined
    };

    const editing = this.editingProject();
    if (editing) {
      this.projectService.updateProject(editing.id, payload).subscribe({
        next: (updated) => {
          const arr = this.projects();
          const idx = arr.findIndex(p => p.id === updated.id);
          if (idx !== -1) {
            const next = [...arr];
            next[idx] = updated;
            this.projects.set(next);
          }
          this.cancelForm();
        },
        error: (err) => {
          this.formError.set(err?.error?.message ?? 'Failed to update project.');
        }
      });
    } else {
      this.projectService.createProject(payload).subscribe({
        next: (created) => {
          this.projects.update(list => [...list, created]);
          this.cancelForm();
        },
        error: (err) => {
          this.formError.set(err?.error?.message ?? 'Failed to create project.');
        }
      });
    }
  }

  deleteProject(project: Project): void {
    this.deleteErrors.update(errs => {
      const next = { ...errs };
      delete next[project.id];
      return next;
    });
    this.projectService.deleteProject(project.id).subscribe({
      next: () => {
        this.projects.update(list => list.filter(p => p.id !== project.id));
      },
      error: (err) => {
        const msg = err.status === 409
          ? 'Project has tasks — delete tasks first'
          : 'Failed to delete project.';
        this.deleteErrors.update(errs => ({ ...errs, [project.id]: msg }));
      }
    });
  }
}
