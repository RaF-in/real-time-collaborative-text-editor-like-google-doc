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
  changeDetection: ChangeDetectionStrategy.OnPush, // OnPush strategy
  template: `
    <div class="editor-container">
      <!-- Header -->
      <div class="editor-header">
        <div class="header-left">
          <div class="doc-icon">📄</div>
          <div class="doc-info">
            <input 
              type="text" 
              class="doc-title" 
              [value]="editorState.docId() || 'Untitled Document'"
              placeholder="Untitled document"
            />
            <div class="doc-metadata">
              <span class="status-badge" [class.connected]="editorState.isConnected()">
                {{ editorState.isConnected() ? '● Connected' : '○ Disconnected' }}
              </span>
              @if (editorState.currentServerId(); as serverId) {
                <span class="server-info">
                  Server: {{ serverId }}
                </span>
              }
            </div>
          </div>
        </div>
        
        <div class="header-right">
          <app-collaborators-panel [collaborators]="editorState.collaborators()" />
          <button class="btn-share">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
              <path d="M18 8C19.6569 8 21 6.65685 21 5C21 3.34315 19.6569 2 18 2C16.3431 2 15 3.34315 15 5C15 6.65685 16.3431 8 18 8Z" stroke="currentColor" stroke-width="2"/>
              <path d="M6 15C7.65685 15 9 13.6569 9 12C9 10.3431 7.65685 9 6 9C4.34315 9 3 10.3431 3 12C3 13.6569 4.34315 15 6 15Z" stroke="currentColor" stroke-width="2"/>
              <path d="M18 22C19.6569 22 21 20.6569 21 19C21 17.3431 19.6569 16 18 16C16.3431 16 15 17.3431 15 19C15 20.6569 16.3431 22 18 22Z" stroke="currentColor" stroke-width="2"/>
              <path d="M8.59 13.51L15.42 17.49M15.41 6.51L8.59 10.49" stroke="currentColor" stroke-width="2"/>
            </svg>
            Share
          </button>
        </div>
      </div>
      
      <!-- Toolbar -->
      <app-editor-toolbar />
      
      <!-- Main Content Area -->
      <div class="editor-main">
        <!-- Loading State -->
        @if (editorState.isLoading()) {
          <div class="loading-overlay">
            <div class="spinner"></div>
            <p>Loading document...</p>
          </div>
        }
        
        <!-- Error State -->
        @if (editorState.error(); as error) {
          <div class="error-banner">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
              <path d="M10 0C4.48 0 0 4.48 0 10s4.48 10 10 10 10-4.48 10-10S15.52 0 10 0zm1 15H9v-2h2v2zm0-4H9V5h2v6z" fill="currentColor"/>
            </svg>
            {{ error }}
          </div>
        }
        
        <!-- Editor Content -->
        <app-editor-content 
          [content]="editorState.content()"
          [isConnected]="editorState.isConnected()"
          (contentChange)="onContentChange($event)"
        />
        
        <!-- Version Vector Display (Debug) -->
        @if (showDebug() && editorState.versionVectorEntries().length > 0) {
          <div class="version-vector-display">
            <div class="vector-title">Version Vector:</div>
            <div class="vector-badges">
              @for (entry of editorState.versionVectorEntries(); track entry.server) {
                <span class="vector-badge">
                  {{ entry.server }}: {{ entry.seq }}
                </span>
              }
            </div>
          </div>
        }
      </div>
      
      <!-- Footer -->
      <div class="editor-footer">
        <div class="footer-left">
          <span class="char-count">{{ editorState.characterCount() }} characters</span>
          <span class="word-count">{{ editorState.wordCount() }} words</span>
        </div>
        <div class="footer-right">
          <button class="btn-icon" (click)="toggleDebug()">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
              <path d="M10 2a8 8 0 100 16 8 8 0 000-16zm0 14a6 6 0 110-12 6 6 0 010 12zm-1-9h2v4h-2V7zm0 5h2v2h-2v-2z"/>
            </svg>
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .editor-container {
      display: flex;
      flex-direction: column;
      height: 100vh;
      background: #f9fbfd;
    }
    
    .editor-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 24px;
      background: white;
      border-bottom: 1px solid #e0e0e0;
      box-shadow: 0 1px 2px rgba(0,0,0,0.05);
    }
    
    .header-left {
      display: flex;
      align-items: center;
      gap: 12px;
      flex: 1;
    }
    
    .doc-icon {
      font-size: 24px;
    }
    
    .doc-info {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }
    
    .doc-title {
      font-size: 18px;
      font-weight: 500;
      border: none;
      outline: none;
      padding: 4px 8px;
      border-radius: 4px;
      transition: background 0.2s;
    }
    
    .doc-title:hover {
      background: #f5f5f5;
    }
    
    .doc-title:focus {
      background: #e8f0fe;
    }
    
    .doc-metadata {
      display: flex;
      gap: 12px;
      font-size: 12px;
      color: #5f6368;
    }
    
    .status-badge {
      display: flex;
      align-items: center;
      gap: 4px;
      padding: 2px 8px;
      border-radius: 12px;
      background: #f44336;
      color: white;
      font-weight: 500;
    }
    
    .status-badge.connected {
      background: #4caf50;
    }
    
    .header-right {
      display: flex;
      align-items: center;
      gap: 12px;
    }
    
    .btn-share {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 8px 16px;
      background: #1a73e8;
      color: white;
      border: none;
      border-radius: 24px;
      font-weight: 500;
      cursor: pointer;
      transition: background 0.2s;
    }
    
    .btn-share:hover {
      background: #1557b0;
    }
    
    .editor-main {
      flex: 1;
      display: flex;
      flex-direction: column;
      overflow: hidden;
      position: relative;
    }
    
    .loading-overlay {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      background: rgba(255, 255, 255, 0.9);
      z-index: 1000;
    }
    
    .spinner {
      width: 40px;
      height: 40px;
      border: 3px solid #e0e0e0;
      border-top-color: #1a73e8;
      border-radius: 50%;
      animation: spin 1s linear infinite;
    }
    
    @keyframes spin {
      to { transform: rotate(360deg); }
    }
    
    .error-banner {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px 24px;
      background: #fef7e0;
      color: #8a6116;
      border-bottom: 1px solid #f9e6a3;
    }
    
    .version-vector-display {
      position: fixed;
      bottom: 60px;
      left: 24px;
      padding: 12px 16px;
      background: white;
      border: 1px solid #e0e0e0;
      border-radius: 8px;
      box-shadow: 0 2px 8px rgba(0,0,0,0.1);
      z-index: 100;
    }
    
    .vector-title {
      font-size: 11px;
      font-weight: 600;
      color: #5f6368;
      margin-bottom: 8px;
    }
    
    .vector-badges {
      display: flex;
      gap: 6px;
      flex-wrap: wrap;
    }
    
    .vector-badge {
      padding: 4px 10px;
      background: #1a73e8;
      color: white;
      border-radius: 12px;
      font-size: 11px;
      font-weight: 500;
    }
    
    .editor-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 8px 24px;
      background: white;
      border-top: 1px solid #e0e0e0;
      font-size: 12px;
      color: #5f6368;
    }
    
    .footer-left {
      display: flex;
      gap: 16px;
    }
    
    .btn-icon {
      width: 32px;
      height: 32px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: none;
      border: none;
      border-radius: 50%;
      cursor: pointer;
      color: #5f6368;
      transition: background 0.2s;
    }
    
    .btn-icon:hover {
      background: #f5f5f5;
    }
  `]
})
export class EditorContainerComponent implements OnInit, OnDestroy {
  // Signal for component-specific state
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