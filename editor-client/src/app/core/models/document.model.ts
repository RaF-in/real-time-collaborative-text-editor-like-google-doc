// src/app/core/models/document.model.ts
export interface Document {
  id: string;
  title: string;
  content: string;
  createdAt: Date;
  updatedAt: Date;
  ownerId: string;
  collaborators: string[];
}
