import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, catchError, throwError, of } from 'rxjs';
import { 
  LoginRequest, 
  SignupRequest, 
  AuthResponse, 
  RefreshResponse
} from '../models/auth.model';
import { User } from '../models/user.model';
import { TokenService } from './token.service';
import { CookieUtil } from '../utils/cookie.util';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private tokenService = inject(TokenService);
  private router = inject(Router);

  private readonly API_URL = environment.apiUrl;

  // Signals for reactive state management
  private currentUserSignal = signal<User | null>(null);
  private isAuthenticatedSignal = signal<boolean>(false);
  private isLoadingSignal = signal<boolean>(false);

  // Computed signals
  currentUser = this.currentUserSignal.asReadonly();
  isAuthenticated = this.isAuthenticatedSignal.asReadonly();
  isLoading = this.isLoadingSignal.asReadonly();
  
  // Computed: check if user is admin
  isAdmin = computed(() => {
    const user = this.currentUserSignal();
    return user?.roles?.includes('ADMIN') ?? false;
  });

  // Computed: user display name
  userDisplayName = computed(() => {
    const user = this.currentUserSignal();
    return user?.username ?? user?.email ?? 'User';
  });

  constructor() {
    // Initialize authentication state on service creation
    this.initializeAuth();
  }

  /**
   * Initialize authentication state from stored token
   */
  private initializeAuth(): void {
    if (this.tokenService.hasToken() && !this.tokenService.isTokenExpired()) {
      this.loadCurrentUser().subscribe();
    }
  }

  /**
   * Signup new user
   */
  signup(request: SignupRequest): Observable<ApiResponse<AuthResponse>> {
    this.isLoadingSignal.set(true);
    
    return this.http.post<ApiResponse<AuthResponse>>(
      `${this.API_URL}/api/auth/signup`,
      request,
      { withCredentials: true } // Important: send/receive cookies
    ).pipe(
      tap(response => {
        if (response.success && response.data) {
          this.handleAuthSuccess(response.data);
        }
        this.isLoadingSignal.set(false);
      }),
      catchError(error => {
        this.isLoadingSignal.set(false);
        return throwError(() => error);
      })
    );
  }

  /**
   * Login user
   */
  login(request: LoginRequest): Observable<ApiResponse<AuthResponse>> {
    this.isLoadingSignal.set(true);
    
    return this.http.post<ApiResponse<AuthResponse>>(
      `${this.API_URL}/api/auth/login`,
      request,
      { withCredentials: true } // Important: send/receive cookies
    ).pipe(
      tap(response => {
        if (response.success && response.data) {
          this.handleAuthSuccess(response.data);
        }
        this.isLoadingSignal.set(false);
      }),
      catchError(error => {
        this.isLoadingSignal.set(false);
        return throwError(() => error);
      })
    );
  }

  /**
   * Refresh access token using cookie-based refresh token
   */
  refreshToken(): Observable<ApiResponse<RefreshResponse>> {
    const csrfToken = CookieUtil.getCsrfToken();
    
    if (!csrfToken) {
      console.error('CSRF token not found');
      this.handleLogout();
      return throwError(() => new Error('CSRF token not found'));
    }

    return this.http.post<ApiResponse<RefreshResponse>>(
      `${this.API_URL}/api/auth/refresh`,
      {},
      {
        withCredentials: true, // Send refresh_token cookie
        headers: {
          'X-CSRF-TOKEN': csrfToken // CSRF double-submit pattern
        }
      }
    ).pipe(
      tap(response => {
        if (response.success && response.data) {
          // Store new access token
          this.tokenService.setAccessToken(
            response.data.accessToken,
            response.data.expiresIn
          );
          console.log('Token refreshed successfully');
        }
      }),
      catchError(error => {
        console.error('Token refresh failed:', error);
        // Refresh failed - logout user
        this.handleLogout();
        return throwError(() => error);
      })
    );
  }

  /**
   * Logout user
   */
  logout(): Observable<any> {
    return this.http.post<ApiResponse<void>>(
      `${this.API_URL}/api/auth/logout`,
      {},
      { withCredentials: true }
    ).pipe(
      tap(() => {
        this.handleLogout();
      }),
      catchError(error => {
        // Even if API fails, clear local state
        this.handleLogout();
        return of(null);
      })
    );
  }

  /**
   * Get current user info
   */
  getCurrentUser(): Observable<ApiResponse<User>> {
    return this.http.get<ApiResponse<User>>(
      `${this.API_URL}/api/auth/me`
    ).pipe(
      tap(response => {
        if (response.success && response.data) {
          this.currentUserSignal.set(response.data);
          this.isAuthenticatedSignal.set(true);
        }
      })
    );
  }

  /**
   * Load current user (private helper)
   */
  private loadCurrentUser(): Observable<ApiResponse<User>> {
    return this.getCurrentUser().pipe(
      catchError(() => {
        this.handleLogout();
        return of({ success: false } as ApiResponse<User>);
      })
    );
  }

  /**
   * Handle successful authentication
   */
  private handleAuthSuccess(authResponse: AuthResponse): void {
    // Store access token
    this.tokenService.setAccessToken(
      authResponse.accessToken,
      authResponse.expiresIn
    );

    // Set user and auth state
    this.currentUserSignal.set(authResponse.user);
    this.isAuthenticatedSignal.set(true);

    // Navigate to home or dashboard
    this.router.navigate(['/home']);
  }

  /**
   * Handle logout
   */
  private handleLogout(): void {
    this.tokenService.removeAccessToken();
    this.currentUserSignal.set(null);
    this.isAuthenticatedSignal.set(false);
    this.router.navigate(['/auth/login']);
  }

  /**
   * Handle OAuth2 callback
   */
  handleOAuthCallback(token: string): void {
    // Token received from OAuth2 redirect
    // We need to get user info and set up state
    this.tokenService.setAccessToken(token, 900); // 15 minutes default
    this.loadCurrentUser().subscribe();
  }
}