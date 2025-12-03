// src/app/features/editor/components/editor-content/editor-content.component.ts
import { 
  Component, 
  Input, 
  Output, 
  EventEmitter, 
  ViewChild, 
  ElementRef,
  ChangeDetectionStrategy,
  signal,
  effect
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-editor-content',
  standalone: true,
  imports: [CommonModule, FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="editor-content-wrapper">
      <div class="editor-paper">
        <textarea
          #editorTextarea
          class="editor-textarea"
          [ngModel]="content"
          [disabled]="!isConnected"
          (ngModelChange)="onInput($event)"
          (keydown)="onKeyDown($event)"
          placeholder="Start typing..."
          spellcheck="true"
        ></textarea>
      </div>
    </div>
  `,
  styles: [`
    .editor-content-wrapper {
      flex: 1;
      overflow-y: auto;
      padding: 48px 96px;
      background: #f9fbfd;
    }
    
    .editor-paper {
      max-width: 816px;
      min-height: 1056px;
      margin: 0 auto;
      background: white;
      box-shadow: 0 0 0 0.75pt #d1d1d1, 0 0 3pt 2pt rgba(0,0,0,0.05);
      padding: 96px;
    }
    
    .editor-textarea {
      width: 100%;
      min-height: 864px;
      border: none;
      outline: none;
      font-family: 'Arial', sans-serif;
      font-size: 11pt;
      line-height: 1.5;
      color: #202124;
      resize: none;
      background: transparent;
    }
    
    .editor-textarea:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }
    
    .editor-textarea::placeholder {
      color: #9aa0a6;
    }
  `]
})
export class EditorContentComponent {
  @Input() content = '';
  @Input() isConnected = false;
  @Output() contentChange = new EventEmitter<{ type: 'insert' | 'delete'; char?: string; position: number }>();
  
  @ViewChild('editorTextarea') textarea!: ElementRef<HTMLTextAreaElement>;
  
  private lastContent = signal('');
  
  constructor() {
    // Update lastContent when content changes
    effect(() => {
      this.lastContent.set(this.content);
    });
  }
  
  onInput(newContent: string): void {
    const cursorPosition = this.textarea?.nativeElement.selectionStart || 0;
    const diff = this.findDiff(this.lastContent(), newContent, cursorPosition);
    
    if (diff) {
      this.contentChange.emit(diff);
    }
    
    this.lastContent.set(newContent);
  }
  
  onKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Tab') {
      event.preventDefault();
      const target = event.target as HTMLTextAreaElement;
      const start = target.selectionStart;
      const end = target.selectionEnd;
      
      target.value = target.value.substring(0, start) + '    ' + target.value.substring(end);
      target.selectionStart = target.selectionEnd = start + 4;
    }
  }
  
  private findDiff(
    oldContent: string, 
    newContent: string, 
    cursorPos: number
  ): { type: 'insert' | 'delete'; char?: string; position: number } | null {
    if (newContent.length > oldContent.length) {
      const position = cursorPos - 1;
      const char = newContent[position];
      return { type: 'insert', char, position };
    } else if (newContent.length < oldContent.length) {
      const position = cursorPos;
      return { type: 'delete', position };
    }
    return null;
  }
}

