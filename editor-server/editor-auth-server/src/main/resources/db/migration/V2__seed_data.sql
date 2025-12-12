-- ============================================
-- V2__seed_data.sql
-- Location: src/main/resources/db/migration/V2__seed_data.sql
-- ============================================

-- Insert default roles
INSERT INTO roles (id, name, description, created_at) VALUES
                                                          (gen_random_uuid(), 'ROLE_USER', 'Standard user role with basic permissions', NOW()),
                                                          (gen_random_uuid(), 'ROLE_ADMIN', 'Administrator role with full access', NOW()),
                                                          (gen_random_uuid(), 'ROLE_MODERATOR', 'Moderator role with limited administrative access', NOW());

-- Insert default permissions
INSERT INTO permissions (id, name, description, resource, action, created_at) VALUES
                                                                                  -- User permissions
                                                                                  (gen_random_uuid(), 'user:read', 'Read user information', 'user', 'read', NOW()),
                                                                                  (gen_random_uuid(), 'user:write', 'Update user information', 'user', 'write', NOW()),
                                                                                  (gen_random_uuid(), 'user:delete', 'Delete user account', 'user', 'delete', NOW()),

                                                                                  -- Admin permissions
                                                                                  (gen_random_uuid(), 'admin:users:read', 'View all users', 'admin', 'users:read', NOW()),
                                                                                  (gen_random_uuid(), 'admin:users:write', 'Manage all users', 'admin', 'users:write', NOW()),
                                                                                  (gen_random_uuid(), 'admin:roles:manage', 'Manage roles and permissions', 'admin', 'roles:manage', NOW()),
                                                                                  (gen_random_uuid(), 'admin:audit:read', 'View audit logs', 'admin', 'audit:read', NOW());

-- Assign permissions to roles
-- ROLE_USER gets basic user permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ROLE_USER'
  AND p.name IN ('user:read', 'user:write', 'user:delete');

-- ROLE_ADMIN gets all permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ROLE_ADMIN';

-- ROLE_MODERATOR gets user permissions + audit read
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ROLE_MODERATOR'
  AND p.name IN ('user:read', 'user:write', 'admin:users:read', 'admin:audit:read');

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
       );

-- Assign admin role to admin user
INSERT INTO user_roles (user_id, role_id) VALUES (admin_user_id, admin_role_id);
END $$;