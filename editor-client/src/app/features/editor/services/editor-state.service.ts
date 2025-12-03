// src/app/features/editor/services/editor-state.service.ts
import { Injectable, signal, computed, effect } from '@angular/core';
import { Subject } from 'rxjs';
import { Collaborator } from '../../../core/models/collaborator.model';
import { CRDTOperation } from '../../../core/models/crdt-operation.model';
import { DocumentSnapshot } from '../../../core/models/document-snapshot.model';
import { LogEntry } from '../../../core/models/log-entry.model';
import { VersionVector } from '../../../core/models/version-vector.model';
import { WebSocketService } from '../../../core/services/websocket.service';
import { WebSocketMessage } from '../../../core/models/websocket-message.model';
import { DocumentService } from '../../../core/services/document.service';
import { CRDTService } from '../../../core/services/crdt.service';
import { LoadBalancerService } from '../../../core/services/load-balancer.service';


@Injectable()
export class EditorStateService {
  
  // Signals for state management (replaces BehaviorSubject)
  readonly docId = signal<string | null>(null);
  readonly userId = signal<string | null>(null);
  readonly content = signal<string>('');
  readonly snapshot = signal<DocumentSnapshot[]>([]);
  readonly versionVector = signal<VersionVector>({});
  readonly isLoading = signal<boolean>(false);
  readonly currentServerId = signal<string | null>(null);
  readonly collaborators = signal<Collaborator[]>([]);
  readonly isSyncing = signal<boolean>(false);
  readonly error = signal<string | null>(null);
  
  // Computed signals
  readonly isConnected = computed(() => this.wsService.isConnected());
  readonly characterCount = computed(() => this.content().length);
  readonly wordCount = computed(() => {
    const text = this.content().trim();
    return text.length > 0 ? text.split(/\s+/).length : 0;
  });
  readonly hasCollaborators = computed(() => this.collaborators().length > 0);
  readonly versionVectorEntries = computed(() => {
    return Object.entries(this.versionVector()).map(([server, seq]) => ({
      server,
      seq
    }));
  });
  
  private isProcessingRemoteOp = false;
  private logs$ = new Subject<LogEntry>();
  
  constructor(
    private wsService: WebSocketService,
    private documentService: DocumentService,
    private crdtService: CRDTService,
    private loadBalancerService: LoadBalancerService
  ) {
    this.setupMessageHandlers();
    this.setupEffects();
  }
  
  /**
   * Setup effects for side effects based on signal changes
   */
  private setupEffects(): void {
    // Log connection state changes
    effect(() => {
      const connected = this.isConnected();
      this.addLog(connected ? 'success' : 'error', 
        `Connection state: ${connected ? 'CONNECTED' : 'DISCONNECTED'}`);
    });
    
    // Log error changes
    effect(() => {
      const err = this.error();
      if (err) {
        this.addLog('error', `Error: ${err}`);
      }
    });
  }
  
  /**
   * Get logs observable
   */
  getLogs() {
    return this.logs$.asObservable();
  }
  
  /**
   * Connect to document
   */
  async connectToDocument(docId: string, userId: string): Promise<void> {
    this.isLoading.set(true);
    this.docId.set(docId);
    this.userId.set(userId);
    this.error.set(null);
    
    this.addLog('info', `Connecting to document: ${docId} as user: ${userId}`);
    
    try {
      // Get server assignment using consistent hashing
      this.addLog('info', 'Getting server assignment from load balancer...');
      const assignment = await this.loadBalancerService
        .getServerAssignment(userId)
        .toPromise();
      
      if (!assignment) {
        throw new Error('Failed to get server assignment');
      }
      
      this.addLog('success', `Assigned to server: ${assignment.serverId}`);
      this.currentServerId.set(assignment.serverId);
      
      // Connect to WebSocket
      this.addLog('info', `Connecting to WebSocket: ${assignment.wsUrl}`);
      this.wsService.connect(assignment.wsUrl);
      
      // Wait for connection
      await this.waitForConnection();
      
      // Subscribe to document
      this.subscribeToDocument(docId, userId);
      
    } catch (error: any) {
      this.addLog('error', `Connection failed: ${error.message}`);
      this.isLoading.set(false);
      this.error.set(error.message);
      throw error;
    }
  }
  
  /**
   * Disconnect from document
   */
  disconnect(): void {
    const doc = this.docId();
    const user = this.userId();
    
    if (doc && user) {
      this.wsService.send({
        type: 'UNSUBSCRIBE',
        docId: doc,
        userId: user
      });
    }
    
    this.wsService.disconnect();
    this.resetState();
    this.addLog('info', 'Disconnected from document');
  }
  
