// src/app/core/models/user.model.ts
export interface User {
  id: string;
  username: string;
  email: string;
  provider: 'LOCAL' | 'GOOGLE' | 'FACEBOOK' | 'GITHUB';
  enabled: boolean;
  emailVerified: boolean;
  roles: string[];
  permissions: string[];
  createdAt: string;
  lastLoginAt?: string;
}