// src/app/core/services/websocket.service.ts
import { Injectable, OnDestroy, signal, computed } from '@angular/core';
import { Observable, Subject, timer } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';
import { ConnectionState } from '../models/connection-state.model';
import { WebSocketMessage, MessageType } from '../models/websocket-message.model';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService implements OnDestroy {
  private socket: WebSocket | null = null;
  private destroy$ = new Subject<void>();
  
  // Signals for reactive state
  readonly connectionState = signal<ConnectionState>('DISCONNECTED');
  readonly isConnected = computed(() => this.connectionState() === 'CONNECTED');
  readonly isConnecting = computed(() => this.connectionState() === 'CONNECTING');
  readonly isDisconnected = computed(() => this.connectionState() === 'DISCONNECTED');
  
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectDelay = 1000;
  
  // Observable streams for messages (still useful for RxJS operators)
  private messageSubject$ = new Subject<WebSocketMessage>();
  private errorSubject$ = new Subject<Event>();
  
  constructor() {}
  
  /**
   * Connect to WebSocket server
   */
  connect(wsUrl: string): Observable<WebSocketMessage> {
    if (this.socket?.readyState === WebSocket.OPEN) {
      console.log('WebSocket already connected');
      return this.messageSubject$.asObservable();
    }
    
    this.connectionState.set('CONNECTING');
    console.log('Connecting to WebSocket:', wsUrl);
    
    try {
      this.socket = new WebSocket(wsUrl);
      this.setupSocketHandlers();
    } catch (error) {
      console.error('WebSocket connection error:', error);
      this.connectionState.set('DISCONNECTED');
      this.scheduleReconnect(wsUrl);
    }
    
    return this.messageSubject$.asObservable();
  }
  
  /**
   * Setup WebSocket event handlers
   */
  private setupSocketHandlers(): void {
    if (!this.socket) return;
    
    this.socket.onopen = () => {
      console.log('WebSocket connected successfully');
      this.connectionState.set('CONNECTED');
      this.reconnectAttempts = 0;
      this.reconnectDelay = 1000;
      this.startHeartbeat();
    };
    
    this.socket.onmessage = (event) => {
      try {
        const message: WebSocketMessage = JSON.parse(event.data);
        console.log('Received WebSocket message:', message.type, message);
        this.messageSubject$.next(message);
      } catch (error) {
        console.error('Error parsing WebSocket message:', error);
      }
    };
    
    this.socket.onerror = (event) => {
      console.error('WebSocket error:', event);
      this.errorSubject$.next(event);
      this.connectionState.set('DISCONNECTED');
    };
    
    this.socket.onclose = (event) => {
      console.log('WebSocket closed:', event.code, event.reason);
      this.connectionState.set('DISCONNECTED');
      
      if (event.code !== 1000) {
        this.scheduleReconnect(this.socket?.url || '');
      }
    };
  }
  
  /**
   * Send message to server
   */
  send(message: WebSocketMessage): void {
    if (this.socket?.readyState === WebSocket.OPEN) {
      const jsonMessage = JSON.stringify(message);
      console.log('Sending WebSocket message:', message.type);
      this.socket.send(jsonMessage);
    } else {
      console.warn('WebSocket not connected. Message not sent:', message);
    }
  }
  
  /**
   * Disconnect from WebSocket
   */
  disconnect(): void {
    if (this.socket) {
      console.log('Disconnecting WebSocket');
      this.socket.close(1000, 'Client disconnect');
      this.socket = null;
      this.connectionState.set('DISCONNECTED');
    }
  }
  
  /**
   * Get messages by type
   */
  getMessagesByType(messageType: MessageType): Observable<WebSocketMessage> {
    return this.messageSubject$.pipe(
      filter(msg => msg.type === messageType)
    );
  }
  
  /**
   * Get all messages
   */
  getMessages(): Observable<WebSocketMessage> {
    return this.messageSubject$.asObservable();
  }
  
  /**
   * Get errors
   */
  getErrors(): Observable<Event> {
    return this.errorSubject$.asObservable();
  }
  
  /**
   * Schedule reconnection with exponential backoff
   */
  private scheduleReconnect(wsUrl: string): void {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('Max reconnection attempts reached');
      return;
    }
    
    this.reconnectAttempts++;
    const delay = this.reconnectDelay * Math.pow(2, this.reconnectAttempts - 1);
    
    console.log(`Reconnecting in ${delay}ms (attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts})`);
    
    timer(delay)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        console.log('Attempting to reconnect...');
        this.connect(wsUrl);
      });
  }
  
  /**
   * Start heartbeat to keep connection alive
   */
  private startHeartbeat(): void {
    timer(0, 30000)
      .pipe(
        takeUntil(this.destroy$),
        filter(() => this.socket?.readyState === WebSocket.OPEN)
      )
      .subscribe(() => {
        this.send({ type: 'PING' });
      });
  }
  
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.disconnect();
  }
}