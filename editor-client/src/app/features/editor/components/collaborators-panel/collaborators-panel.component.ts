// src/app/features/editor/components/collaborators-panel/collaborators-panel.component.ts
import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Collaborator } from '../../../../core/models/collaborator.model';

@Component({
  selector: 'app-collaborators-panel',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './collaborators-panel.component.html',
  styleUrls: ['./collaborators-panel.component.css']
})
export class CollaboratorsPanelComponent {
  @Input() collaborators: Collaborator[] = [];
  
  get visibleCollaborators(): Collaborator[] {
    return this.collaborators.slice(0, 3);
  }
  
  getInitials(name: string): string {
    return name
      .split(/[- ]/)
      .map(part => part[0])
      .join('')
      .toUpperCase()
      .slice(0, 2);
  }
}