// src/app/features/editor/editor.routes.ts
import { Routes } from '@angular/router';
import { authGuard } from '../../core/guards/auth.guard';

export const EDITOR_ROUTES: Routes = [
  {
    path: ':id',
    loadComponent: () => import('./components/editor-container/editor-container.component')
      .then(m => m.EditorContainerComponent),
    canActivate: [authGuard]
  }
];