import { Component, inject, OnInit, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { DocumentSharingService } from '../../../../core/services/document-sharing.service';

@Component({
  selector: 'app-share-link-redirect',
  standalone: true,
  imports: [CommonModule, MatProgressSpinnerModule, MatButtonModule],
  template: `
    <div class="redirect-container">
      @if (isLoading()) {
        <div class="loading">
          <mat-spinner diameter="60"></mat-spinner>
          <p>Granting access to document...</p>
        </div>
      } @else if (error()) {
        <div class="error">
          <h2>Access Denied</h2>
          <p>{{ error() }}</p>
          <button mat-raised-button (click)="navigateToHome()">Go Home</button>
        </div>
      }
    </div>
  `,
  styles: [`
    .redirect-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 100vh;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    }

    .loading, .error {
      text-align: center;
      color: white;
      padding: 40px;
    }

    .loading p {
      margin-top: 20px;
      font-size: 16px;
    }

    .error h2 {
      font-size: 24px;
      margin-bottom: 16px;
    }

    .error button {
      margin-top: 20px;
      background: white;
      color: #667eea;
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ShareLinkRedirectComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly sharingService = inject(DocumentSharingService);

  readonly isLoading = signal(true);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    const token = this.route.snapshot.paramMap.get('token');
    console.log('ShareLinkRedirect: Token:', token);

    if (!token) {
      this.error.set('Invalid share link');
      this.isLoading.set(false);
      return;
    }

    // Grant access via share link
    this.sharingService.accessViaShareableLink(token).subscribe({
      next: (info) => {
        console.log('ShareLinkRedirect: Successfully granted access:', info);
        // Redirect to the document
        this.router.navigate(['/editor', info.documentId]);
      },
      error: (err) => {
        console.error('ShareLinkRedirect: Error accessing link:', err);
        this.error.set(err.error?.message || 'This link is invalid or has expired');
        this.isLoading.set(false);
      }
    });
  }

  navigateToHome(): void {
    this.router.navigate(['/home']);
  }
}