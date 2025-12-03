// src/app/features/homepage/homepage.component.ts
import { Component, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

@Component({
    selector: 'app-homepage',
    standalone: true,
    imports: [CommonModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    template: `
    <div class="homepage-container">
      <div class="homepage-content">
        <!-- Header Section -->
        <header class="homepage-header">
          <div class="logo-section">
            <div class="logo-icon">📝</div>
            <h1 class="app-title">Collaborative Editor</h1>
          </div>
          <p class="app-subtitle">Create and collaborate on documents in real-time</p>
        </header>

        <!-- Start New Document Section -->
        <section class="document-section">
          <h2 class="section-title">Start a new document</h2>
          
          <div class="document-templates">
            <!-- Blank Document Card -->
            <div class="template-card blank-doc" (click)="createBlankDocument()">
              <div class="template-preview">
                <div class="blank-page">
                  <div class="blank-line"></div>
                  <div class="blank-line"></div>
                  <div class="blank-line"></div>
                  <div class="blank-line short"></div>
                </div>
              </div>
              <div class="template-info">
                <div class="template-icon">+</div>
                <span class="template-name">Blank</span>
              </div>
            </div>
          </div>
        </section>

        <!-- Recent Documents Section (Optional - for future enhancement) -->
        <section class="recent-section">
          <h2 class="section-title">Recent documents</h2>
          <div class="empty-state">
            <div class="empty-icon">📄</div>
            <p class="empty-text">No recent documents</p>
            <p class="empty-subtext">Documents you create will appear here</p>
          </div>
        </section>
      </div>
    </div>
  `,
    styles: [`
    .homepage-container {
      min-height: 100vh;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      padding: 24px;
      display: flex;
      justify-content: center;
      align-items: flex-start;
    }

    .homepage-content {
      max-width: 1200px;
      width: 100%;
      padding-top: 60px;
    }

    /* Header Section */
    .homepage-header {
      text-align: center;
      margin-bottom: 64px;
      color: white;
    }

    .logo-section {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 16px;
      margin-bottom: 16px;
    }

    .logo-icon {
      font-size: 48px;
      filter: drop-shadow(0 4px 8px rgba(0,0,0,0.2));
    }

    .app-title {
      font-size: 48px;
      font-weight: 300;
      margin: 0;
      letter-spacing: -0.5px;
      text-shadow: 0 2px 4px rgba(0,0,0,0.1);
    }

    .app-subtitle {
      font-size: 18px;
      font-weight: 300;
      margin: 0;
      opacity: 0.95;
    }

    /* Document Section */
    .document-section {
      margin-bottom: 48px;
    }

    .section-title {
      font-size: 16px;
      font-weight: 500;
      color: white;
      margin-bottom: 24px;
      opacity: 0.9;
    }

    .document-templates {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
      gap: 24px;
    }

    /* Template Card */
    .template-card {
      background: white;
      border-radius: 8px;
      overflow: hidden;
      cursor: pointer;
      transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
      box-shadow: 0 2px 8px rgba(0,0,0,0.1);
    }

    .template-card:hover {
      transform: translateY(-8px);
      box-shadow: 0 12px 24px rgba(0,0,0,0.2);
    }

    .template-card:active {
      transform: translateY(-4px);
    }

    .template-preview {
      height: 240px;
      background: #f9f9f9;
      display: flex;
      align-items: center;
      justify-content: center;
      border-bottom: 1px solid #e0e0e0;
      position: relative;
      overflow: hidden;
    }

    .blank-page {
      width: 140px;
      height: 180px;
      background: white;
      border-radius: 4px;
      box-shadow: 0 2px 8px rgba(0,0,0,0.1);
      padding: 20px;
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .blank-line {
      height: 3px;
      background: linear-gradient(90deg, #e0e0e0 0%, #f5f5f5 100%);
      border-radius: 2px;
    }

    .blank-line.short {
      width: 60%;
    }

    .template-info {
      padding: 16px;
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .template-icon {
      width: 32px;
      height: 32px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 20px;
      font-weight: 300;
    }

    .template-name {
      font-size: 14px;
      font-weight: 500;
      color: #202124;
    }

    /* Recent Section */
    .recent-section {
      background: rgba(255, 255, 255, 0.1);
      backdrop-filter: blur(10px);
      border-radius: 12px;
      padding: 32px;
      border: 1px solid rgba(255, 255, 255, 0.2);
    }

    .empty-state {
      text-align: center;
      padding: 48px 24px;
    }

    .empty-icon {
      font-size: 64px;
      opacity: 0.5;
      margin-bottom: 16px;
    }

    .empty-text {
      font-size: 18px;
      font-weight: 500;
      color: white;
      margin: 0 0 8px 0;
    }

    .empty-subtext {
      font-size: 14px;
      color: rgba(255, 255, 255, 0.8);
      margin: 0;
    }

    /* Responsive Design */
    @media (max-width: 768px) {
      .homepage-content {
        padding-top: 32px;
      }

      .app-title {
        font-size: 36px;
      }

      .logo-icon {
        font-size: 36px;
      }

      .document-templates {
        grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
        gap: 16px;
      }

      .template-preview {
        height: 200px;
      }

      .blank-page {
        width: 100px;
        height: 140px;
        padding: 16px;
      }
    }
  `]
})
export class HomepageComponent {
    constructor(private router: Router) { }

    createBlankDocument(): void {
        // Generate unique document ID
        const timestamp = Date.now();
        const randomStr = Math.random().toString(36).substring(2, 9);
        const docId = `doc-${timestamp}-${randomStr}`;

        // Navigate to editor with new document ID
        this.router.navigate(['/editor', docId]);
    }
}
