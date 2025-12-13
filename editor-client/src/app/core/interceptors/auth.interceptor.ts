import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError, switchMap } from 'rxjs';
import { TokenService } from '../services/token.service';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const tokenService = inject(TokenService);
  const authService = inject(AuthService);
  const token = tokenService.getAccessToken();

  // Don't add token to auth endpoints (login, signup, refresh)
  if (req.url.includes('/auth/login') || 
      req.url.includes('/auth/signup') || 
      req.url.includes('/auth/refresh')) {
    return next(req);
  }

  // Clone request with token if available
  let clonedReq = req;
  if (token && !tokenService.isTokenExpired()) {
    clonedReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  // Handle response and auto-refresh on 401
  return next(clonedReq).pipe(
    catchError((error: HttpErrorResponse) => {
      // If 401 and we have a refresh token, try to refresh
      if (error.status === 401 && !req.url.includes('/auth/refresh')) {
        return authService.refreshToken().pipe(
          switchMap((response) => {
            // Retry original request with new token
            const newToken = tokenService.getAccessToken();
            const retryReq = req.clone({
              setHeaders: {
                Authorization: `Bearer ${newToken}`
              }
            });
            return next(retryReq);
          }),
          catchError((refreshError) => {
            // Refresh failed, return original error
            return throwError(() => error);
          })
        );
      }
      return throwError(() => error);
    })
  );
};