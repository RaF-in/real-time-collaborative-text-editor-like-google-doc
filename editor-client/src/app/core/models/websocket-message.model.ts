import { CRDTOperation } from "./crdt-operation.model";
import { DocumentSnapshot } from "./document-snapshot.model";
import { VersionVector } from "./version-vector.model";

// src/app/core/models/websocket-message.model.ts
export interface WebSocketMessage {
  type: MessageType;
  docId?: string;
  userId?: string;
  serverId?: string;
  operation?: CRDTOperation;
  snapshot?: DocumentSnapshot[];
  versionVector?: VersionVector;
  content?: string;
  message?: string;
  timestamp?: number;
  sessionId?: string;
  missingOperations?: { [serverId: string]: CRDTOperation[] };
  currentVersionVector?: VersionVector;
  totalMissing?: number;
  insertAfterPosition?: string | null;
  insertBeforePosition?: string | null;
}


export type MessageType =
  | 'CONNECTED'
  | 'SUBSCRIBE'
  | 'SUBSCRIBED'
  | 'OPERATION'
  | 'OPERATION_ACK'
  | 'OPERATION_BROADCAST'
  | 'SYNC_REQUEST'
  | 'SYNC_RESPONSE'
  | 'USER_JOINED'
  | 'USER_LEFT'
  | 'UNSUBSCRIBE'
  | 'UNSUBSCRIBED'
  | 'PING'
  | 'PONG'
  | 'ERROR';