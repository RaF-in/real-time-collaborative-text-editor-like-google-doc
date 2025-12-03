// src/app/core/models/log-entry.model.ts
export interface LogEntry {
  time: Date;
  type: 'info' | 'success' | 'error' | 'warning';
  message: string;
}