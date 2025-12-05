// src/app/features/document-list/document-list.component.ts
import { Component, ChangeDetectionStrategy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

interface DocumentItem {
  id: string;
  title: string;
  lastModified: Date;
  owner: string;
}

@Component({
  selector: 'app-document-list',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './document-list.component.html',
  styleUrls: ['./document-list.component.css']
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