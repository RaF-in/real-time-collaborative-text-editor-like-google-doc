import { HttpInterceptorFn } from '@angular/common/http';
import { CookieUtil } from '../utils/cookie.util';

export const csrfInterceptor: HttpInterceptorFn = (req, next) => {
  // Only add CSRF token to refresh endpoint
  if (req.url.includes('/auth/refresh')) {
    const csrfToken = CookieUtil.getCsrfToken();
    
    if (csrfToken) {
      const cloned = req.clone({
        setHeaders: {
          'X-CSRF-TOKEN': csrfToken
        }
      });
      return next(cloned);
    } else {
      console.warn('CSRF token not found for refresh request');
    }
  }

  return next(req);
};