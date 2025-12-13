import { Component, ChangeDetectionStrategy, signal, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { SignupRequest } from '../../../../core/models/auth.model';
import { CustomValidators } from '../../../../core/utils/validators.util';
import { environment } from '../../../../../environments/environment';

@Component({
  selector: 'app-signup',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './signup.component.html',
  styleUrls: ['./signup.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SignupComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  // Signals
  isLoading = this.authService.isLoading;
  errorMessage = signal<string | null>(null);
  showPassword = signal<boolean>(false);

  signupForm: FormGroup;
  googleSignupUrl = `${environment.apiUrl}/oauth2/authorization/google`;

  // Computed signal for password strength
  passwordStrength = computed(() => {
    const passwordControl = this.signupForm?.get('password');
    if (!passwordControl?.value) return null;

    const errors = passwordControl.errors;
    if (!errors || !errors['passwordStrength']) return 'strong';

    const strength = errors['passwordStrength'];
    const checks = [
      strength.hasUpperCase,
      strength.hasLowerCase,
      strength.hasNumeric,
      strength.hasSpecialChar,
      strength.isLengthValid
    ];
    const passedChecks = checks.filter(Boolean).length;

    if (passedChecks <= 2) return 'weak';
    if (passedChecks <= 3) return 'medium';
    return 'strong';
  });

  constructor() {
    this.signupForm = this.fb.group({
      username: ['', [
        Validators.required,
        Validators.minLength(3),
        Validators.maxLength(50),
        CustomValidators.username()
      ]],
      email: ['', [
        Validators.required,
        Validators.email
      ]],
      password: ['', [
        Validators.required,
        Validators.minLength(8),
        CustomValidators.passwordStrength()
      ]]
    });
  }

  onSubmit(): void {
    if (this.signupForm.valid) {
      this.errorMessage.set(null);
      const request: SignupRequest = this.signupForm.value;

      this.authService.signup(request).subscribe({
        next: (response) => {
          if (response.success) {
            console.log('Signup successful');
            // AuthService handles navigation
          }
        },
        error: (error) => {
          this.errorMessage.set(error.message || 'Signup failed. Please try again.');
        }
      });
    } else {
      this.signupForm.markAllAsTouched();
    }
  }

  togglePasswordVisibility(): void {
    this.showPassword.update(value => !value);
  }

  getFieldError(fieldName: string): string | null {
    const field = this.signupForm.get(fieldName);
    if (field?.invalid && (field.dirty || field.touched)) {
      if (field.errors?.['required']) {
        return `${this.getFieldLabel(fieldName)} is required`;
      }
      if (field.errors?.['email']) {
        return 'Please enter a valid email address';
      }
      if (field.errors?.['minlength']) {
        const minLength = field.errors['minlength'].requiredLength;
        return `${this.getFieldLabel(fieldName)} must be at least ${minLength} characters`;
      }
      if (field.errors?.['username']) {
        return 'Username can only contain letters, numbers, underscore and hyphen';
      }
      if (field.errors?.['passwordStrength']) {
        return 'Password must contain uppercase, lowercase, number, and special character';
      }
    }
    return null;
  }

  private getFieldLabel(fieldName: string): string {
    const labels: Record<string, string> = {
      username: 'Username',
      email: 'Email',
      password: 'Password'
    };
    return labels[fieldName] || fieldName;
  }

  getPasswordStrengthText(): string {
    const strength = this.passwordStrength();
    if (!strength) return '';
    return strength.charAt(0).toUpperCase() + strength.slice(1);
  }

  signupWithGoogle(): void {
    window.location.href = this.googleSignupUrl;
  }
}