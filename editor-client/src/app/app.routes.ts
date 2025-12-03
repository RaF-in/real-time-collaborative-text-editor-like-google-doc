// src/app/app.routes.ts
import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/homepage/homepage.component')
      .then(m => m.HomepageComponent)
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
    redirectTo: '/'
  }
];