  /**
   * Insert character at position
   */
  insertCharacter(character: string, position: number): void {
    const doc = this.docId();
    const user = this.userId();
    
    if (!doc || !user || !this.isConnected()) {
      console.warn('Cannot insert: not connected');
      return;
    }
    
    const snap = this.snapshot();
    const { afterPosition, beforePosition } = this.crdtService
      .findInsertionPositions(snap, position);
    
    this.wsService.send({
      type: 'OPERATION',
      docId: doc,
      insertAfterPosition: afterPosition,
      insertBeforePosition: beforePosition,
      operation: {
        docId: doc,
        userId: user,
        operationType: 'INSERT',
        character: character
      }
    });
    
    this.addLog('info', `Insert '${character}' after: ${afterPosition}, before: ${beforePosition}`);
  }
  
  /**
   * Delete character at position
   */
  deleteCharacter(position: number): void {
    const doc = this.docId();
    const user = this.userId();
    
    if (!doc || !user || !this.isConnected()) {
      console.warn('Cannot delete: not connected');
      return;
    }
    
    const snap = this.snapshot();
    const fractionalPosition = this.crdtService.findDeletePosition(snap, position);
    
    if (!fractionalPosition) {
      console.warn('Cannot delete: invalid position');
      return;
    }
    
    this.wsService.send({
      type: 'OPERATION',
      docId: doc,
      operation: {
        docId: doc,
        userId: user,
        operationType: 'DELETE',
        fractionalPosition: fractionalPosition
      }
    });
    
    this.addLog('info', `Delete at position: ${fractionalPosition}`);
  }
  
  /**
   * Setup WebSocket message handlers
   */
  private setupMessageHandlers(): void {
    this.wsService.getMessages().subscribe((message) => {
      this.handleMessage(message);
    });
  }
  
  /**
   * Handle incoming WebSocket message
   */
  private handleMessage(message: WebSocketMessage): void {
    switch (message.type) {
      case 'CONNECTED':
        this.handleConnected(message);
        break;
      case 'SUBSCRIBED':
        this.handleSubscribed(message);
        break;
      case 'OPERATION_ACK':
        this.handleOperationAck(message);
        break;
      case 'OPERATION_BROADCAST':
        this.handleOperationBroadcast(message);
        break;
      case 'USER_JOINED':
        this.handleUserJoined(message);
        break;
      case 'USER_LEFT':
        this.handleUserLeft(message);
        break;
      case 'SYNC_RESPONSE':
        this.handleSyncResponse(message);
        break;
      case 'ERROR':
        this.handleError(message);
        break;
    }
  }
  
  private handleConnected(message: WebSocketMessage): void {
    this.addLog('success', `Connected to server: ${message.serverId}`);
    this.currentServerId.set(message.serverId || null);
  }
  
  private handleSubscribed(message: WebSocketMessage): void {
    this.addLog('success', `Subscribed to document: ${message.docId}`);
    
    const snap = message.snapshot || [];
    const vv = message.versionVector || {};
    const cont = message.content || '';
    
    this.snapshot.set(snap);
    this.versionVector.set(vv);
    this.content.set(cont);
    this.isLoading.set(false);
    
    this.addLog('info', `Loaded ${snap.length} characters`);
  }
  
  private handleOperationAck(message: WebSocketMessage): void {
    if (!message.operation) return;
    
    const op = message.operation;
    this.addLog('success', `Operation ACK: seq ${op.serverSeqNum}`);
    
    if (op.serverId && op.serverSeqNum) {
      this.updateVersionVector(op.serverId, op.serverSeqNum);
    }
    
    const newSnapshot = this.crdtService.applyOperation(this.snapshot(), op);
    const newContent = this.crdtService.snapshotToContent(newSnapshot);
    
    this.snapshot.set(newSnapshot);
    this.content.set(newContent);
  }
  
  private handleOperationBroadcast(message: WebSocketMessage): void {
    if (!message.operation) return;
    
    const op = message.operation;
    this.addLog('info', `Received operation from ${op.userId}: ${op.operationType}`);
    
    this.isProcessingRemoteOp = true;
    
    const newSnapshot = this.crdtService.applyOperation(this.snapshot(), op);
    const newContent = this.crdtService.snapshotToContent(newSnapshot);
    
    if (op.serverId && op.serverSeqNum) {
      this.updateVersionVector(op.serverId, op.serverSeqNum);
      
      if (this.crdtService.hasVersionGaps(this.versionVector(), op.serverId, op.serverSeqNum)) {
        this.requestSync();
      }
    }
    
    this.snapshot.set(newSnapshot);
    this.content.set(newContent);
    
    this.isProcessingRemoteOp = false;
  }
  
