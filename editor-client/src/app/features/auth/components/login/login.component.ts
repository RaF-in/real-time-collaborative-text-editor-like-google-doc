import { Component, ChangeDetectionStrategy, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
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
  }

  onSubmit(): void {
    if (this.loginForm.valid) {
      this.errorMessage.set(null);
      const request: LoginRequest = this.loginForm.value;

      this.authService.login(request).subscribe({
        next: (response) => {
          if (response.success) {
            // AuthService handles navigation
            console.log('Login successful');
          }
        },
        error: (error) => {
          this.errorMessage.set(error.message || 'Login failed. Please try again.');
        }
      });
    } else {
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
    // Redirect to backend OAuth2 endpoint
    window.location.href = this.googleLoginUrl;
  }
}
