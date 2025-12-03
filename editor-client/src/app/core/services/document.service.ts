// src/app/core/services/document.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CRDTOperation } from '../models/crdt-operation.model';
import { DocumentSnapshot } from '../models/document-snapshot.model';
import { VersionVector } from '../models/version-vector.model';
import { environment } from '../../../environments/environment';

export interface DocumentStateResponse {
  docId: string;
  snapshot: DocumentSnapshot[];
  versionVector: VersionVector;
  content: string;
}

export interface MissingOperationsRequest {
  docId: string;
  clientVersionVector: VersionVector;
}

export interface MissingOperationsResponse {
  docId: string;
  operations: { [serverId: string]: CRDTOperation[] };
  currentVersionVector: VersionVector;
}

@Injectable({
  providedIn: 'root'
})
export class DocumentService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) { }

  /**
   * Initialize a new document
   */
  initializeDocument(docId: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/api/documents/${docId}/initialize`, {});
  }

  /**
   * Get document state (snapshot + version vector)
   */
  getDocumentState(docId: string): Observable<DocumentStateResponse> {
    return this.http.get<DocumentStateResponse>(`${this.apiUrl}/api/documents/${docId}/state`);
  }

  /**
   * Get version vector
   */
  getVersionVector(docId: string): Observable<VersionVector> {
    return this.http.get<VersionVector>(`${this.apiUrl}/api/documents/${docId}/version`);
  }

  /**
   * Get document content
   */
  getDocumentContent(docId: string): Observable<{ docId: string; content: string }> {
    return this.http.get<{ docId: string; content: string }>(
      `${this.apiUrl}/api/documents/${docId}/content`
    );
  }

  /**
   * Get missing operations
   */
  getMissingOperations(request: MissingOperationsRequest): Observable<MissingOperationsResponse> {
    return this.http.post<MissingOperationsResponse>(
      `${this.apiUrl}/api/documents/missing-operations`,
      request
    );
  }

  /**
   * Create a new document
   */
  createDocument(): Observable<{ id: string; title: string; createdAt: number; status: string }> {
    return this.http.post<{ id: string; title: string; createdAt: number; status: string }>(
      `${this.apiUrl}/api/documents/create`,
      {}
    );
  }

  /**
   * Check if a document exists
   */
  checkDocumentExists(docId: string): Observable<{ exists: boolean; id: string; title?: string; createdAt?: number }> {
    return this.http.get<{ exists: boolean; id: string; title?: string; createdAt?: number }>(
      `${this.apiUrl}/api/documents/${docId}/exists`
    );
  }
}

