import { Component, Input, Output, EventEmitter, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Task, TaskCreateRequest, TaskStatus, TaskPriority } from '../../models/task.model';
import { ProjectService } from '../../services/project.service';
import { Project } from '../../models/project.model';

@Component({
  selector: 'app-task-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './task-form.component.html'
})
export class TaskFormComponent implements OnInit {
  @Input() task: Task | null = null;
  @Input() errorMessage = '';
  @Output() submitted = new EventEmitter<TaskCreateRequest>();
  @Output() cancelled = new EventEmitter<void>();

  private fb = inject(FormBuilder);
  private projectService = inject(ProjectService);

  form!: FormGroup;
  projects: Project[] = [];

  statuses: TaskStatus[] = ['TODO', 'IN_PROGRESS', 'DONE'];
  priorities: TaskPriority[] = ['LOW', 'MEDIUM', 'HIGH'];

  ngOnInit(): void {
    this.form = this.fb.group({
      title: [this.task?.title ?? '', Validators.required],
      description: [this.task?.description ?? ''],
      status: [this.task?.status ?? 'TODO'],
      priority: [this.task?.priority ?? 'MEDIUM'],
      dueDate: [this.task?.dueDate ?? ''],
      projectId: [this.task?.projectId ?? null]
    });

    this.projectService.getProjects().subscribe({
      next: (data) => (this.projects = data),
      error: () => { /* leave projects empty — dropdown stays empty, form still usable */ }
    });
  }

  onSubmit(): void {
    if (this.form.invalid) return;
    const value = this.form.value;
    const payload: TaskCreateRequest = {
      title: value.title.trim(),
      description: value.description || undefined,
      status: value.status,
      priority: value.priority,
      dueDate: value.dueDate || undefined,
      projectId: value.projectId ? Number(value.projectId) : undefined
    };
    this.submitted.emit(payload);
  }

  onCancel(): void {
    this.cancelled.emit();
  }
}
