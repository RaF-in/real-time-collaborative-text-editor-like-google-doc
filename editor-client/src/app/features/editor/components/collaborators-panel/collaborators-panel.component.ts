// src/app/features/editor/components/collaborators-panel/collaborators-panel.component.ts
import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Collaborator } from '../../../../core/models/collaborator.model';


@Component({
  selector: 'app-collaborators-panel',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (collaborators.length > 0) {
      <div class="collaborators-panel">
        <div class="collaborator-avatars">
          @for (collab of visibleCollaborators; track collab.userId) {
            <div 
              class="avatar" 
              [style.background]="collab.avatarColor"
              [title]="collab.userName"
            >
              {{ getInitials(collab.userName) }}
            </div>
          }
          @if (collaborators.length > 3) {
            <div class="avatar more">
              +{{ collaborators.length - 3 }}
            </div>
          }
        </div>
      </div>
    }
  `,
  styles: [`
    .collaborators-panel {
      display: flex;
      align-items: center;
    }
    
    .collaborator-avatars {
      display: flex;
      align-items: center;
    }
    
    .avatar {
      width: 32px;
      height: 32px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 50%;
      color: white;
      font-weight: 500;
      font-size: 12px;
      border: 2px solid white;
      margin-left: -8px;
      cursor: pointer;
      transition: transform 0.2s;
    }
    
    .avatar:first-child {
      margin-left: 0;
    }
    
    .avatar:hover {
      transform: scale(1.1);
      z-index: 10;
    }
    
    .avatar.more {
      background: #5f6368;
      font-size: 11px;
    }
  `]
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