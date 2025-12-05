// src/app/features/homepage/homepage.component.ts
import { Component, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

@Component({
    selector: 'app-homepage',
    standalone: true,
    imports: [CommonModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './homepage.component.html',
    styleUrls: ['./homepage.component.css']
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