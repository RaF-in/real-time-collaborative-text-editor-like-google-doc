import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { Router } from '@angular/router';
import { TokenService } from '../services/token.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const tokenService = inject(TokenService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      let errorMessage = 'An error occurred';

      if (error.error instanceof ErrorEvent) {
        // Client-side error
        errorMessage = `Error: ${error.error.message}`;
      } else {
        // Server-side error
        if (error.status === 401) {
          // Unauthorized - clear token and redirect to login
          tokenService.removeAccessToken();
          router.navigate(['/auth/login']);
          errorMessage = 'Session expired. Please login again.';
        } else if (error.status === 403) {
          // Forbidden - CSRF or permission error
          if (error.error?.message) {
            errorMessage = error.error.message;
          } else {
            errorMessage = 'Access denied';
          }
        } else if (error.status === 423) {
          // Account locked
          errorMessage = error.error?.message || 'Account is locked';
        } else if (error.status === 0) {
          // Network error
          errorMessage = 'Network error. Please check your connection.';
        } else {
          // Other server errors
          errorMessage = error.error?.message || `Server error: ${error.status}`;
        }
      }

      console.error('HTTP Error:', errorMessage, error);
      return throwError(() => ({ message: errorMessage, originalError: error }));
    })
  );
};