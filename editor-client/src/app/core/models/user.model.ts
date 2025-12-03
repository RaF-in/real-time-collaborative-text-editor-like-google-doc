// src/app/core/models/user.model.ts
export interface User {
  id: string;
  name: string;
  email: string;
  avatarColor?: string;
  isOnline?: boolean;
}