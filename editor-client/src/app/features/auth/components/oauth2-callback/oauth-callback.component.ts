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
    console.log('OAuth Callback: Component initialized');

    // Extract token from query params (sent by backend)
    this.route.queryParams.subscribe(params => {
      console.log('OAuth Callback: Query params received:', params);

      const token = params['token'];
      const error = params['error'];

      // Check for our custom stored return URL first
      const customReturnUrl = sessionStorage.getItem('oauthReturnUrl');
      const timestamp = sessionStorage.getItem('oauthReturnTimestamp');

      // Clear the custom return URL after retrieving (use only once)
      sessionStorage.removeItem('oauthReturnUrl');
      sessionStorage.removeItem('oauthReturnTimestamp');

      console.log('OAuth Callback: token:', token ? 'present' : 'missing');
      console.log('OAuth Callback: error:', error);
      console.log('OAuth Callback: customReturnUrl:', customReturnUrl);

      if (error) {
        console.error('OAuth error:', error);
        this.router.navigate(['/auth/login'], {
          queryParams: { error: 'OAuth authentication failed' }
        });
        return;
      }

      if (token) {
        // Use our custom return URL if available
        if (customReturnUrl && timestamp) {
          // Check if the timestamp is recent (within 5 minutes)
          const elapsed = Date.now() - parseInt(timestamp);
          if (elapsed < 5 * 60 * 1000) { // 5 minutes
            console.log('OAuth Callback: Using custom return URL:', customReturnUrl);
            this.authService.storeReturnUrl(customReturnUrl);
          }
        }

        // Handle OAuth callback with token
        console.log('OAuth Callback: Handling OAuth callback');
        this.authService.handleOAuthCallback(token);
      } else {
        console.error('No token received from OAuth');
        this.router.navigate(['/auth/login']);
      }
    });
  }
}