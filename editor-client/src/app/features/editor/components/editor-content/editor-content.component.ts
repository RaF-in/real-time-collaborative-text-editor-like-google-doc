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
  templateUrl: './editor-content.component.html',
  styleUrls: ['./editor-content.component.css']
})
export class EditorContentComponent {
  @Input() content = '';
  @Input() isConnected = false;
  @Output() contentChange = new EventEmitter<{ type: 'insert' | 'delete'; char?: string; position: number }>();
  
  @ViewChild('editorTextarea') textarea!: ElementRef<HTMLTextAreaElement>;
  
  private lastContent = signal('');
  
  constructor() {
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