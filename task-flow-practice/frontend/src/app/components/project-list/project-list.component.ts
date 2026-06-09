import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ProjectService } from '../../services/project.service';
import { Project } from '../../models/project.model';

@Component({
  selector: 'app-project-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './project-list.component.html'
})
export class ProjectListComponent implements OnInit {
  private projectService = inject(ProjectService);
  private fb = inject(FormBuilder);

  projects: Project[] = [];
  loadError = '';
  showForm = false;
  editingProject: Project | null = null;
  deleteErrors: Record<number, string> = {};

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
      next: (data) => (this.projects = data),
      error: () => (this.loadError = 'Failed to load projects.')
    });
  }

  openCreate(): void {
    this.editingProject = null;
    this.initForm();
    this.showForm = true;
  }

  openEdit(project: Project): void {
    this.editingProject = project;
    this.initForm(project);
    this.showForm = true;
  }

  cancelForm(): void {
    this.showForm = false;
    this.editingProject = null;
  }

  onSubmit(): void {
    if (this.form.invalid) return;
    const value = this.form.value;
    const payload = {
      name: value.name.trim(),
      description: value.description || undefined
    };

    if (this.editingProject) {
      this.projectService.updateProject(this.editingProject.id, payload).subscribe({
        next: (updated) => {
          const idx = this.projects.findIndex(p => p.id === updated.id);
          if (idx !== -1) this.projects[idx] = updated;
          this.cancelForm();
        }
      });
    } else {
      this.projectService.createProject(payload).subscribe({
        next: (created) => {
          this.projects = [...this.projects, created];
          this.cancelForm();
        }
      });
    }
  }

  deleteProject(project: Project): void {
    delete this.deleteErrors[project.id];
    this.projectService.deleteProject(project.id).subscribe({
      next: () => {
        this.projects = this.projects.filter(p => p.id !== project.id);
      },
      error: (err) => {
        if (err.status === 409) {
          this.deleteErrors = {
            ...this.deleteErrors,
            [project.id]: 'Project has tasks — delete tasks first'
          };
        } else {
          this.deleteErrors = {
            ...this.deleteErrors,
            [project.id]: 'Failed to delete project.'
          };
        }
      }
    });
  }
}
