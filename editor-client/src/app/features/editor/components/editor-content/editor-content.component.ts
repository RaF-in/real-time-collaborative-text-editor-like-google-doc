// src/app/features/editor/components/editor-content/editor-content.component.ts
import {
  Component,
  Input,
  Output,
  EventEmitter,
  viewChild,
  ElementRef,
  ChangeDetectionStrategy,
  signal,
  effect,
  AfterContentChecked
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

interface KeyInfo {
  key: string;
  cursorPos: number;
  selectionStart: number;
  selectionEnd: number;
  timestamp: number;
}

interface DiffResult {
  type: 'insert' | 'delete';
  char?: string;
  position: number;
}

@Component({
  selector: 'app-editor-content',
  standalone: true,
  imports: [CommonModule, FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './editor-content.component.html',
  styleUrls: ['./editor-content.component.css']
})
export class EditorContentComponent implements AfterContentChecked {
  @Input() content = '';
  @Input() isConnected = false;
  @Input() canEdit = signal(false);
  @Output() contentChange = new EventEmitter<{ type: 'insert' | 'delete'; char?: string; position: number }>();

  // Use viewChild with signal for modern Angular
  readonly textarea = viewChild<ElementRef<HTMLTextAreaElement>>('editorTextarea');

  private lastContent = signal('');

  // Tracking variables for hybrid approach
  private lastCursorPosition = 0;
  private lastSelectionStart = 0;
  private lastSelectionEnd = 0;
  private pendingKeyInfo: KeyInfo | null = null;
  private isProcessingRemoteUpdate = false;
  private lastLocalContent = '';
  private updateQueue: (() => void)[] = [];

  constructor() {
    effect(() => {
      this.lastContent.set(this.content);
      // Initialize lastLocalContent on first run
      if (this.lastLocalContent === '') {
        this.lastLocalContent = this.content;
      }
      // Reset remote update flag when content changes from outside
      if (this.content !== this.lastLocalContent && !this.isProcessingRemoteUpdate) {
        this.scheduleCursorPositionRestore();
      }
    });
  }

  ngAfterContentChecked(): void {
    // Process any scheduled updates
    while (this.updateQueue.length > 0) {
      const update = this.updateQueue.shift();
      if (update) {
        update();
      }
    }
  }

  onInput(newContent: string): void {
    // Skip processing if this is a remote update
    if (this.isProcessingRemoteUpdate) {
      this.isProcessingRemoteUpdate = false;
      return;
    }

    const textarea = this.textarea()?.nativeElement;
    if (!textarea) return;

    const currentCursorPosition = textarea.selectionStart || 0;
    const currentSelectionStart = textarea.selectionStart || 0;
    const currentSelectionEnd = textarea.selectionEnd || 0;

    // Calculate diff using hybrid approach
    const diff = this.calculateDiff(
      this.pendingKeyInfo,
      this.lastContent(),
      newContent,
      {
        cursorBefore: this.lastCursorPosition,
        cursorAfter: currentCursorPosition,
        selectionBefore: { start: this.lastSelectionStart, end: this.lastSelectionEnd },
        selectionAfter: { start: currentSelectionStart, end: currentSelectionEnd }
      }
    );

    // Clear pending key info after processing
    this.pendingKeyInfo = null;

    if (diff) {
      // Emit the operation
      this.contentChange.emit({
        type: diff.type,
        char: diff.char,
        position: diff.position
      });
    }

    // Update tracking variables
    this.lastContent.set(newContent);
    this.lastLocalContent = newContent;
    this.lastCursorPosition = currentCursorPosition;
    this.lastSelectionStart = currentSelectionStart;
    this.lastSelectionEnd = currentSelectionEnd;
  }

  onKeyDown(event: KeyboardEvent): void {
    const textarea = event.target as HTMLTextAreaElement;
    const cursorPos = textarea.selectionStart || 0;
    const selectionStart = textarea.selectionStart || 0;
    const selectionEnd = textarea.selectionEnd || 0;

    // Track key information for hybrid detection
    const keyToTrack = ['Backspace', 'Delete', 'Enter'];
    if (keyToTrack.includes(event.key)) {
      this.pendingKeyInfo = {
        key: event.key,
        cursorPos: cursorPos,
        selectionStart: selectionStart,
        selectionEnd: selectionEnd,
        timestamp: Date.now()
      };
    }

    // Handle Tab key
    if (event.key === 'Tab') {
      event.preventDefault();
      const start = textarea.selectionStart;
      const end = textarea.selectionEnd;

      // Insert spaces manually
      textarea.value = textarea.value.substring(0, start) + '    ' + textarea.value.substring(end);
      textarea.selectionStart = textarea.selectionEnd = start + 4;

      // Trigger input event manually since we prevented default
      this.onInput(textarea.value);
      return;
    }

    // Save cursor position before the key takes effect
    this.lastCursorPosition = cursorPos;
    this.lastSelectionStart = selectionStart;
    this.lastSelectionEnd = selectionEnd;
  }

  /**
   * Hybrid diff calculation algorithm that combines multiple detection methods
   */
  private calculateDiff(
    keyInfo: KeyInfo | null,
    oldContent: string,
    newContent: string,
    cursorInfo: {
      cursorBefore: number;
      cursorAfter: number;
      selectionBefore: { start: number; end: number };
      selectionAfter: { start: number; end: number };
    }
  ): DiffResult | null {
    // No change detected
    if (oldContent === newContent) {
      return null;
    }

    const lengthDiff = newContent.length - oldContent.length;

    // Method 1: Use key information if available (most reliable)
    if (keyInfo && Date.now() - keyInfo.timestamp < 100) {
      const keyBasedDiff = this.calculateDiffFromKey(keyInfo, oldContent, newContent);
      if (keyBasedDiff) {
        return keyBasedDiff;
      }
    }

    // Method 2: Use cursor movement and selection information
    const cursorBasedDiff = this.calculateDiffFromCursor(
      lengthDiff,
      cursorInfo,
      oldContent,
      newContent
    );
    if (cursorBasedDiff) {
      return cursorBasedDiff;
    }

    // Method 3: Smart string comparison as fallback
    return this.calculateDiffFromStrings(oldContent, newContent, cursorInfo.cursorAfter);
  }

  /**
   * Calculate diff based on the key that was pressed
   */
  private calculateDiffFromKey(
    keyInfo: KeyInfo,
    oldContent: string,
    newContent: string
  ): DiffResult | null {
    const wasSelection = keyInfo.selectionStart !== keyInfo.selectionEnd;

    switch (keyInfo.key) {
      case 'Backspace':
        if (wasSelection) {
          // Delete selection
          return { type: 'delete', position: keyInfo.selectionStart };
        } else {
          // Delete character before cursor
          const position = Math.max(0, keyInfo.cursorPos - 1);
          return { type: 'delete', position };
        }

      case 'Delete':
        if (wasSelection) {
          // Delete selection
          return { type: 'delete', position: keyInfo.selectionStart };
        } else {
          // Delete character at cursor
          return { type: 'delete', position: keyInfo.cursorPos };
        }

      case 'Enter':
        // Insert newline character
        return {
          type: 'insert',
          char: '\n',
          position: keyInfo.cursorPos
        };
    }

    return null;
  }

  /**
   * Calculate diff based on cursor movement patterns
   */
  private calculateDiffFromCursor(
    lengthDiff: number,
    cursorInfo: {
      cursorBefore: number;
      cursorAfter: number;
      selectionBefore: { start: number; end: number };
      selectionAfter: { start: number; end: number };
    },
    oldContent: string,
    newContent: string
  ): DiffResult | null {
    const wasSelection = cursorInfo.selectionBefore.start !== cursorInfo.selectionBefore.end;

    if (lengthDiff > 0) {
      // INSERT operation
      if (cursorInfo.cursorAfter > cursorInfo.cursorBefore) {
        // Cursor moved forward - likely insert at cursorBefore
        const position = cursorInfo.cursorBefore;
        const char = this.findInsertedCharacter(oldContent, newContent, position);
        return { type: 'insert', char, position };
      } else if (wasSelection) {
        // Replaced selection with something
        return this.calculateDiffFromStrings(oldContent, newContent, cursorInfo.selectionBefore.start);
      }
    } else if (lengthDiff < 0) {
      // DELETE operation
      const deletedLength = Math.abs(lengthDiff);

      if (wasSelection) {
        // Selection was deleted
        return { type: 'delete', position: cursorInfo.selectionBefore.start };
      } else if (cursorInfo.cursorAfter < cursorInfo.cursorBefore) {
        // Cursor moved left - backspace
        return { type: 'delete', position: cursorInfo.cursorAfter };
      } else if (cursorInfo.cursorAfter === cursorInfo.cursorBefore) {
        // Cursor didn't move - could be delete key
        return { type: 'delete', position: cursorInfo.cursorBefore };
      }
    }

    return null;
  }

  /**
   * Calculate diff by comparing strings (fallback method)
   */
  private calculateDiffFromStrings(
    oldContent: string,
    newContent: string,
    cursorPos: number
  ): DiffResult | null {
    const lengthDiff = newContent.length - oldContent.length;

    if (lengthDiff > 0) {
      // INSERT: Find where characters differ
      for (let i = 0; i < newContent.length; i++) {
        if (i >= oldContent.length || oldContent[i] !== newContent[i]) {
          const char = newContent[i];
          return { type: 'insert', char, position: i };
        }
      }
    } else if (lengthDiff < 0) {
      // DELETE: Find where characters differ
      const deletedLength = Math.abs(lengthDiff);

      for (let i = 0; i < Math.min(oldContent.length, newContent.length); i++) {
        if (oldContent[i] !== newContent[i]) {
          return { type: 'delete', position: i };
        }
      }

      // If all characters up to newContent.length match, deletion was at the end
      if (newContent.length < oldContent.length) {
        return { type: 'delete', position: newContent.length };
      }
    }

    return null;
  }

  /**
   * Find the character that was inserted at a specific position
   */
  private findInsertedCharacter(
    oldContent: string,
    newContent: string,
    position: number
  ): string {
    if (position < newContent.length && position < oldContent.length) {
      // Character might have shifted
      if (newContent[position] !== oldContent[position]) {
        return newContent[position];
      }
    }

    if (position < newContent.length && position >= oldContent.length) {
      // New character at the end
      return newContent[position];
    }

    // Default: return the character at the position in new content
    return newContent[Math.min(position, newContent.length - 1)];
  }

  /**
   * Schedule cursor position restoration after remote updates
   */
  private scheduleCursorPositionRestore(): void {
    this.updateQueue.push(() => {
      const textarea = this.textarea()?.nativeElement;
      if (textarea) {
        // Only restore if within valid range
        if (this.lastCursorPosition >= 0 && this.lastCursorPosition <= textarea.value.length) {
          textarea.setSelectionRange(this.lastCursorPosition, this.lastCursorPosition);
        }
      }
    });
  }

  /**
   * Mark that content is being updated from remote source
   */
  public markRemoteUpdate(): void {
    this.isProcessingRemoteUpdate = true;
  }
}