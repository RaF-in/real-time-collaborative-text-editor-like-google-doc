import { Component, inject, signal, OnInit, ChangeDetectionStrategy, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormArray, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTabsModule } from '@angular/material/tabs';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { DocumentSharingService } from '../../../../core/services/document-sharing.service';
import { PermissionLevel } from '../../../../core/models/sharing.model';

export interface ShareDialogData {
  documentId: string;
  documentTitle: string;
}

@Component({
  selector: 'app-share-dialog',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatDialogModule, MatButtonModule,
    MatInputModule, MatSelectModule, MatTabsModule, MatIconModule, MatProgressSpinnerModule
  ],
  templateUrl: './share-dialog.component.html',
  styleUrl: './share-dialog.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ShareDialogComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<ShareDialogComponent>);
  readonly data: ShareDialogData = inject(MAT_DIALOG_DATA);
  readonly sharingService = inject(DocumentSharingService);
  private readonly snackBar = inject(MatSnackBar);
  
  readonly PermissionLevel = PermissionLevel;
  readonly isLoading = signal(false);
  readonly isSharingMultiple = signal(false);
  readonly isCreatingLink = signal(false);
  
  shareForm!: FormGroup;
  linkForm!: FormGroup;
  
  readonly permissionOptions = [
    { value: PermissionLevel.EDITOR, label: 'Can edit', icon: 'edit' },
    { value: PermissionLevel.VIEWER, label: 'Can view', icon: 'visibility' }
  ];
  
  ngOnInit(): void {
    this.initializeForms();
    this.loadData();
  }
  
  private initializeForms(): void {
    this.shareForm = this.fb.group({
      recipients: this.fb.array([this.createRecipientFormGroup()]),
      message: ['', Validators.maxLength(500)]
    });
    
    this.linkForm = this.fb.group({
      permissionLevel: [PermissionLevel.VIEWER, Validators.required],
      expiresInDays: [null, Validators.min(1)]
    });
  }
  
  private createRecipientFormGroup(): FormGroup {
    return this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      permissionLevel: [PermissionLevel.EDITOR, Validators.required]
    });
  }
  
  get recipients(): FormArray {
    return this.shareForm.get('recipients') as FormArray;
  }
  
  addRecipient(): void {
    if (this.recipients.length < 50) {
      this.recipients.push(this.createRecipientFormGroup());
    }
  }
  
  removeRecipient(index: number): void {
    if (this.recipients.length > 1) {
      this.recipients.removeAt(index);
    }
  }
  
  private loadData(): void {
    this.isLoading.set(true);
    this.sharingService.getPermissions(this.data.documentId).subscribe({
      next: () => this.isLoading.set(false),
      error: () => {
        this.showError('Failed to load permissions');
        this.isLoading.set(false);
      }
    });
    
    this.sharingService.getShareableLinks(this.data.documentId).subscribe();
  }
  
  shareWithMultiple(): void {
    if (this.shareForm.invalid) return;
    
    this.isSharingMultiple.set(true);
    
    this.sharingService.shareWithMultiple(this.data.documentId, this.shareForm.value).subscribe({
      next: (response) => {
        this.showSuccess(`Shared with ${response.successCount} of ${response.totalRecipients} people. Email invitations sent.`);
        if (response.failureCount > 0) {
          this.showError(`${response.failureCount} failed. Please check the email addresses.`);
        }
        this.shareForm.reset();
        this.recipients.clear();
        this.recipients.push(this.createRecipientFormGroup());
        this.isSharingMultiple.set(false);
      },
      error: (err) => {
        this.showError(err.error?.message || 'Failed to share document');
        this.isSharingMultiple.set(false);
      }
    });
  }
  
  createShareableLink(): void {
    if (this.linkForm.invalid) return;
    
    this.isCreatingLink.set(true);
    this.sharingService.createShareableLink(this.data.documentId, this.linkForm.value).subscribe({
      next: () => {
        this.showSuccess('Shareable link created successfully');
        this.linkForm.reset({ permissionLevel: PermissionLevel.VIEWER });
        this.isCreatingLink.set(false);
      },
      error: () => {
        this.showError('Failed to create link');
        this.isCreatingLink.set(false);
      }
    });
  }
  
  copyLink(url: string): void {
    navigator.clipboard.writeText(url);
    this.showSuccess('Link copied to clipboard');
  }
  
  revokeLink(linkId: string): void {
    if (!confirm('Revoke this link? Anyone with the link will lose access.')) return;
    
    this.sharingService.revokeShareableLink(linkId).subscribe({
      next: () => this.showSuccess('Link revoked successfully'),
      error: () => this.showError('Failed to revoke link')
    });
  }
  
  removePermission(userId: string, email: string): void {
    if (!confirm(`Remove ${email}'s access?`)) return;
    
    this.sharingService.removePermission(this.data.documentId, userId).subscribe({
      next: () => this.showSuccess('Access removed. User will be notified via email.'),
      error: () => this.showError('Failed to remove access')
    });
  }
  
  changePermission(userId: string, newLevel: PermissionLevel): void {
    this.sharingService.updatePermission(this.data.documentId, userId, newLevel).subscribe({
      next: () => this.showSuccess('Permission updated. User will be notified via email.'),
      error: () => this.showError('Failed to update permission')
    });
  }
  
  private showSuccess(message: string): void {
    this.snackBar.open(message, 'Close', { duration: 4000 });
  }
  
  private showError(message: string): void {
    this.snackBar.open(message, 'Close', { duration: 5000, panelClass: 'error-snackbar' });
  }
  
  close(): void {
    this.dialogRef.close();
  }
}