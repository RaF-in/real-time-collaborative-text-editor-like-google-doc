import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class TokenService {
  private readonly ACCESS_TOKEN_KEY = 'access_token';
  private readonly TOKEN_EXPIRY_KEY = 'token_expiry';
  
  // Signal for token expiry
  private tokenExpirySignal = signal<number | null>(null);
  
  /**
   * Store access token
   */
  setAccessToken(token: string, expiresIn: number): void {
    localStorage.setItem(this.ACCESS_TOKEN_KEY, token);
    const expiryTime = Date.now() + (expiresIn * 1000);
    localStorage.setItem(this.TOKEN_EXPIRY_KEY, expiryTime.toString());
    this.tokenExpirySignal.set(expiryTime);
  }

  /**
   * Get access token
   */
  getAccessToken(): string | null {
    return localStorage.getItem(this.ACCESS_TOKEN_KEY);
  }

  /**
   * Remove access token
   */
  removeAccessToken(): void {
    localStorage.removeItem(this.ACCESS_TOKEN_KEY);
    localStorage.removeItem(this.TOKEN_EXPIRY_KEY);
    this.tokenExpirySignal.set(null);
  }

  /**
   * Check if token exists
   */
  hasToken(): boolean {
    return !!this.getAccessToken();
  }

  /**
   * Check if token is expired
   */
  isTokenExpired(): boolean {
    const expiryTime = localStorage.getItem(this.TOKEN_EXPIRY_KEY);
    if (!expiryTime) {
      return true;
    }
    return Date.now() >= parseInt(expiryTime, 10);
  }

  /**
   * Get token expiry signal (readonly)
   */
  get tokenExpiry() {
    return this.tokenExpirySignal.asReadonly();
  }
}