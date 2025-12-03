// src/app/features/document-list/document-list.component.ts
import { Component, ChangeDetectionStrategy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

interface DocumentItem {
  id: string;
  title: string;
  lastModified: Date;
  owner: string;
}

@Component({
  selector: 'app-document-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="document-list-container">
      <div class="header">
        <h1>My Documents</h1>
        <button class="btn-new" (click)="createNewDocument()">
          <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
            <path d="M10 3v14M3 10h14" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          </svg>
          New Document
        </button>
      </div>
      
      <div class="documents-grid">
        @for (doc of documents(); track doc.id) {
          <a [routerLink]="['/editor', doc.id]" class="document-card">
            <div class="doc-icon">📄</div>
            <div class="doc-info">
              <h3>{{ doc.title }}</h3>
              <p class="doc-meta">
                Last modified: {{ doc.lastModified | date:'short' }}
              </p>
              <p class="doc-owner">Owner: {{ doc.owner }}</p>
            </div>
          </a>
        }
        
        @if (documents().length === 0) {
          <div class="empty-state">
            <div class="empty-icon">📄</div>
            <h3>No documents yet</h3>
            <p>Create your first document to get started</p>
          </div>
        }
      </div>
    </div>
  `,
  styles: [`
    .document-list-container {
      padding: 24px;
      max-width: 1200px;
      margin: 0 auto;
    }
    
    .header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 32px;
    }
    
    h1 {
      font-size: 32px;
      font-weight: 400;
      color: #202124;
    }
    
    .btn-new {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 12px 24px;
      background: #1a73e8;
      color: white;
      border: none;
      border-radius: 24px;
      font-weight: 500;
      cursor: pointer;
      transition: background 0.2s;
    }
    
    .btn-new:hover {
      background: #1557b0;
    }
    
    .documents-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
      gap: 24px;
    }
    
    .document-card {
      display: flex;
      padding: 24px;
      background: white;
      border: 1px solid #e0e0e0;
      border-radius: 8px;
      text-decoration: none;
      color: inherit;
      transition: all 0.2s;
    }
    
    .document-card:hover {
      box-shadow: 0 4px 12px rgba(0,0,0,0.1);
      transform: translateY(-2px);
    }
    
    .doc-icon {
      font-size: 48px;
      margin-right: 16px;
    }
    
    .doc-info {
      flex: 1;
    }
    
    .doc-info h3 {
      font-size: 18px;
      font-weight: 500;
      margin-bottom: 8px;
      color: #202124;
    }
    
    .doc-meta {
      font-size: 14px;
      color: #5f6368;
      margin-bottom: 4px;
    }
    
    .doc-owner {
      font-size: 13px;
      color: #80868b;
    }
    
    .empty-state {
      grid-column: 1 / -1;
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 64px 24px;
      text-align: center;
    }
    
    .empty-icon {
      font-size: 96px;
      opacity: 0.3;
      margin-bottom: 24px;
    }
    
    .empty-state h3 {
      font-size: 24px;
      font-weight: 400;
      color: #202124;
      margin-bottom: 8px;
    }
    
    .empty-state p {
      font-size: 16px;
      color: #5f6368;
    }
  `]
})
export class DocumentListComponent {
  readonly documents = signal<DocumentItem[]>([
    {
      id: 'doc123',
      title: 'Untitled Document',
      lastModified: new Date(),
      owner: 'You'
    }
  ]);
  
  createNewDocument(): void {
    const newDoc: DocumentItem = {
      id: 'doc-' + Date.now(),
      title: 'Untitled Document',
      lastModified: new Date(),
      owner: 'You'
    };
    this.documents.update(docs => [newDoc, ...docs]);
  }
}