  private handleUserJoined(message: WebSocketMessage): void {
    if (!message.userId) return;
    
    this.addLog('success', `User joined: ${message.userId}`);
    
    const collaborator: Collaborator = {
      userId: message.userId,
      userName: message.userId,
      avatarColor: this.generateColor(message.userId),
      isTyping: false
    };
    
    this.collaborators.update(collab => [...collab, collaborator]);
  }
  
  private handleUserLeft(message: WebSocketMessage): void {
    if (!message.userId) return;
    
    this.addLog('info', `User left: ${message.userId}`);
    
    this.collaborators.update(collab => 
      collab.filter(c => c.userId !== message.userId)
    );
  }
  
  private handleSyncResponse(message: WebSocketMessage): void {
    if (!message.missingOperations) return;
    
    const totalMissing = message.totalMissing || 0;
    this.addLog('info', `Sync response: ${totalMissing} missing operations`);
    
    const allOps: CRDTOperation[] = [];
    for (const ops of Object.values(message.missingOperations)) {
      allOps.push(...ops);
    }
    
    allOps.sort((a, b) => 
      new Date(a.timestamp || 0).getTime() - new Date(b.timestamp || 0).getTime()
    );
    
    this.isProcessingRemoteOp = true;
    
    let snap = this.snapshot();
    for (const op of allOps) {
      snap = this.crdtService.applyOperation(snap, op);
    }
    
    const cont = this.crdtService.snapshotToContent(snap);
    
    this.snapshot.set(snap);
    this.content.set(cont);
    this.versionVector.set(message.currentVersionVector || this.versionVector());
    this.isSyncing.set(false);
    
    this.isProcessingRemoteOp = false;
    this.addLog('success', `Applied ${allOps.length} missing operations`);
  }
  
  private handleError(message: WebSocketMessage): void {
    this.addLog('error', `Error: ${message.message}`);
    this.error.set(message.message || 'Unknown error');
  }
  
  private subscribeToDocument(docId: string, userId: string): void {
    this.wsService.send({
      type: 'SUBSCRIBE',
      docId,
      userId
    });
  }
  
  private requestSync(): void {
    const doc = this.docId();
    if (!doc || this.isSyncing()) return;
    
    this.addLog('info', 'Requesting sync for missing operations...');
    this.isSyncing.set(true);
    
    this.wsService.send({
      type: 'SYNC_REQUEST',
      docId: doc,
      versionVector: this.versionVector()
    });
  }
  
  private updateVersionVector(serverId: string, seqNum: number): void {
    this.versionVector.update(vv => {
      const currentSeq = vv[serverId] || 0;
      if (seqNum > currentSeq) {
        return { ...vv, [serverId]: seqNum };
      }
      return vv;
    });
  }
  
  private waitForConnection(): Promise<void> {
    return new Promise((resolve, reject) => {
      const timeout = setTimeout(() => reject(new Error('Connection timeout')), 10000);
      
      const checkConnection = () => {
        if (this.wsService.isConnected()) {
          clearTimeout(timeout);
          resolve();
        } else {
          setTimeout(checkConnection, 100);
        }
      };
      
      checkConnection();
    });
  }
  
  private resetState(): void {
    this.docId.set(null);
    this.userId.set(null);
    this.content.set('');
    this.snapshot.set([]);
    this.versionVector.set({});
    this.isLoading.set(false);
    this.currentServerId.set(null);
    this.collaborators.set([]);
    this.isSyncing.set(false);
    this.error.set(null);
  }
  
  private addLog(type: LogEntry['type'], message: string): void {
    this.logs$.next({ time: new Date(), type, message });
    console.log(`[${type.toUpperCase()}] ${message}`);
  }
  
  private generateColor(userId: string): string {
    const colors = [
      '#FF6B6B', '#4ECDC4', '#45B7D1', '#FFA07A', 
      '#98D8C8', '#F7DC6F', '#BB8FCE', '#85C1E2'
    ];
    const hash = userId.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0);
    return colors[hash % colors.length];
  }
  
  isProcessingRemote(): boolean {
    return this.isProcessingRemoteOp;
  }
}