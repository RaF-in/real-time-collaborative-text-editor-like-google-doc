import { Component, inject, signal, OnInit, ChangeDetectionStrategy, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormArray, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatCheckboxModule } from '@angular/material/checkbox';
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
    MatInputModule, MatSelectModule, MatIconModule, MatProgressSpinnerModule,
    MatCheckboxModule
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
  readonly isSharing = signal(false);
  readonly shareableLink = signal<string>('');
  
  shareForm!: FormGroup;
  
  readonly permissionOptions = [
    { value: PermissionLevel.EDITOR, label: 'Can edit', icon: 'edit' },
    { value: PermissionLevel.VIEWER, label: 'Can view', icon: 'visibility' }
  ];
  
  ngOnInit(): void {
    this.initializeForm();
    this.loadData();
    this.generateShareableLink();
  }
  
  private initializeForm(): void {
    this.shareForm = this.fb.group({
      recipients: this.fb.array([this.createRecipientFormGroup()]),
      message: ['', Validators.maxLength(500)],
      notifyPeople: [true]
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
  }
  
  private generateShareableLink(): void {
    // Generate shareable link immediately when dialog opens
    this.sharingService.createShareableLink(this.data.documentId, {
      permissionLevel: PermissionLevel.EDITOR,
      expiresInDays: 1
    }).subscribe({
      next: (link) => {
        // Generate a user-accessible frontend URL
        const frontendUrl = `${window.location.origin}/link/${link.linkToken}`;
        this.shareableLink.set(frontendUrl);
      },
      error: () => {
        // If link creation fails, try to get existing links
        this.sharingService.getShareableLinks(this.data.documentId).subscribe({
          next: (links) => {
            if (links.length > 0) {
              // Generate a user-accessible frontend URL
              const frontendUrl = `${window.location.origin}/link/${links[0].linkToken}`;
              this.shareableLink.set(frontendUrl);
            }
          }
        });
      }
    });
  }
  
  share(): void {
    if (this.shareForm.invalid) return;
    
    const formValue = this.shareForm.value;
    const notifyPeople = formValue.notifyPeople;
    
    // Check if there are any valid recipients
    const hasValidRecipients = this.recipients.controls.some(
      control => control.get('email')?.value && control.valid
    );
    
    if (!hasValidRecipients) {
      this.showError('Please add at least one valid email address');
      return;
    }
    
    this.isSharing.set(true);
    
    const shareData = {
      recipients: formValue.recipients,
      message: formValue.message,
      shareableLink: this.shareableLink(),
      notifyPeople: notifyPeople
    };
    
    this.sharingService.shareWithMultiple(this.data.documentId, shareData).subscribe({
      next: (response) => {
        if (notifyPeople) {
          this.showSuccess(`Shared with ${response.successCount} of ${response.totalRecipients} people. Email notifications sent with link.`);
        } else {
          this.showSuccess(`Shared with ${response.successCount} of ${response.totalRecipients} people. No notifications sent.`);
        }
        
        if (response.failureCount > 0) {
          this.showError(`${response.failureCount} failed. Please check the email addresses.`);
        }
        
        this.isSharing.set(false);
        this.dialogRef.close();
      },
      error: (err) => {
        this.showError(err.error?.message || 'Failed to share document');
        this.isSharing.set(false);
      }
    });
  }
  
  copyLink(): void {
    const link = this.shareableLink();
    if (link) {
      navigator.clipboard.writeText(link);
      this.showSuccess('Link copied to clipboard');
    }
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
}