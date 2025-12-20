import { Component, inject, signal, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { DocumentSharingService } from '../../../../core/services/document-sharing.service';
import { PermissionLevel } from '../../../../core/models/sharing.model';

@Component({
  selector: 'app-request-access',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatCardModule, MatButtonModule,
    MatInputModule, MatSelectModule, MatIconModule, MatProgressSpinnerModule
  ],
  template: `
    <div class="request-access-container">
      <mat-card class="request-card">
        @if (isLoading()) {
          <mat-card-content>
            <div class="loading">
              <mat-spinner diameter="50"></mat-spinner>
              <p>Loading document information...</p>
            </div>
          </mat-card-content>
        } @else if (requestSent()) {
          <mat-card-content>
            <div class="success">
              <mat-icon class="success-icon">mark_email_read</mat-icon>
              <h2>Request Sent!</h2>
              <p>Your access request has been sent to the document owner via email.</p>
              <p class="info">You'll receive an email notification when your request is reviewed.</p>
              <button mat-raised-button color="primary" (click)="goToDashboard()">
                Go to My Documents
              </button>
            </div>
          </mat-card-content>
        } @else if (accessInfo()) {
          <mat-card-header>
            <mat-icon mat-card-avatar>lock</mat-icon>
            <mat-card-title>Request Access</mat-card-title>
            <mat-card-subtitle>{{ accessInfo()?.title }}</mat-card-subtitle>
          </mat-card-header>
          
          <mat-card-content>
            <p class="info-text">
              You don't have permission to view this document. 
              Request access from <strong>{{ accessInfo()?.ownerName }}</strong>.
              They will receive an email with your request.
            </p>
            
            <form [formGroup]="requestForm" (ngSubmit)="submitRequest()">
              <mat-form-field class="full-width">
                <mat-label>Request permission level</mat-label>
                <mat-select formControlName="requestedPermission">
                  <mat-option [value]="PermissionLevel.EDITOR">
                    <mat-icon>edit</mat-icon>
                    Can edit
                  </mat-option>
                  <mat-option [value]="PermissionLevel.VIEWER">
                    <mat-icon>visibility</mat-icon>
                    Can view
                  </mat-option>
                </mat-select>
              </mat-form-field>
              
              <mat-form-field class="full-width">
                <mat-label>Message (optional)</mat-label>
                <textarea matInput formControlName="message" rows="4"
                          placeholder="Explain why you need access..."></textarea>
                <mat-hint>This message will be included in the email to the owner</mat-hint>
              </mat-form-field>
              
              <div class="actions">
                <button mat-button type="button" (click)="cancel()">Cancel</button>
                <button mat-raised-button color="primary" type="submit"
                        [disabled]="requestForm.invalid || isSubmitting()">
                  @if (isSubmitting()) {
                    <mat-spinner diameter="20"></mat-spinner>
                  } @else {
                    Send Request
                  }
                </button>
              </div>
            </form>
          </mat-card-content>
        }
      </mat-card>
    </div>
  `,
  styles: [`
    .request-access-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 80vh;
      padding: 24px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    }
    
    .request-card {
      width: 100%;
      max-width: 600px;
    }
    
    .loading, .success {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 16px;
      padding: 40px;
      text-align: center;
    }
    
    .success-icon {
      font-size: 64px;
      width: 64px;
      height: 64px;
      color: #4caf50;
    }
    
    .info-text {
      color: #666;
      margin-bottom: 24px;
    }
    
    .info {
      font-size: 14px;
      color: #666;
    }
    
    .full-width {
      width: 100%;
      margin-bottom: 16px;
    }
    
    .actions {
      display: flex;
      justify-content: flex-end;
      gap: 12px;
      margin-top: 24px;
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RequestAccessComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly sharingService = inject(DocumentSharingService);
  private readonly fb = inject(FormBuilder);
  
  readonly PermissionLevel = PermissionLevel;
  readonly isLoading = signal(true);
  readonly isSubmitting = signal(false);
  readonly requestSent = signal(false);
  readonly accessInfo = this.sharingService.accessInfo;
  
  requestForm!: FormGroup;
  
  ngOnInit(): void {
    this.initializeForm();
    this.loadAccessInfo();
  }
  
  private initializeForm(): void {
    this.requestForm = this.fb.group({
      requestedPermission: [PermissionLevel.VIEWER, Validators.required],
      message: ['', Validators.maxLength(500)]
    });
  }
  
  private loadAccessInfo(): void {
    const documentId = this.route.snapshot.paramMap.get('id');
    if (!documentId) {
      this.router.navigate(['/home']);
      return;
    }
    
    this.sharingService.getAccessInfo(documentId).subscribe({
      next: (info) => {
        this.isLoading.set(false);
        if (info.userPermission) {
          this.router.navigate(['/editor', documentId]);
        }
        if (!info.canRequestAccess) {
          this.router.navigate(['/no-access'], { queryParams: { documentId } });
        }
      },
      error: () => this.isLoading.set(false)
    });
  }
  
  submitRequest(): void {
    if (this.requestForm.invalid) return;
    
    const documentId = this.route.snapshot.paramMap.get('id');
    if (!documentId) return;
    
    this.isSubmitting.set(true);
    this.sharingService.requestAccess(documentId, this.requestForm.value).subscribe({
      next: () => {
        this.requestSent.set(true);
        this.isSubmitting.set(false);
      },
      error: (err) => {
        alert(err.error?.message || 'Failed to send request');
        this.isSubmitting.set(false);
      }
    });
  }
  
  cancel(): void {
    this.router.navigate(['/home']);
  }
  
  goToDashboard(): void {
    this.router.navigate(['/home']);
  }
}