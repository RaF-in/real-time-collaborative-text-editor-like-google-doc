import { Component, OnInit, inject, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-oauth-callback',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './oauth-callback.component.html',
  styleUrls: ['./oauth-callback.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OAuthCallbackComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private authService = inject(AuthService);

  ngOnInit(): void {
    // Extract token from query params (sent by backend)
    this.route.queryParams.subscribe(params => {
      const token = params['token'];
      const error = params['error'];
      const returnUrl = params['state']; // OAuth2 state parameter can contain return URL

      if (error) {
        console.error('OAuth error:', error);
        this.router.navigate(['/auth/login'], {
          queryParams: { error: 'OAuth authentication failed' }
        });
        return;
      }

      if (token) {
        // Store return URL if present in OAuth state
        if (returnUrl) {
          this.authService.storeReturnUrl(decodeURIComponent(returnUrl));
        }
        // Handle OAuth callback with token
        this.authService.handleOAuthCallback(token);
        // AuthService will handle navigation based on stored return URL
      } else {
        console.error('No token received from OAuth');
        this.router.navigate(['/auth/login']);
      }
    });
  }
}