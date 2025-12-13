// src/app/app.config.ts
import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter, withComponentInputBinding, withInMemoryScrolling } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { csrfInterceptor } from './core/interceptors/csrf.interceptor';
import { errorInterceptor } from './core/interceptors/error.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(
      routes,
      withComponentInputBinding(), // Bind route params to component inputs
      withInMemoryScrolling({      // Scroll to top on navigation
        scrollPositionRestoration: 'top'
      })
    ),
    provideHttpClient(
      withInterceptors([
        authInterceptor,    
        csrfInterceptor,    
        errorInterceptor    
      ])
    )
  ]
};