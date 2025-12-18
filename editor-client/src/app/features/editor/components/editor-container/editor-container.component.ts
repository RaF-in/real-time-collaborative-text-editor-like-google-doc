// src/app/features/editor/components/editor-container/editor-container.component.ts
import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, signal, viewChild, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { Subject, takeUntil } from 'rxjs';
import { EditorStateService } from '../../services/editor-state.service';
import { EditorContentComponent } from '../editor-content/editor-content.component';
import { CollaboratorsPanelComponent } from '../collaborators-panel/collaborators-panel.component';
import { EditorToolbarComponent } from '../editor-toolbar/editor-toolbar.component';
import { MatDialog } from '@angular/material/dialog';
import { MatIcon } from '@angular/material/icon';
import { DocumentSharingService } from '../../../../core/services/document-sharing.service';
import { ShareDialogComponent } from '../../../sharing/components/share-dialog/share-dialog.component';


@Component({
  selector: 'app-editor-container',
  standalone: true,
  imports: [
    CommonModule,
    EditorToolbarComponent,
    EditorContentComponent,
    CollaboratorsPanelComponent,
    MatIcon
],
  providers: [EditorStateService],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './editor-container.component.html',
  styleUrls: ['./editor-container.component.css']
})
export class EditorContainerComponent implements OnInit, OnDestroy {
  readonly showDebug = signal(false);

  // Use viewChild with signal for modern Angular
  readonly editorContent = viewChild<EditorContentComponent>('editorContent');

  private destroy$ = new Subject<void>();
  private isProcessingRemoteOperation = false;
  private readonly dialog = inject(MatDialog);
  private readonly sharingService = inject(DocumentSharingService);
  
  readonly documentId = signal<string>('');
  readonly documentTitle = signal<string>('');
  readonly canShare = signal(false);

  constructor(
    private route: ActivatedRoute,
    readonly editorState: EditorStateService
  ) {}
  
  ngOnInit(): void {
    const docId = this.route.snapshot.paramMap.get('id') || 'doc123';
    const userId = this.generateUserId();
        const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.documentId.set(id);
      this.loadAccessInfo(id);
    }
    this.editorState.connectToDocument(docId, userId).catch(error => {
      console.error('Connection error:', error);
    });
  }

  private loadAccessInfo(documentId: string): void {
    this.sharingService.getAccessInfo(documentId).subscribe(info => {
      this.canShare.set(info.canShare);
      // Load document title from your document service
    });
  }
  
  openShareDialog(): void {
    this.dialog.open(ShareDialogComponent, {
      width: '700px',
      data: {
        documentId: this.documentId(),
        documentTitle: this.documentTitle()
      }
    });
  }
  
  ngOnDestroy(): void {
    this.editorState.disconnect();
    this.destroy$.next();
    this.destroy$.complete();
  }
  
  onContentChange(event: { type: 'insert' | 'delete'; char?: string; position: number }): void {
    // Check if this is during remote operation processing
    if (this.editorState.isProcessingRemote()) {
      // This is likely an echo or remote update, notify the editor component
      const editorComponent = this.editorContent();
      if (editorComponent) {
        editorComponent.markRemoteUpdate();
      }
      return;
    }

    // This is a local operation
    this.isProcessingRemoteOperation = false;

    if (event.type === 'insert' && event.char) {
      this.editorState.insertCharacter(event.char, event.position);
    } else if (event.type === 'delete') {
      this.editorState.deleteCharacter(event.position);
    }
  }

  /**
   * Call this when remote operations are being processed
   */
  private onRemoteOperationStart(): void {
    this.isProcessingRemoteOperation = true;
    const editorComponent = this.editorContent();
    if (editorComponent) {
      editorComponent.markRemoteUpdate();
    }
  }
  
  toggleDebug(): void {
    this.showDebug.update(show => !show);
  }
  
  private generateUserId(): string {
    return 'user-' + Math.random().toString(36).substr(2, 9);
  }
}