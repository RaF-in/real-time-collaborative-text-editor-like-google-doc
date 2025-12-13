-- SQL script to verify role and permission assignments
-- Run this against the auth database to check if permissions are correctly assigned

-- Check all roles
SELECT
    r.name as role_name,
    r.description as role_description,
    COUNT(rp.permission_id) as permission_count
FROM roles r
LEFT JOIN role_permissions rp ON r.id = rp.role_id
GROUP BY r.id, r.name, r.description
ORDER BY r.name;

-- Check all permissions
SELECT
    p.name as permission_name,
    p.description as permission_description,
    p.resource,
    p.action
FROM permissions p
ORDER BY p.resource, p.action;

-- Check ROLE_USER permissions
SELECT
    p.name as permission_name,
    p.resource,
    p.action
FROM permissions p
JOIN role_permissions rp ON p.id = rp.permission_id
JOIN roles r ON rp.role_id = r.id
WHERE r.name = 'ROLE_USER'
ORDER BY p.resource, p.action;

-- Check ROLE_ADMIN permissions
SELECT
    p.name as permission_name,
    p.resource,
    p.action
FROM permissions p
JOIN role_permissions rp ON p.id = rp.permission_id
JOIN roles r ON rp.role_id = r.id
WHERE r.name = 'ROLE_ADMIN'
ORDER BY p.resource, p.action;

-- Check what permissions a specific user has
-- Replace 'testuser' with actual username
SELECT
    u.username,
    u.email,
    r.name as role,
    p.name as permission
FROM users u
JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON ur.role_id = r.id
JOIN role_permissions rp ON r.id = rp.role_id
JOIN permissions p ON rp.permission_id = p.id
WHERE u.username = 'admin'
ORDER BY r.name, p.resource, p.action;