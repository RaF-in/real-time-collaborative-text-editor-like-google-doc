// src/app/features/editor/editor.routes.ts
import { Routes } from '@angular/router';

export const EDITOR_ROUTES: Routes = [
  {
    path: ':id',
    loadComponent: () => import('./components/editor-container/editor-container.component')
      .then(m => m.EditorContainerComponent)
  }
];