export class CookieUtil {
  /**
   * Get cookie value by name
   */
  static getCookie(name: string): string | null {
    const nameEQ = name + '=';
    const ca = document.cookie.split(';');
    
    for (let i = 0; i < ca.length; i++) {
      let c = ca[i];
      while (c.charAt(0) === ' ') {
        c = c.substring(1, c.length);
      }
      if (c.indexOf(nameEQ) === 0) {
        return c.substring(nameEQ.length, c.length);
      }
    }
    return null;
  }

  /**
   * Check if cookie exists
   */
  static hasCookie(name: string): boolean {
    return this.getCookie(name) !== null;
  }

  /**
   * Get CSRF token from cookie
   */
  static getCsrfToken(): string | null {
    return this.getCookie('csrf_token');
  }

  /**
   * Check if refresh token cookie exists
   */
  static hasRefreshToken(): boolean {
    return this.hasCookie('refresh_token');
  }
}
