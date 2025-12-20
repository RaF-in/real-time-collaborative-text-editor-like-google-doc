import { inject } from '@angular/core';
import { Router, ActivatedRouteSnapshot, CanActivateFn } from '@angular/router';
import { map, catchError, of, switchMap } from 'rxjs';
import { DocumentSharingService } from '../services/document-sharing.service';
import { DocumentService } from '../services/document.service';

export const documentAccessGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const sharingService = inject(DocumentSharingService);
  const documentService = inject(DocumentService);
  const router = inject(Router);

  const documentId = route.paramMap.get('id');
  if (!documentId) {
    router.navigate(['/home']);
    return false;
  }

  // Check if document exists by calling snapshot server directly
  return documentService.checkDocumentExists(documentId).pipe(
    switchMap(existsResponse => {
      // If document exists, check access permissions
      if (existsResponse.exists) {
        return sharingService.getAccessInfo(documentId).pipe(
          map(accessInfo => {
            if (accessInfo.userPermission) {
              return true;
            }

            if (accessInfo.canRequestAccess) {
              router.navigate(['/documents', documentId, 'request-access']);
              return false;
            }

            router.navigate(['/no-access'], { queryParams: { documentId } });
            return false;
          })
        );
      }

      // Document doesn't exist - allow access for creation
      return of(true);
    }),
    catchError((error) => {
      // If the error is a 404 (document not found), allow access for creation
      if (error.status === 404) {
        return of(true);
      }

      // For other errors, navigate to homepage
      router.navigate(['/home']);
      return of(false);
    })
  );
};