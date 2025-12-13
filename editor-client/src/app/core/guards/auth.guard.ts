import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { TokenService } from '../services/token.service';
import { map, catchError, of } from 'rxjs';

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const tokenService = inject(TokenService);
  const router = inject(Router);

  // Check if user has valid token
  if (!tokenService.hasToken() || tokenService.isTokenExpired()) {
    // No token or expired - redirect to login
    router.navigate(['/auth/login'], { 
      queryParams: { returnUrl: state.url } 
    });
    return false;
  }

  // Token exists - verify with backend
  if (!authService.isAuthenticated()) {
    return authService.getCurrentUser().pipe(
      map(response => {
        if (response.success) {
          return true;
        }
        router.navigate(['/auth/login'], { 
          queryParams: { returnUrl: state.url } 
        });
        return false;
      }),
      catchError(() => {
        router.navigate(['/auth/login'], { 
          queryParams: { returnUrl: state.url } 
        });
        return of(false);
      })
    );
  }

  return true;
};