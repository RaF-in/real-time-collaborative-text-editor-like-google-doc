import { Collaborator } from "./collaborator.model";
import { DocumentSnapshot } from "./document-snapshot.model";
import { VersionVector } from "./version-vector.model";

// src/app/core/models/editor-state.model.ts
export interface EditorState {
  docId: string | null;
  userId: string | null;
  content: string;
  snapshot: DocumentSnapshot[];
  versionVector: VersionVector;
  isConnected: boolean;
  isLoading: boolean;
  currentServerId: string | null;
  collaborators: Collaborator[];
  isSyncing: boolean;
  error: string | null;
}