import { Component, inject, signal, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-no-access',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatButtonModule, MatIconModule],
  template: `
    <div class="no-access-container">
      <mat-card class="no-access-card">
        <mat-card-content>
          <div class="content">
            <mat-icon class="lock-icon">lock</mat-icon>
            <h2>Access Denied</h2>
            <p class="message">You don't have permission to access this document.</p>
            <p class="sub-message">
              If you believe you should have access, please contact the document owner.
            </p>
            <div class="actions">
              <button mat-raised-button color="primary" (click)="goBack()">
                <mat-icon>arrow_back</mat-icon>
                Go Back
              </button>
            </div>
          </div>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .no-access-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 80vh;
      padding: 24px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    }
    
    .no-access-card {
      width: 100%;
      max-width: 500px;
      text-align: center;
    }
    
    .content {
      padding: 40px 20px;
      
      .lock-icon {
        font-size: 80px;
        width: 80px;
        height: 80px;
        color: #f44336;
        margin-bottom: 24px;
      }
      
      h2 {
        font-size: 32px;
        margin-bottom: 16px;
        color: #333;
      }
      
      .message {
        font-size: 16px;
        color: #666;
        margin-bottom: 12px;
      }
      
      .sub-message {
        font-size: 14px;
        color: #999;
        margin-bottom: 32px;
      }
      
      .actions {
        display: flex;
        justify-content: center;
        gap: 16px;
      }
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class NoAccessComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  
  readonly documentId = signal<string | null>(null);
  
  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      if (params['documentId']) {
        this.documentId.set(params['documentId']);
      }
    });
  }
  
  goBack(): void {
    this.router.navigate(['/documents']);
  }
}