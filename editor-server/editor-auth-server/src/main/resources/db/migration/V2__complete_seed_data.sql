-- ============================================
-- V2__complete_seed_data.sql
-- Complete seed data with consistent naming (underscore format)
-- ============================================

-- Insert default roles
INSERT INTO roles (id, name, description, created_at) VALUES
    (gen_random_uuid(), 'ROLE_USER', 'Standard user role with basic document permissions', NOW()),
    (gen_random_uuid(), 'ROLE_ADMIN', 'Administrator role with full system access', NOW()),
    (gen_random_uuid(), 'ROLE_MODERATOR', 'Moderator role with limited administrative access', NOW())
ON CONFLICT (name) DO NOTHING;

-- Insert default permissions (using consistent underscore format)
INSERT INTO permissions (id, name, description, resource, action, created_at) VALUES
    -- User permissions
    (gen_random_uuid(), 'USER_READ', 'Read user information', 'USER', 'READ', NOW()),
    (gen_random_uuid(), 'USER_WRITE', 'Update user information', 'USER', 'WRITE', NOW()),
    (gen_random_uuid(), 'USER_DELETE', 'Delete user account', 'USER', 'DELETE', NOW()),

    -- Document permissions
    (gen_random_uuid(), 'DOCUMENT_CREATE', 'Create new documents', 'DOCUMENT', 'CREATE', NOW()),
    (gen_random_uuid(), 'DOCUMENT_READ', 'Read/view documents', 'DOCUMENT', 'READ', NOW()),
    (gen_random_uuid(), 'DOCUMENT_WRITE', 'Edit documents', 'DOCUMENT', 'WRITE', NOW()),
    (gen_random_uuid(), 'DOCUMENT_DELETE', 'Delete documents', 'DOCUMENT', 'DELETE', NOW()),
    (gen_random_uuid(), 'DOCUMENT_LIST', 'List all documents', 'DOCUMENT', 'LIST', NOW()),
    (gen_random_uuid(), 'DOCUMENT_ACCESS', 'Access document load balancer', 'DOCUMENT', 'ACCESS', NOW()),

    -- Admin permissions
    (gen_random_uuid(), 'ADMIN_READ', 'Read administrative data', 'ADMIN', 'READ', NOW()),
    (gen_random_uuid(), 'ADMIN_WRITE', 'Write administrative data', 'ADMIN', 'WRITE', NOW()),
    (gen_random_uuid(), 'ADMIN_USERS_READ', 'View all users', 'ADMIN', 'USERS_READ', NOW()),
    (gen_random_uuid(), 'ADMIN_USERS_WRITE', 'Manage all users', 'ADMIN', 'USERS_WRITE', NOW()),
    (gen_random_uuid(), 'ADMIN_ROLES_MANAGE', 'Manage roles and permissions', 'ADMIN', 'ROLES_MANAGE', NOW()),
    (gen_random_uuid(), 'ADMIN_AUDIT_READ', 'View audit logs', 'ADMIN', 'AUDIT_READ', NOW())
ON CONFLICT (name) DO NOTHING;

-- Assign permissions to roles
-- ROLE_USER gets basic user and document permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ROLE_USER'
  AND p.name IN (
    'USER_READ', 'USER_WRITE', 'USER_DELETE',
    'DOCUMENT_CREATE', 'DOCUMENT_READ', 'DOCUMENT_WRITE',
    'DOCUMENT_DELETE', 'DOCUMENT_LIST', 'DOCUMENT_ACCESS'
  )
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ROLE_ADMIN gets all permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ROLE_ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ROLE_MODERATOR gets user permissions, document permissions, and limited admin permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ROLE_MODERATOR'
  AND p.name IN (
    'USER_READ', 'USER_WRITE',
    'DOCUMENT_READ', 'DOCUMENT_WRITE', 'DOCUMENT_LIST', 'DOCUMENT_ACCESS',
    'ADMIN_USERS_READ', 'ADMIN_AUDIT_READ'
  )
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Create a default admin user (password: Admin@123)
-- You should change this password immediately after first login
DO $$
DECLARE
    admin_user_id UUID := gen_random_uuid();
    admin_role_id UUID;
BEGIN
    -- Get admin role ID
    SELECT id INTO admin_role_id FROM roles WHERE name = 'ROLE_ADMIN';

    -- Insert admin user
    INSERT INTO users (id, username, email, password_hash, provider, enabled, email_verified, created_at, updated_at)
    VALUES (
        admin_user_id,
        'admin',
        'admin@authservice.com',
        '$argon2id$v=19$m=16384,t=2,p=1$VXNpZXBBTTRmclhmR05BZw$fBJWCRvq4rJXCGLqKBXUCw', -- Admin@123
        'LOCAL',
        true,
        true,
        NOW(),
        NOW()
    ) ON CONFLICT (id) DO NOTHING;

    -- Assign admin role to admin user
    INSERT INTO user_roles (user_id, role_id)
    VALUES (admin_user_id, admin_role_id)
    ON CONFLICT (user_id, role_id) DO NOTHING;
END $$;