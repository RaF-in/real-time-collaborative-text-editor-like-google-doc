// src/app/features/editor/components/editor-container/editor-container.component.ts
import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { Subject, takeUntil } from 'rxjs';
import { EditorStateService } from '../../services/editor-state.service';
import { EditorContentComponent } from '../editor-content/editor-content.component';
import { CollaboratorsPanelComponent } from '../collaborators-panel/collaborators-panel.component';
import { EditorToolbarComponent } from '../editor-toolbar/editor-toolbar.component';

@Component({
  selector: 'app-editor-container',
  standalone: true,
  imports: [
    CommonModule,
    EditorToolbarComponent,
    EditorContentComponent,
    CollaboratorsPanelComponent
  ],
  providers: [EditorStateService],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './editor-container.component.html',
  styleUrls: ['./editor-container.component.css']
})
export class EditorContainerComponent implements OnInit, OnDestroy {
  readonly showDebug = signal(false);
  
  private destroy$ = new Subject<void>();
  
  constructor(
    private route: ActivatedRoute,
    readonly editorState: EditorStateService
  ) {}
  
  ngOnInit(): void {
    const docId = this.route.snapshot.paramMap.get('id') || 'doc123';
    const userId = this.generateUserId();
    
    this.editorState.connectToDocument(docId, userId).catch(error => {
      console.error('Connection error:', error);
    });
  }
  
  ngOnDestroy(): void {
    this.editorState.disconnect();
    this.destroy$.next();
    this.destroy$.complete();
  }
  
  onContentChange(event: { type: 'insert' | 'delete'; char?: string; position: number }): void {
    if (this.editorState.isProcessingRemote()) return;
    
    if (event.type === 'insert' && event.char) {
      this.editorState.insertCharacter(event.char, event.position);
    } else if (event.type === 'delete') {
      this.editorState.deleteCharacter(event.position);
    }
  }
  
  toggleDebug(): void {
    this.showDebug.update(show => !show);
  }
  
  private generateUserId(): string {
    return 'user-' + Math.random().toString(36).substr(2, 9);
  }
}