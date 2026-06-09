import { Component, Input, Output, EventEmitter, ViewChildren, QueryList } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Task, TaskStatus } from '../../models/task.model';
import { TaskCardComponent } from '../task-card/task-card.component';

@Component({
  selector: 'app-kanban-column',
  standalone: true,
  imports: [CommonModule, TaskCardComponent],
  templateUrl: './kanban-column.component.html'
})
export class KanbanColumnComponent {
  @Input() title = '';
  @Input() tasks: Task[] = [];
  @Output() statusChanged = new EventEmitter<{ task: Task; nextStatus: TaskStatus }>();

  @ViewChildren(TaskCardComponent) cards!: QueryList<TaskCardComponent>;

  onCardStatusChanged(event: { task: Task; nextStatus: TaskStatus }): void {
    this.statusChanged.emit(event);
  }

  /** Reset the moving flag on the card matching the given task id */
  resetCard(taskId: number): void {
    const card = this.cards?.find(c => c.task.id === taskId);
    card?.resetMoving();
  }
}
