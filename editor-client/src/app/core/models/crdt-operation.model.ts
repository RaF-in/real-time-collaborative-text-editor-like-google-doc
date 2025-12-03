// src/app/core/models/crdt-operation.model.ts
export interface CRDTOperation {
  id?: number;
  docId: string;
  userId: string;
  serverId?: string;
  operationType: 'INSERT' | 'DELETE';
  character?: string;
  fractionalPosition?: string;
  serverSeqNum?: number;
  timestamp?: string;
  processed?: boolean;
}