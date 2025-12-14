import { Component, ChangeDetectionStrategy, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { LoginRequest } from '../../../../core/models/auth.model';
import { environment } from '../../../../../environments/environment';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  // Signals for reactive state
  isLoading = this.authService.isLoading;
  errorMessage = signal<string | null>(null);
  showPassword = signal<boolean>(false);

  loginForm: FormGroup;
  googleLoginUrl = `${environment.apiUrl}/oauth2/authorization/google`;

  constructor() {
    this.loginForm = this.fb.group({
      usernameOrEmail: ['', [Validators.required]],
      password: ['', [Validators.required]]
    });

    // Store return URL from query params
    this.route.queryParams.subscribe(params => {
      console.log('Login: Query params:', params);
      if (params['returnUrl']) {
        console.log('Login: Found return URL in params:', params['returnUrl']);
        this.authService.storeReturnUrl(params['returnUrl']);
      } else {
        console.log('Login: No return URL found in params');
      }
    });
  }

  onSubmit(): void {
    if (this.loginForm.valid) {
      this.errorMessage.set(null);
      const request: LoginRequest = this.loginForm.value;

      console.log('Login: Submitting login form with:', request);
      console.log('Login: Return URL in sessionStorage before login:', sessionStorage.getItem('returnUrl'));

      this.authService.login(request).subscribe({
        next: (response) => {
          console.log('Login: Login response:', response);
          if (response.success) {
            // AuthService handles navigation
            console.log('Login successful');
          }
        },
        error: (error) => {
          console.error('Login: Login error:', error);
          this.errorMessage.set(error.message || 'Login failed. Please try again.');
        }
      });
    } else {
      console.log('Login: Form is invalid');
      this.loginForm.markAllAsTouched();
    }
  }

  togglePasswordVisibility(): void {
    this.showPassword.update(value => !value);
  }

  getFieldError(fieldName: string): string | null {
    const field = this.loginForm.get(fieldName);
    if (field?.invalid && (field.dirty || field.touched)) {
      if (field.errors?.['required']) {
        return `${this.getFieldLabel(fieldName)} is required`;
      }
    }
    return null;
  }

  private getFieldLabel(fieldName: string): string {
    const labels: Record<string, string> = {
      usernameOrEmail: 'Username or Email',
      password: 'Password'
    };
    return labels[fieldName] || fieldName;
  }

  loginWithGoogle(): void {
    // Get current return URL from sessionStorage or query params
    const returnUrl = sessionStorage.getItem('returnUrl') ||
                      this.route.snapshot.queryParams['returnUrl'] ||
                      '/home';

    // Build OAuth URL with state parameter containing return URL
    const oauthUrl = new URL(this.googleLoginUrl);
    oauthUrl.searchParams.set('state', encodeURIComponent(returnUrl));

    // Redirect to backend OAuth2 endpoint
    window.location.href = oauthUrl.toString();
  }
}
