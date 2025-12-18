export enum PermissionLevel {
  OWNER = 'OWNER',
  EDITOR = 'EDITOR',
  VIEWER = 'VIEWER'
}

export interface ShareRecipient {
  email: string;
  permissionLevel: PermissionLevel;
}

export interface ShareWithMultipleRequest {
  recipients: ShareRecipient[];
  notifyPeople: boolean;
  message?: string;
}

export interface ShareResult {
  email: string;
  success: boolean;
  message: string;
  permission?: DocumentPermission;
}

export interface ShareMultipleResponse {
  totalRecipients: number;
  successCount: number;
  failureCount: number;
  results: ShareResult[];
}

export interface DocumentPermission {
  id: string;
  userId: string;
  userEmail: string;
  userName: string;
  userAvatarUrl?: string;
  permissionLevel: PermissionLevel;
  grantedAt: string;
  canRemove: boolean;
  canChangePermission: boolean;
}

export interface AccessRequest {
  id: string;
  documentId: string;
  documentTitle?: string;
  requesterId: string;
  requesterEmail: string;
  requesterName: string;
  requesterAvatarUrl?: string;
  requestedPermission: PermissionLevel;
  message?: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';
  requestedAt: string;
  resolvedAt?: string;
  resolvedBy?: string;
}

export interface RequestAccessRequest {
  requestedPermission: PermissionLevel;
  message?: string;
}

export interface ResolveAccessRequestRequest {
  approve: boolean;
  permissionLevel?: PermissionLevel;
  message?: string;
}

export interface ShareableLink {
  id: string;
  linkToken: string;
  fullUrl: string;
  permissionLevel: PermissionLevel;
  createdAt: string;
  expiresAt?: string;
  isActive: boolean;
  accessCount: number;
  lastAccessedAt?: string;
}

export interface CreateShareableLinkRequest {
  permissionLevel: PermissionLevel;
  expiresInDays?: number;
}

export interface DocumentAccessInfo {
  documentId: string;
  title: string;
  userPermission?: PermissionLevel;
  canEdit: boolean;
  canShare: boolean;
  canManagePermissions: boolean;
  canRequestAccess: boolean;
  ownerId: string;
  ownerEmail: string;
  ownerName: string;
}

export interface NotificationData {
  id: string;
  type: string;
  title: string;
  message: string;
  data: Record<string, any>;
  isRead: boolean;
  createdAt: string;
  readAt?: string;
}