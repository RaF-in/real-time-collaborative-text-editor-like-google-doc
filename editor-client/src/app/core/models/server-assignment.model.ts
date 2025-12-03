// src/app/core/models/server-assignment.model.ts
export interface ServerAssignment {
  key: string;
  serverId: string;
  serverAddress: string;
  wsUrl: string;
  source: string;
}
