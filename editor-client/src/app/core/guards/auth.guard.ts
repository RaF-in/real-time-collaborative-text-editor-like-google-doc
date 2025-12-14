import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { TokenService } from '../services/token.service';
import { map, catchError, of } from 'rxjs';

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const tokenService = inject(TokenService);
  const router = inject(Router);

  console.log('AuthGuard: Checking access for URL:', state.url);
  console.log('AuthGuard: Has token:', tokenService.hasToken());
  console.log('AuthGuard: Token expired:', tokenService.isTokenExpired());

  // Check if user has valid token
  if (!tokenService.hasToken() || tokenService.isTokenExpired()) {
    // No token or expired - redirect to login
    console.log('AuthGuard: Redirecting to login with returnUrl:', state.url);
    router.navigate(['/auth/login'], {
      queryParams: { returnUrl: state.url }
    });
    return false;
  }

  // Token exists - verify with backend
  console.log('AuthGuard: isAuthenticated value:', authService.isAuthenticated());
  if (!authService.isAuthenticated()) {
    console.log('AuthGuard: User not authenticated, fetching current user...');
    return authService.getCurrentUser().pipe(
      map(response => {
        console.log('AuthGuard: getCurrentUser response:', response);
        if (response.success) {
          return true;
        }
        router.navigate(['/auth/login'], {
          queryParams: { returnUrl: state.url }
        });
        return false;
      }),
      catchError(() => {
        console.log('AuthGuard: getCurrentUser failed, redirecting to login');
        router.navigate(['/auth/login'], {
          queryParams: { returnUrl: state.url }
        });
        return of(false);
      })
    );
  }

  return true;
};