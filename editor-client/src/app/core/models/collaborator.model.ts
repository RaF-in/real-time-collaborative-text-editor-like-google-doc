// src/app/core/models/collaborator.model.ts
export interface Collaborator {
  userId: string;
  userName: string;
  avatarColor: string;
  cursorPosition?: number;
  isTyping?: boolean;
  lastSeen?: Date;
}
