// src/app/features/homepage/homepage.component.ts
import { Component, ChangeDetectionStrategy, OnInit, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { DocumentService } from '../../core/services/document.service';
import { User } from '../../core/models/user.model';
import { TruncatePipe } from '../../shared/pipes/truncate.pipe';
import { RelativeTimePipe } from '../../shared/pipes/relative-time.pipe';

export interface Document {
  id: string;
  title: string;
  content?: string;
  lastModified: Date;
  createdBy: string;
}

@Component({
    selector: 'app-homepage',
    standalone: true,
    imports: [
        CommonModule,
        RouterLink,
        TruncatePipe,
        RelativeTimePipe
    ],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './homepage.component.html',
    styleUrls: ['./homepage.component.css']
})
export class HomepageComponent implements OnInit {
    private authService: AuthService;
    private documentService: DocumentService;
    private router: Router;
    currentUser: () => User | null;
    recentDocuments: Document[] = [];
    showUserMenu = false;
    isCreatingDocument = false;

    constructor(
        authService: AuthService,
        documentService: DocumentService,
        router: Router
    ) {
        this.authService = authService;
        this.documentService = documentService;
        this.router = router;
        this.currentUser = this.authService.currentUser;
    }

    ngOnInit() {
        this.loadRecentDocuments();
    }

    loadRecentDocuments() {
        // TODO: Replace with actual API call
        this.recentDocuments = [
            {
                id: 'doc-1',
                title: 'Meeting Notes',
                content: 'Meeting discussion points:\n1. Q1 goals\n2. Budget allocation\n3. Team updates',
                lastModified: new Date(Date.now() - 3600000), // 1 hour ago
                createdBy: 'You'
            },
            {
                id: 'doc-2',
                title: 'Project Proposal',
                content: 'Executive Summary\n\nThis proposal outlines the implementation strategy for our new collaborative text editor platform...',
                lastModified: new Date(Date.now() - 86400000), // 1 day ago
                createdBy: 'You'
            },
            {
                id: 'doc-3',
                title: 'Q4 Goals & Objectives',
                content: 'Product Goals:\n- Launch beta version\n- Onboard 1000+ users\n- Implement real-time collaboration',
                lastModified: new Date(Date.now() - 604800000), // 1 week ago
                createdBy: 'You'
            }
        ];
    }

    createBlankDocument(): void {
        if (this.isCreatingDocument) {
            return; // Prevent multiple clicks while creating
        }

        this.isCreatingDocument = true;

        // Step 1: Create document via main server API
        this.documentService.createDocument().subscribe({
            next: (createResponse) => {
                const docId = createResponse.id;

                // Step 2: Initialize document in snapshot database
                this.documentService.initializeDocument(docId).subscribe({
                    next: () => {
                        // Step 3: Navigate to editor with created document ID
                        this.router.navigate(['/editor', docId]);
                        this.isCreatingDocument = false;
                    },
                    error: (initError) => {
                        console.error('Failed to initialize document in snapshot:', initError);
                        // Still navigate even if snapshot initialization fails,
                        // the document exists in main server and can be initialized later
                        this.router.navigate(['/editor', docId]);
                        this.isCreatingDocument = false;
                    }
                });
            },
            error: (createError) => {
                console.error('Failed to create document:', createError);
                this.isCreatingDocument = false;
                // Show error message to user (you could implement a toast notification here)
                alert('Failed to create document. Please try again.');
            }
        });
    }

    openDocument(documentId: string): void {
        this.router.navigate(['/editor', documentId]);
    }

    getDocumentPreview(content?: string): string {
        if (!content) return 'No content';
        return content.replace(/[#*`\[\]]/g, '').substring(0, 150);
    }

    toggleUserMenu(): void {
        this.showUserMenu = !this.showUserMenu;
    }

    logout(): void {
        this.authService.logout().subscribe({
            next: () => {
                // Logout successful
            },
            error: (error) => {
                console.error('Logout error:', error);
                // Even on error, local state will be cleared by authService
            }
        });
        this.showUserMenu = false;
    }

    getInitials(): string {
        const user = this.currentUser();
        if (!user) return '?';

        if (user.username) {
            return user.username.substring(0, 2).toUpperCase();
        }
        if (user.email) {
            return user.email.substring(0, 2).toUpperCase();
        }
        return 'U';
    }

    @HostListener('document:click', ['$event'])
    onDocumentClick(event: MouseEvent) {
        if (this.showUserMenu && !(event.target as Element).closest('.user-menu')) {
            this.showUserMenu = false;
        }
    }
}