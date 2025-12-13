import { Component, ChangeDetectionStrategy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { UserService } from '../../core/services/user.service';
import { ToastService } from '../../shared/services/toast.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DashboardComponent {
  private authService = inject(AuthService);
  private userService = inject(UserService);
  private router = inject(Router);
  private toastService = inject(ToastService);

  // Signals from AuthService
  currentUser = this.authService.currentUser;
  userDisplayName = this.authService.userDisplayName;
  isAdmin = this.authService.isAdmin;
  isAuthenticated = this.authService.isAuthenticated;

  // Local signals
  showUserMenu = signal<boolean>(false);
  isLoggingOut = signal<boolean>(false);
  showSidebar = signal<boolean>(false);

  toggleUserMenu(): void {
    this.showUserMenu.update(value => !value);
  }

  toggleSidebar(): void {
    this.showSidebar.update(value => !value);
  }

  logout(): void {
    if (confirm('Are you sure you want to logout?')) {
      this.isLoggingOut.set(true);
      
      this.authService.logout().subscribe({
        next: () => {
          this.toastService.success('Logged out successfully');
          this.isLoggingOut.set(false);
          // AuthService handles navigation to login
        },
        error: (error) => {
          this.toastService.error('Logout failed: ' + error.message);
          this.isLoggingOut.set(false);
        }
      });
    }
  }

  navigateToProfile(): void {
    const user = this.currentUser();
    if (user) {
      this.router.navigate(['/profile', user.id]);
    }
    this.showUserMenu.set(false);
  }

  navigateToSettings(): void {
    this.router.navigate(['/settings']);
    this.showUserMenu.set(false);
  }

  getInitials(): string {
    const user = this.currentUser();
    if (!user) return '?';
    
    if (user.username) {
      return user.username.charAt(0).toUpperCase();
    }
    if (user.email) {
      return user.email.charAt(0).toUpperCase();
    }
    return '?';
  }

  getRoleBadgeClass(role: string): string {
    switch (role) {
      case 'ADMIN':
        return 'badge-admin';
      case 'MODERATOR':
        return 'badge-moderator';
      default:
        return 'badge-user';
    }
  }
}