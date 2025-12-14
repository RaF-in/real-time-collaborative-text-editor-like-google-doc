# Document Sharing Implementation Guide

## Table of Contents
1. [Overview](#overview)
2. [Current Implementation](#current-implementation)
3. [Proposed Permission-Based Sharing](#proposed-permission-based-sharing)
4. [Implementation Phases](#implementation-phases)
5. [Code Examples](#code-examples)
6. [Database Schema](#database-schema)
7. [API Endpoints](#api-endpoints)
8. [Frontend Components](#frontend-components)
9. [Security Considerations](#security-considerations)

## Overview

This document outlines the complete implementation strategy for document sharing in the real-time collaborative text editor. The implementation is divided into two main parts:

1. **Current Implementation**: Basic URL sharing (already implemented)
2. **Proposed Implementation**: Permission-based sharing with granular access control

## Current Implementation

### What's Working

The basic URL sharing feature is currently implemented and allows users to:

1. Share document URLs directly
2. Redirect to login page if not authenticated
3. Automatically redirect to the document after successful authentication
4. Maintain document state across browser tabs via WebSocket

### Key Components

#### 1. Frontend Changes

**AuthService (`editor-client/src/app/core/services/auth.service.ts`)**
- `storeReturnUrl(url)`: Stores the intended destination URL in sessionStorage
- `isSafeRedirect(url)`: Validates that redirect URLs are internal only
- `handleAuthSuccess()`: Redirects to stored URL after authentication

**LoginComponent (`editor-client/src/app/features/auth/components/login/login.component.ts`)**
- Captures `returnUrl` from query parameters on component initialization
- Stores return URL when OAuth2 login is initiated

**OAuthCallbackComponent (`editor-client/src/app/features/auth/components/oauth2-callback/oauth-callback.component.ts`)**
- Handles return URL from OAuth2 state parameter
- Ensures redirect after OAuth2 authentication

**AuthGuard (`editor-client/src/app/core/guards/auth.guard.ts`)**
- Uses `state.url` to preserve full URL including query parameters and fragments
- Redirects unauthenticated users to login with `returnUrl` parameter

#### 2. Flow Diagram

```
User A shares URL: http://app.com/editor/doc-123
                 |
                 v
User B clicks link (not logged in)
                 |
                 v
AuthGuard redirects → /auth/login?returnUrl=/editor/doc-123
                 |
                 v
User B logs in → AuthService handles success
                 |
                 v
Redirects to → /editor/doc-123 (document loads)
```

## Proposed Permission-Based Sharing

### Architecture Overview

The permission-based sharing system will implement a role-based access control (RBAC) model specifically for documents, with the following key features:

1. **Document Ownership**: Every document has an owner with full control
2. **Permission Levels**: Owner, Editor, Viewer
3. **Sharing Mechanisms**: Direct user invitation, public links, email invitations
4. **Audit Trail**: Track all sharing and access activities

### Permission Model

#### Access Levels

```java
public enum AccessLevel {
    OWNER(0),      // Full control, can delete, manage permissions
    EDITOR(1),     // Can edit and share with others
    VIEWER(2),     // Read-only access
    PUBLIC(3);     // Unauthenticated access (future)

    private final int priority;

    AccessLevel(int priority) {
        this.priority = priority;
    }

    public int priority() {
        return priority;
    }

    public boolean canPerform(AccessLevel requiredLevel) {
        return this.priority <= requiredLevel.priority();
    }
}
```

#### Permission Hierarchy

- **Owner** > **Editor** > **Viewer**
- Each level inherits capabilities of lower levels
- Example: An Editor can do everything a Viewer can plus edit

## Implementation Phases

### Phase 1: Core Permission System

#### 1.1 Backend Models

**Document Entity**
```java
@Entity
@Table(name = "documents")
public class Document {
    @Id UUID id;
    String title;
    UUID ownerId;  // Document owner
    AccessLevel defaultAccessLevel = AccessLevel.PRIVATE;
    Instant createdAt;
    Instant updatedAt;
    Instant deletedAt;  // Soft delete
}
```

**DocumentPermission Entity**
```java
@Entity
@Table(name = "document_permissions")
public class DocumentPermission {
    @Id UUID id;
    @ManyToOne Document document;
    @ManyToOne User user;
    AccessLevel accessLevel;
    UUID grantedBy;  // Who granted this permission
    Instant grantedAt;
}
```

#### 1.2 Services

**DocumentAccessService**
```java
@Service
public class DocumentAccessService {
    // Check if user has access to document
    public boolean hasAccess(UUID userId, UUID docId, AccessLevel requiredLevel)

    // Grant permission to user
    public DocumentPermission grantPermission(UUID docId, UUID userId,
                                             AccessLevel level, UUID grantedBy)

    // Revoke permission
    public void revokePermission(UUID docId, UUID userId, UUID revokedBy)

    // Get all permissions for a document
    public List<DocumentPermission> getDocumentPermissions(UUID docId, UUID requestingUserId)
}
```

#### 1.3 Security Integration

**Custom Security Expression**
```java
@Component("documentSecurity")
public class DocumentSecurityExpression {
    public boolean hasAccess(String documentId, String level) {
        // Implementation for @PreAuthorize annotations
    }
}
```

### Phase 2: Public Link Sharing

#### 2.1 PublicLink Entity

```java
@Entity
public class PublicLink {
    @Id UUID id;
    @ManyToOne Document document;
    String linkToken;  // Unique token for URL
    AccessLevel accessLevel;  // Viewer or Editor via link
    Instant expiresAt;  // Optional expiration
    UUID createdBy;
    Instant createdAt;
    boolean active;
}
```

#### 2.2 PublicLinkService

```java
@Service
public class PublicLinkService {
    public String generatePublicLink(UUID documentId, AccessLevel accessLevel,
                                   UUID createdBy, Integer expiresInDays)

    public boolean validatePublicLink(String token, UUID documentId)

    public void revokePublicLink(String token)

    public List<PublicLink> getActiveLinks(UUID documentId)
}
```

#### 2.3 Public Link Flow

```
Document Owner clicks "Share Public Link"
                        |
                        v
System generates token: abc123def456
                        |
                        v
Creates URL: https://app.com/shared/abc123def456
                        |
                        v
Anyone with link can access (no login required for Viewer access)
```

### Phase 3: Email Invitations

#### 3.1 Invitation Entity

```java
@Entity
public class Invitation {
    @Id UUID id;
    @ManyToOne Document document;
    String inviteeEmail;
    UUID inviterId;
    AccessLevel accessLevel;
    String invitationToken;
    Instant expiresAt;
    Instant acceptedAt;
    boolean accepted;
}
```

#### 3.2 Email Service

```java
@Service
public class EmailService {
    public void sendInvitationEmail(String toEmail, Document document,
                                   String inviteUrl, User inviter)

    public void sendAccessGrantedEmail(String toEmail, Document document,
                                      AccessLevel level, User granter)

    public void sendAccessRevokedEmail(String toEmail, Document document, User revoker)
}
```

#### 3.3 Invitation Flow

```
Owner invites external@company.com
            |
            v
Create Invitation entity with unique token
            |
            v
Send email: https://app.com/invite/xyz789
            |
            v
Recipient clicks link
            |
            v
If registered → Direct access
If not registered → Register → Then access
```

### Phase 4: Frontend Implementation

#### 4.1 Share Dialog Component

**Component Structure**
```typescript
// share-dialog.component.ts
@Component({
  selector: 'app-share-dialog',
  template: `
    <div class="share-dialog">
      <!-- Share via Email -->
      <section class="share-section">
        <h3>Invite by Email</h3>
        <input [(ngModel)]="inviteeEmail" placeholder="Enter email">
        <select [(ngModel)]="selectedAccessLevel">
          <option value="VIEWER">Can view</option>
          <option value="EDITOR">Can edit</option>
        </select>
        <button (click)="inviteUser()">Send Invitation</button>
      </section>

      <!-- Public Link Sharing -->
      <section class="share-section">
        <h3>Public Link</h3>
        <div *ngIf="publicLink">
          <input [value]="publicLink" readonly>
          <button (click)="copyLink()">Copy</button>
          <button (click)="revokeLink()">Revoke</button>
        </div>
        <div *ngIf="!publicLink">
          <button (click)="generatePublicLink()">Create Public Link</button>
        </div>
      </section>

      <!-- Current Collaborators -->
      <section class="share-section">
        <h3>People with Access</h3>
        <div *ngFor="let permission of permissions">
          <span>{{ permission.user.email }}</span>
          <span>{{ permission.accessLevel }}</span>
          <button (click)="removePermission(permission.id)">Remove</button>
        </div>
      </section>
    </div>
  `
})
export class ShareDialogComponent {
  permissions: DocumentPermissionDTO[] = [];
  publicLink: string | null = null;

  constructor(
    private documentShareService: DocumentShareService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit() {
    this.loadPermissions();
  }
}
```

#### 4.2 Document Share Service

```typescript
@Injectable({
  providedIn: 'root'
})
export class DocumentShareService {
  constructor(private http: HttpClient) {}

  inviteUser(documentId: string, email: string, accessLevel: string): Observable<any> {
    return this.http.post(`${this.API_URL}/documents/${documentId}/invite`, {
      email,
      accessLevel
    });
  }

  generatePublicLink(documentId: string, accessLevel: string): Observable<any> {
    return this.http.post(`${this.API_URL}/documents/${documentId}/public-link`, {
      accessLevel
    });
  }

  getPermissions(documentId: string): Observable<DocumentPermissionDTO[]> {
    return this.http.get<DocumentPermissionDTO[]>(`${this.API_URL}/documents/${documentId}/permissions`);
  }

  revokePermission(documentId: string, userId: string): Observable<any> {
    return this.http.delete(`${this.API_URL}/documents/${documentId}/permissions/${userId}`);
  }
}
```

#### 4.3 Routes for Special Access

```typescript
// app.routes.ts - Add new routes
{
  path: 'shared/:token',
  component: PublicDocumentComponent,
  resolve: {
    document: publicDocumentResolver
  }
},
{
  path: 'invite/:token',
  component: InvitationHandlerComponent,
  canActivate: [authGuard]
}
```

### Phase 5: Advanced Features

#### 5.1 Team-Based Permissions

```java
@Entity
public class Team {
    @Id UUID id;
    String name;
    UUID ownerId;
    List<User> members = new ArrayList<>();
}

@Entity
public class TeamDocumentPermission {
    @Id UUID id;
    @ManyToOne Team team;
    @ManyToOne Document document;
    AccessLevel accessLevel;
    UUID grantedBy;
}
```

#### 5.2 Access Logging

```java
@Entity
public class DocumentAccessLog {
    @Id UUID id;
    UUID documentId;
    UUID userId;  // null for public link access
    String action;  // VIEW, EDIT, SHARE, REVOKE
    String ipAddress;
    String userAgent;
    Instant timestamp;
    Map<String, Object> metadata;  // JSON field for additional data
}
```

#### 5.3 Rate Limiting

```java
@Component
public class ShareRateLimiter {
    private final RateLimiter emailInviteLimiter = RateLimiter.create(5.0);  // 5 per minute
    private final RateLimiter publicLinkLimiter = RateLimiter.create(2.0);   // 2 per minute

    public boolean canSendInvitation() {
        return emailInviteLimiter.tryAcquire();
    }

    public boolean canCreatePublicLink() {
        return publicLinkLimiter.tryAcquire();
    }
}
```

## Database Schema

### Complete Schema

```sql
-- Documents table
CREATE TABLE documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(500) NOT NULL,
    owner_id UUID NOT NULL REFERENCES users(id),
    default_access_level VARCHAR(20) DEFAULT 'PRIVATE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- Document permissions table
CREATE TABLE document_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    access_level VARCHAR(20) NOT NULL CHECK (access_level IN ('OWNER', 'EDITOR', 'VIEWER')),
    granted_by UUID REFERENCES users(id),
    granted_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(document_id, user_id)
);

-- Public links table
CREATE TABLE public_links (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    link_token VARCHAR(255) UNIQUE NOT NULL,
    access_level VARCHAR(20) NOT NULL CHECK (access_level IN ('VIEWER', 'EDITOR')),
    expires_at TIMESTAMP WITH TIME ZONE,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    active BOOLEAN DEFAULT true
);

-- Invitations table
CREATE TABLE invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    invitee_email VARCHAR(255) NOT NULL,
    inviter_id UUID NOT NULL REFERENCES users(id),
    access_level VARCHAR(20) NOT NULL CHECK (access_level IN ('EDITOR', 'VIEWER')),
    invitation_token VARCHAR(255) UNIQUE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    accepted_at TIMESTAMP WITH TIME ZONE,
    accepted BOOLEAN DEFAULT false
);

-- Teams table (for future implementation)
CREATE TABLE teams (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    owner_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Team members table
CREATE TABLE team_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(team_id, user_id)
);

-- Team document permissions table
CREATE TABLE team_document_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    access_level VARCHAR(20) NOT NULL CHECK (access_level IN ('EDITOR', 'VIEWER')),
    granted_by UUID NOT NULL REFERENCES users(id),
    granted_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(team_id, document_id)
);

-- Access logs table
CREATE TABLE document_access_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES documents(id),
    user_id UUID REFERENCES users(id),  -- nullable for public access
    action VARCHAR(50) NOT NULL,
    ip_address INET,
    user_agent TEXT,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    metadata JSONB
);
```

### Indexes

```sql
-- Performance indexes
CREATE INDEX idx_documents_owner ON documents(owner_id);
CREATE INDEX idx_documents_created ON documents(created_at);
CREATE INDEX idx_doc_permissions_document ON document_permissions(document_id);
CREATE INDEX idx_doc_permissions_user ON document_permissions(user_id);
CREATE INDEX idx_public_links_token ON public_links(link_token);
CREATE INDEX idx_public_links_document ON public_links(document_id);
CREATE INDEX idx_invitations_token ON invitations(invitation_token);
CREATE INDEX idx_invitations_email ON invitations(invitee_email);
CREATE INDEX idx_access_logs_document ON document_access_logs(document_id);
CREATE INDEX idx_access_logs_timestamp ON document_access_logs(timestamp);
```

## API Endpoints

### Authentication Endpoints
- `GET /api/documents/{id}/access` - Check if current user has access
- `POST /api/documents/{id}/access-token` - Generate access token for embedded views

### Permission Management
- `GET /api/documents/{id}/permissions` - List all permissions for a document
- `POST /api/documents/{id}/permissions` - Grant permission to user
- `PUT /api/documents/{id}/permissions/{userId}` - Update user's permission level
- `DELETE /api/documents/{id}/permissions/{userId}` - Revoke user's access

### Public Link Sharing
- `POST /api/documents/{id}/public-links` - Create new public link
- `GET /api/documents/{id}/public-links` - List active public links
- `DELETE /api/documents/{id}/public-links/{linkId}` - Revoke public link
- `GET /shared/{token}` - Access document via public link

### Email Invitations
- `POST /api/documents/{id}/invitations` - Send email invitation
- `GET /api/documents/{id}/invitations` - List pending invitations
- `POST /invitations/{token}/accept` - Accept invitation
- `DELETE /invitations/{token}` - Cancel/decline invitation

### Team Sharing (Future)
- `POST /api/teams` - Create new team
- `POST /api/teams/{id}/members` - Add member to team
- `POST /api/teams/{id}/documents/{docId}` - Share document with team
- `GET /api/documents/{id}/team-permissions` - List team permissions

## Frontend Components

### ShareDialog Component
- Location: `editor-client/src/app/features/editor/components/share-dialog/`
- Responsibilities:
  - Invite users via email
  - Generate public links
  - Manage existing permissions
  - Display current collaborators

### PublicDocument Component
- Location: `editor-client/src/app/features/public-document/`
- Responsibilities:
  - Render document in read-only mode
  - Show access level indicator
  - Prompt to sign up/register for full access

### InvitationHandler Component
- Location: `editor-client/src/app/features/invitation/`
- Responsibilities:
  - Handle invitation acceptance
  - Auto-login after registration
  - Redirect to shared document

### PermissionBadge Component
- Location: `editor-client/src/app/shared/components/permission-badge/`
- Responsibilities:
  - Display user's current permission level
  - Show owner/editor/viewer badges
  - Consistent UI across the app

## Security Considerations

### Authentication & Authorization
1. **JWT Token Security**
   - Short-lived access tokens (15 minutes)
   - Refresh tokens with secure, httpOnly cookies
   - Include permissions in JWT claims for better performance

2. **Permission Validation**
   - Always validate permissions on the server
   - Never trust client-side permission checks
   - Implement rate limiting for permission changes

3. **Public Link Security**
   - Use cryptographically secure random tokens
   - Set reasonable expiration dates
   - Track link usage for abuse detection

### Input Validation
```java
// Example of secure validation
@RestController
public class DocumentController {

    @PostMapping("/{id}/permissions")
    public ResponseEntity<?> grantPermission(
            @PathVariable UUID id,
            @Valid @RequestBody ShareRequestDTO request,
            Authentication auth) {

        // Validate document ownership
        if (!documentService.isOwner(id, getCurrentUserId(auth))) {
            throw new AccessDeniedException("Only owners can grant permissions");
        }

        // Validate email format
        if (!EmailValidator.getInstance().isValid(request.getEmail())) {
            throw new IllegalArgumentException("Invalid email address");
        }

        // Check rate limit
        if (!rateLimiter.canSendInvitation()) {
            throw new TooManyRequestsException("Too many invitations sent");
        }

        // Proceed with granting permission
        return ResponseEntity.ok(documentService.grantPermission(id, request));
    }
}
```

### CORS and CSP
```yaml
# application.yml
cors:
  allowed-origins:
    - https://app.yourdomain.com
    - https://yourdomain.com
  allowed-methods: GET,POST,PUT,DELETE,OPTIONS
  allow-credentials: true

spring:
  security:
    headers:
      content-security-policy: "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline';"
```

### Audit Trail Implementation
```java
@Aspect
@Component
public class DocumentAuditAspect {

    @AfterReturning(
        pointcut = "execution(* com.mmtext.authserver.service.DocumentAccessService.grantPermission(..))",
        returning = "permission"
    )
    public void auditPermissionGrant(JoinPoint jp, DocumentPermission permission) {
        AccessLog log = AccessLog.builder()
            .documentId(permission.getDocument().getId())
            .userId(permission.getGrantedBy())
            .action("GRANT_PERMISSION")
            .metadata(Map.of(
                "granteeId", permission.getUser().getId(),
                "accessLevel", permission.getAccessLevel()
            ))
            .build();

        accessLogService.save(log);
    }
}
```

## Performance Considerations

### Caching Strategy
```java
@Service
public class DocumentAccessService {

    @Cacheable(value = "document-access", key = "{#userId, #documentId}")
    public boolean hasAccess(UUID userId, UUID documentId, AccessLevel requiredLevel) {
        // Implementation
    }

    @CacheEvict(value = "document-access", key = "{#userId, #documentId}")
    public void revokePermission(UUID documentId, UUID userId, UUID revokedBy) {
        // Implementation
    }
}
```

### Database Optimization
1. **Connection Pooling**
   - Configure appropriate pool size based on load
   - Use HikariCP for optimal performance

2. **Query Optimization**
   - Use composite indexes for common queries
   - Implement pagination for permission lists
   - Consider read replicas for heavy read operations

3. **Batch Operations**
   ```java
   // Batch permission grants for better performance
   public void batchGrantPermissions(UUID documentId, List<BatchPermissionRequest> requests) {
       List<DocumentPermission> permissions = requests.stream()
           .map(req -> new DocumentPermission(...))
           .collect(Collectors.toList());

       documentPermissionRepository.saveAll(permissions);
   }
   ```

## Testing Strategy

### Unit Tests
```java
@ExtendWith(MockitoExtension.class)
class DocumentAccessServiceTest {

    @Mock
    private DocumentPermissionRepository permissionRepo;

    @InjectMocks
    private DocumentAccessService service;

    @Test
    void shouldGrantAccessToDocumentOwner() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        // When
        boolean hasAccess = service.hasAccess(userId, documentId, AccessLevel.EDITOR);

        // Then
        assertTrue(hasAccess);
    }

    @Test
    void shouldDenyAccessWithoutPermission() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        // Mock no permissions found
        when(permissionRepo.findByDocumentIdAndUserId(documentId, userId))
            .thenReturn(Optional.empty());

        // When
        boolean hasAccess = service.hasAccess(userId, documentId, AccessLevel.VIEWER);

        // Then
        assertFalse(hasAccess);
    }
}
```

### Integration Tests
```java
@SpringBootTest
@AutoConfigureTestDatabase
class DocumentControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldGrantPermissionToUser() {
        // Given
        String documentId = "doc-123";
        ShareRequestDTO request = new ShareRequestDTO("user-456", "VIEWER");

        // When
        ResponseEntity<DocumentPermissionDTO> response = restTemplate.postForEntity(
            "/api/documents/" + documentId + "/permissions",
            request,
            DocumentPermissionDTO.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("VIEWER", response.getBody().getAccessLevel());
    }
}
```

## Deployment Considerations

### Environment Variables
```bash
# Auth Server
AUTH_SERVER_JWT_SECRET=${JWT_SECRET}
AUTH_SERVER_DB_URL=${DATABASE_URL}
AUTH_SERVER_EMAIL_HOST=${SMTP_HOST}
AUTH_SERVER_EMAIL_PORT=${SMTP_PORT}

# Editor Server
EDITOR_SERVER_ID=${SERVER_ID}
EDITOR_AUTH_SERVER_URL=${AUTH_SERVER_URL}

# Client
APP_API_URL=${API_BASE_URL}
APP_ENVIRONMENT=${ENVIRONMENT}
```

### Docker Configuration
```dockerfile
# Auth Server Dockerfile
FROM openjdk:17-jdk-slim
COPY target/auth-server.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

```yaml
# docker-compose.yml
version: '3.8'
services:
  auth-server:
    build: ./editor-auth-server
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DATABASE_URL=jdbc:postgresql://postgres:5432/auth_db
    depends_on:
      - postgres

  editor-server:
    build: ./editor-server-main
    environment:
      - AUTH_SERVER_URL=http://auth-server:8081
    depends_on:
      - auth-server
```

## Monitoring and Metrics

### Key Metrics to Track
1. **Document Access**
   - Number of unique users per document
   - Peak concurrent users
   - Average session duration

2. **Sharing Activity**
   - Number of shares per day
   - Most shared documents
   - Permission change frequency

3. **Performance**
   - Permission check latency
   - Cache hit/miss ratio
   - Database query performance

### Implementation Example
```java
@Component
public class DocumentMetrics {
    private final MeterRegistry meterRegistry;
    private final Counter shareCounter;
    private final Timer accessTimer;

    public DocumentMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.shareCounter = Counter.builder("document.shares")
            .description("Number of document shares")
            .register(meterRegistry);
        this.accessTimer = Timer.builder("document.access.check")
            .description("Time taken to check document access")
            .register(meterRegistry);
    }

    public void recordShare() {
        shareCounter.increment();
    }

    public Timer.Sample startAccessTimer() {
        return Timer.start(meterRegistry);
    }
}
```

## Migration Strategy

### Phase 1 Migration (Basic Permissions)
1. Deploy database migration V3
2. Deploy backend services with permission checks
3. Maintain backward compatibility - existing documents remain accessible
4. Gradually enable permission features

### Phase 2 Migration (Full Features)
1. Deploy public link functionality
2. Deploy email invitation system
3. Update frontend with sharing UI
4. Enable all features with feature flags

### Rollback Plan
- Keep backup of database before migration
- Implement feature flags for quick rollback
- Monitor error rates during deployment
- Prepare rollback scripts for database changes

## Conclusion

This implementation plan provides a comprehensive solution for document sharing with:

1. **Current Implementation**: Working URL sharing with authentication redirect
2. **Future Enhancement Plans**: Complete permission-based sharing system
3. **Production-Ready Features**: Security, performance, monitoring, and scalability

The implementation follows industry best practices and can be deployed incrementally based on business priorities.