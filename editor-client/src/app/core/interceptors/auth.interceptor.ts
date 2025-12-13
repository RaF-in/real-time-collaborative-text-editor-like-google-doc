import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { TokenService } from '../services/token.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const tokenService = inject(TokenService);
  const token = tokenService.getAccessToken();

  // Don't add token to auth endpoints (login, signup, refresh)
  if (req.url.includes('/auth/login') || 
      req.url.includes('/auth/signup') || 
      req.url.includes('/auth/refresh')) {
    return next(req);
  }

  // Add Authorization header if token exists
  if (token && !tokenService.isTokenExpired()) {
    const cloned = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
    return next(cloned);
  }

  return next(req);
};