import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  DocumentAccessInfo,
  DocumentPermission,
  ShareWithMultipleRequest,
  ShareMultipleResponse,
  AccessRequest,
  RequestAccessRequest,
  ResolveAccessRequestRequest,
  ShareableLink,
  CreateShareableLinkRequest,
  NotificationData,
  PermissionLevel
} from '../models/sharing.model';

@Injectable({
  providedIn: 'root'
})
export class DocumentSharingService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/share`;
  
  // Signals for state management
  readonly accessInfo = signal<DocumentAccessInfo | null>(null);
  readonly permissions = signal<DocumentPermission[]>([]);
  readonly pendingRequests = signal<AccessRequest[]>([]);
  readonly shareableLinks = signal<ShareableLink[]>([]);
  readonly unreadNotifications = signal<number>(0);
  
  // ========================================
  // Access Information
  // ========================================
  
  getAccessInfo(documentId: string): Observable<DocumentAccessInfo> {
    return this.http.get<DocumentAccessInfo>(
      `${this.baseUrl}/documents/${documentId}/access-info`
    ).pipe(
      tap(info => this.accessInfo.set(info))
    );
  }
  
  // ========================================
  // Sharing with Multiple People
  // ========================================
  
  shareWithMultiple(
    documentId: string,
    request: ShareWithMultipleRequest
  ): Observable<ShareMultipleResponse> {
    return this.http.post<ShareMultipleResponse>(
      `${this.baseUrl}/documents/${documentId}/share-multiple`,
      request
    ).pipe(
      tap(() => this.refreshPermissions(documentId))
    );
  }
  
  // ========================================
  // Permission Management
  // ========================================
  
  getPermissions(documentId: string): Observable<DocumentPermission[]> {
    return this.http.get<DocumentPermission[]>(
      `${this.baseUrl}/documents/${documentId}/permissions`
    ).pipe(
      tap(perms => this.permissions.set(perms))
    );
  }
  
  updatePermission(
    documentId: string,
    userId: string,
    permissionLevel: PermissionLevel
  ): Observable<DocumentPermission> {
    return this.http.put<DocumentPermission>(
      `${this.baseUrl}/documents/${documentId}/permissions`,
      { userId, permissionLevel }
    ).pipe(
      tap(() => this.refreshPermissions(documentId))
    );
  }
  
  removePermission(documentId: string, userId: string): Observable<void> {
    return this.http.delete<void>(
      `${this.baseUrl}/documents/${documentId}/permissions/${userId}`
    ).pipe(
      tap(() => this.refreshPermissions(documentId))
    );
  }
  
  // ========================================
  // Access Requests
  // ========================================
  
  requestAccess(
    documentId: string,
    request: RequestAccessRequest
  ): Observable<AccessRequest> {
    return this.http.post<AccessRequest>(
      `${this.baseUrl}/documents/${documentId}/request-access`,
      request
    );
  }
  
  getPendingAccessRequests(documentId: string): Observable<AccessRequest[]> {
    return this.http.get<AccessRequest[]>(
      `${this.baseUrl}/documents/${documentId}/access-requests`
    ).pipe(
      tap(requests => this.pendingRequests.set(requests))
    );
  }
  
  getMyAccessRequests(): Observable<AccessRequest[]> {
    return this.http.get<AccessRequest[]>(
      `${this.baseUrl}/access-requests/my-requests`
    );
  }
  
  resolveAccessRequest(
    requestId: string,
    request: ResolveAccessRequestRequest
  ): Observable<void> {
    return this.http.post<void>(
      `${this.baseUrl}/access-requests/${requestId}/resolve`,
      request
    ).pipe(
      tap(() => {
        // Refresh both requests and permissions if approved
        const docId = this.accessInfo()?.documentId;
        if (docId) {
          this.refreshPendingRequests(docId);
          if (request.approve) {
            this.refreshPermissions(docId);
          }
        }
      })
    );
  }
  
  // ========================================
  // Shareable Links
  // ========================================
  
  createShareableLink(
    documentId: string,
    request: CreateShareableLinkRequest
  ): Observable<ShareableLink> {
    return this.http.post<ShareableLink>(
      `${this.baseUrl}/documents/${documentId}/links`,
      request
    ).pipe(
      tap(() => this.refreshShareableLinks(documentId))
    );
  }
  
  getShareableLinks(documentId: string): Observable<ShareableLink[]> {
    return this.http.get<ShareableLink[]>(
      `${this.baseUrl}/documents/${documentId}/links`
    ).pipe(
      tap(links => this.shareableLinks.set(links))
    );
  }
  
  revokeShareableLink(linkId: string): Observable<void> {
    return this.http.delete<void>(
      `${this.baseUrl}/links/${linkId}`
    ).pipe(
      tap(() => {
        const docId = this.accessInfo()?.documentId;
        if (docId) {
          this.refreshShareableLinks(docId);
        }
      })
    );
  }
  
  accessViaShareableLink(token: string): Observable<DocumentAccessInfo> {
    return this.http.get<DocumentAccessInfo>(
      `${this.baseUrl}/links/${token}/access`
    );
  }
  
  // ========================================
  // Notifications
  // ========================================
  
  getNotifications(page: number = 0, size: number = 20): Observable<NotificationData[]> {
    return this.http.get<NotificationData[]>(
      `${this.baseUrl}/notifications`,
      { params: { page: page.toString(), size: size.toString() } }
    );
  }
  
  getUnreadCount(): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(
      `${this.baseUrl}/notifications/unread-count`
    ).pipe(
      tap(response => this.unreadNotifications.set(response.count))
    );
  }
  
  markNotificationAsRead(notificationId: string): Observable<void> {
    return this.http.put<void>(
      `${this.baseUrl}/notifications/${notificationId}/read`,
      {}
    ).pipe(
      tap(() => this.getUnreadCount().subscribe())
    );
  }
  
  markAllNotificationsAsRead(): Observable<{ markedCount: number }> {
    return this.http.put<{ markedCount: number }>(
      `${this.baseUrl}/notifications/read-all`,
      {}
    ).pipe(
      tap(() => this.unreadNotifications.set(0))
    );
  }
  
  // ========================================
  // Helper Methods
  // ========================================
  
  private refreshPermissions(documentId: string): void {
    this.getPermissions(documentId).subscribe();
  }
  
  private refreshPendingRequests(documentId: string): void {
    this.getPendingAccessRequests(documentId).subscribe();
  }
  
  private refreshShareableLinks(documentId: string): void {
    this.getShareableLinks(documentId).subscribe();
  }
  
  clearState(): void {
    this.accessInfo.set(null);
    this.permissions.set([]);
    this.pendingRequests.set([]);
    this.shareableLinks.set([]);
  }
  
  // Permission helper methods
  canEdit(accessInfo: DocumentAccessInfo | null): boolean {
    return accessInfo?.canEdit ?? false;
  }
  
  canShare(accessInfo: DocumentAccessInfo | null): boolean {
    return accessInfo?.canShare ?? false;
  }
  
  canManagePermissions(accessInfo: DocumentAccessInfo | null): boolean {
    return accessInfo?.canManagePermissions ?? false;
  }
  
  getPermissionLabel(level: string): string {
    const labels: Record<string, string> = {
      'OWNER': 'Owner',
      'EDITOR': 'Can edit',
      'VIEWER': 'Can view'
    };
    return labels[level] || level;
  }
  
  getPermissionIcon(level: string): string {
    const icons: Record<string, string> = {
      'OWNER': 'shield',
      'EDITOR': 'edit',
      'VIEWER': 'visibility'
    };
    return icons[level] || 'person';
  }
}