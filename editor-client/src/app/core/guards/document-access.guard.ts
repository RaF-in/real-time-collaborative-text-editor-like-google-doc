import { inject } from '@angular/core';
import { Router, ActivatedRouteSnapshot, CanActivateFn } from '@angular/router';
import { map, catchError, of } from 'rxjs';
import { DocumentSharingService } from '../services/document-sharing.service';

export const documentAccessGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const sharingService = inject(DocumentSharingService);
  const router = inject(Router);
  
  const documentId = route.paramMap.get('id');
  if (!documentId) {
    router.navigate(['/documents']);
    return false;
  }
  
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
    }),
    catchError(() => {
      router.navigate(['/documents']);
      return of(false);
    })
  );
};