// src/app/core/models/document-snapshot.model.ts
export interface DocumentSnapshot {
  id: number;
  docId: string;
  fractionalPosition: string;
  character: string;
  serverId: string;
  serverSeqNum: number;
  createdAt: string;
  active: boolean;
}
