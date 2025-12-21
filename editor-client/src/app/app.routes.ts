// src/app/app.routes.ts
import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { noAuthGuard } from './core/guards/no-auth.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: '/home',
    pathMatch: 'full'
  },
  {
    path: 'home',
    canActivate: [authGuard],
    loadComponent: () => import('./features/homepage/homepage.component')
      .then(m => m.HomepageComponent)
  },
  {
    path: 'auth',
    canActivate: [noAuthGuard],
    children: [
      {
        path: 'login',
        loadComponent: () => import('./features/auth/components/login/login.component')
          .then(m => m.LoginComponent)
      },
      {
        path: 'signup',
        loadComponent: () => import('./features/auth/components/signup/signup.component')
          .then(m => m.SignupComponent)
      },
      {
        path: 'callback',
        loadComponent: () => import('./features/auth/components/oauth2-callback/oauth-callback.component')
          .then(m => m.OAuthCallbackComponent)
      },
      {
        path: '',
        redirectTo: 'login',
        pathMatch: 'full'
      }
  ]
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () => import('./features/dashboard/dashboard.component')
      .then(m => m.DashboardComponent)
  },
  {
    path: 'editor',
    loadChildren: () => import('./features/editor/editor-routes')
      .then(m => m.EDITOR_ROUTES)
  },
  {
    path: 'documents',
    canActivate: [authGuard],
    loadComponent: () => import('./features/homepage/homepage.component')
      .then(m => m.HomepageComponent)
  },
   {
    path: 'documents/:id/request-access',
    canActivate: [authGuard],
    loadComponent: () => import('./features/sharing/components/request-access/request-access.component')
      .then(m => m.RequestAccessComponent)
  },
  {
    path: 'link/:token',
    canActivate: [authGuard],
    loadComponent: () => import('./features/sharing/components/share-link-redirect/share-link-redirect.component')
      .then(m => m.ShareLinkRedirectComponent)
  },
    {
    path: 'no-access',
    loadComponent: () => import('./features/sharing/components/no-access/no-access.component')
      .then(m => m.NoAccessComponent)
  },
  {
    path: '**',
    redirectTo: '/home'
  }
];