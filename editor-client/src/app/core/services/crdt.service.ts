// src/app/core/services/crdt.service.ts
import { Injectable } from '@angular/core';
import { DocumentSnapshot } from '../models/document-snapshot.model';
import { CRDTOperation } from '../models/crdt-operation.model';

@Injectable({
  providedIn: 'root'
})
export class CRDTService {
  
  /**
   * Apply operation to local snapshot
   */
  applyOperation(
    snapshot: DocumentSnapshot[], 
    operation: CRDTOperation
  ): DocumentSnapshot[] {
    const newSnapshot = [...snapshot];
    
    if (operation.operationType === 'INSERT' && operation.fractionalPosition) {
      // Add new character
      newSnapshot.push({
        id: 0, // Temporary ID
        docId: operation.docId,
        fractionalPosition: operation.fractionalPosition,
        character: operation.character || '',
        serverId: operation.serverId || '',
        serverSeqNum: operation.serverSeqNum || 0,
        createdAt: new Date().toISOString(),
        active: true
      });
      
      // Sort by fractional position
      newSnapshot.sort((a, b) => 
        a.fractionalPosition.localeCompare(b.fractionalPosition)
      );
      
    } else if (operation.operationType === 'DELETE' && operation.fractionalPosition) {
      // Remove character
      const index = newSnapshot.findIndex(
        item => item.fractionalPosition === operation.fractionalPosition
      );
      if (index !== -1) {
        newSnapshot.splice(index, 1);
      }
    }
    
    return newSnapshot;
  }
  
  /**
   * Rebuild content from snapshot
   */
  snapshotToContent(snapshot: DocumentSnapshot[]): string {
    return snapshot
      .filter(item => item.active)
      .map(item => item.character)
      .join('');
  }
  
  /**
   * Find insertion positions for a character at given index
   */
  findInsertionPositions(
    snapshot: DocumentSnapshot[], 
    index: number
  ): { afterPosition: string | null; beforePosition: string | null } {
    const afterPosition = index > 0 && snapshot[index - 1]
      ? snapshot[index - 1].fractionalPosition
      : null;
    
    const beforePosition = index < snapshot.length
      ? snapshot[index].fractionalPosition
      : null;
    
    return { afterPosition, beforePosition };
  }
  
  /**
   * Find character position to delete
   */
  findDeletePosition(snapshot: DocumentSnapshot[], index: number): string | null {
    return index < snapshot.length ? snapshot[index].fractionalPosition : null;
  }
  
  /**
   * Detect if there are gaps in version vector
   */
  hasVersionGaps(
    clientVector: { [key: string]: number }, 
    receivedServerId: string, 
    receivedSeq: number
  ): boolean {
    const clientSeq = clientVector[receivedServerId] || 0;
    return receivedSeq > clientSeq + 1;
  }
}

