import { Component, inject, OnInit, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { DocumentSharingService } from '../../../../core/services/document-sharing.service';

@Component({
  selector: 'app-shared-link-access',
  standalone: true,
  imports: [CommonModule, MatProgressSpinnerModule],
  template: `
    <div class="link-access-container">
      @if (isLoading()) {
        <div class="loading">
          <mat-spinner diameter="60"></mat-spinner>
          <p>Accessing document via link...</p>
        </div>
      } @else if (error()) {
        <div class="error">
          <h2>Invalid or Expired Link</h2>
          <p>{{ error() }}</p>
        </div>
      }
    </div>
  `,
  styles: [`
    .link-access-container {
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
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SharedLinkAccessComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly sharingService = inject(DocumentSharingService);
  
  readonly isLoading = signal(true);
  readonly error = signal<string | null>(null);
  
  ngOnInit(): void {
    const token = this.route.snapshot.paramMap.get('token');
    if (!token) {
      this.error.set('Invalid link');
      this.isLoading.set(false);
      return;
    }
    
    // This will auto-grant permission and return document info
    this.sharingService.accessViaShareableLink(token).subscribe({
      next: (info) => {
        // Redirect directly to the document
        this.router.navigate(['/editor', info.documentId]);
      },
      error: (err) => {
        this.error.set(err.error?.message || 'This link is invalid or has expired');
        this.isLoading.set(false);
      }
    });
  }
}