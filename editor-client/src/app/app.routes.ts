// src/app/app.routes.ts
import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    redirectTo: '/editor/doc123',
    pathMatch: 'full'
  },
  {
    path: 'editor',
    loadChildren: () => import('./features/editor/editor-routes')
      .then(m => m.EDITOR_ROUTES)
  },
  {
    path: 'documents',
    loadComponent: () => import('./features/document-list/document-list.component')
      .then(m => m.DocumentListComponent)
  },
  {
    path: '**',
    redirectTo: '/editor/doc123'
  }
];