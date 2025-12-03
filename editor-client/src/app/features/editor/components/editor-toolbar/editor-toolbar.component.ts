// src/app/features/editor/components/editor-toolbar/editor-toolbar.component.ts
import { Component, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-editor-toolbar',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="toolbar">
      <div class="toolbar-section">
        <button class="toolbar-btn" title="Undo">
          <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
            <path d="M8 5v4H4V5h4zm-2 3V6H5v2h1zm5-3h6v1h-6V5zm0 4h4v1h-4V9zm0 4h6v1h-6v-1zM4 9v6h6V9H4zm5 5H5v-4h4v4z"/>
          </svg>
        </button>
        <button class="toolbar-btn" title="Redo">
          <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
            <path d="M12 5v4h4V5h-4zm2 3v-2h1v2h-1zM5 5h6v1H5V5zm0 4h4v1H5V9zm0 4h6v1H5v-1zm6 0v6h6v-6h-6zm5 5h-4v-4h4v4z"/>
          </svg>
        </button>
        <button class="toolbar-btn" title="Print">
          <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
            <path d="M14 2H6v4h8V2zM6 16h8v2H6v-2zm8-1H6v-3h8v3zm2-11v4h2V4h-2zm-2 7v2h2v-2h-2zM4 6H2v8h2V6zm12 8h2v-2h-2v2z"/>
          </svg>
        </button>
      </div>
      
      <div class="toolbar-divider"></div>
      
      <div class="toolbar-section">
        <select class="toolbar-select" title="Font">
          <option>Arial</option>
          <option>Times New Roman</option>
          <option>Courier New</option>
          <option>Calibri</option>
        </select>
        
        <select class="toolbar-select" title="Font size">
          <option>8</option>
          <option>9</option>
          <option>10</option>
          <option selected>11</option>
          <option>12</option>
          <option>14</option>
          <option>18</option>
          <option>24</option>
        </select>
      </div>
      
      <div class="toolbar-divider"></div>
      
      <div class="toolbar-section">
        <button class="toolbar-btn" title="Bold">
          <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
            <path d="M7 3v14h5.5c2.21 0 4-1.79 4-4 0-1.45-.78-2.72-1.94-3.42A3.99 3.99 0 0013 6c0-2.21-1.79-4-4-4H7zm2 2h2c1.1 0 2 .9 2 2s-.9 2-2 2H9V5zm0 6h3c1.1 0 2 .9 2 2s-.9 2-2 2H9v-4z"/>
          </svg>
        </button>
        <button class="toolbar-btn" title="Italic">
          <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
            <path d="M8 3v2h2.5l-3 10H5v2h8v-2h-2.5l3-10H16V3H8z"/>
          </svg>
        </button>
        <button class="toolbar-btn" title="Underline">
          <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
            <path d="M6 3v7c0 2.21 1.79 4 4 4s4-1.79 4-4V3h-2v7c0 1.1-.9 2-2 2s-2-.9-2-2V3H6zm-2 14h12v2H4v-2z"/>
          </svg>
        </button>
      </div>
      
      <div class="toolbar-divider"></div>
      
      <div class="toolbar-section">
        <button class="toolbar-btn" title="Align left">
          <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
            <path d="M3 3h14v2H3V3zm0 4h10v2H3V7zm0 4h14v2H3v-2zm0 4h10v2H3v-2z"/>
          </svg>
        </button>
        <button class="toolbar-btn" title="Align center">
          <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
            <path d="M3 3h14v2H3V3zm2 4h10v2H5V7zm-2 4h14v2H3v-2zm2 4h10v2H5v-2z"/>
          </svg>
        </button>
        <button class="toolbar-btn" title="Align right">
          <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
            <path d="M3 3h14v2H3V3zm4 4h10v2H7V7zm-4 4h14v2H3v-2zm4 4h10v2H7v-2z"/>
          </svg>
        </button>
      </div>
    </div>
  `,
  styles: [`
    .toolbar {
      display: flex;
      align-items: center;
      gap: 4px;
      padding: 8px 24px;
      background: #edf2fa;
      border-bottom: 1px solid #c6c6c6;
      overflow-x: auto;
    }
    
    .toolbar-section {
      display: flex;
      align-items: center;
      gap: 2px;
    }
    
    .toolbar-divider {
      width: 1px;
      height: 24px;
      background: #dadce0;
      margin: 0 8px;
    }
    
    .toolbar-btn {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 32px;
      height: 32px;
      background: none;
      border: none;
      border-radius: 4px;
      color: #444746;
      cursor: pointer;
      transition: background 0.2s;
    }
    
    .toolbar-btn:hover {
      background: rgba(26, 115, 232, 0.08);
    }
    
    .toolbar-btn:active {
      background: rgba(26, 115, 232, 0.16);
    }
    
    .toolbar-select {
      height: 32px;
      padding: 0 8px;
      border: 1px solid #dadce0;
      border-radius: 4px;
      background: white;
      font-size: 13px;
      color: #444746;
      cursor: pointer;
      outline: none;
    }
    
    .toolbar-select:hover {
      border-color: #1a73e8;
    }
    
    .toolbar-select:focus {
      border-color: #1a73e8;
      box-shadow: 0 0 0 2px rgba(26, 115, 232, 0.1);
    }
  `]
})
export class EditorToolbarComponent {}

