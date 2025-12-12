-- ============================================
-- V1__init_schema.sql
-- Location: src/main/resources/db/migration/V1__init_schema.sql
-- ============================================

-- Users table
CREATE TABLE users (
                       id UUID PRIMARY KEY,
                       username VARCHAR(50) UNIQUE NOT NULL,
                       email VARCHAR(255) UNIQUE NOT NULL,
                       password_hash TEXT,
                       provider VARCHAR(20) NOT NULL,
                       provider_id VARCHAR(255),
                       enabled BOOLEAN NOT NULL DEFAULT true,
                       email_verified BOOLEAN NOT NULL DEFAULT false,
                       failed_login_attempts INTEGER NOT NULL DEFAULT 0,
                       locked_until TIMESTAMP WITH TIME ZONE,
                       created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                       updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                       last_login_at TIMESTAMP WITH TIME ZONE,
                       deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_provider_provider_id ON users(provider, provider_id);

-- Roles table
CREATE TABLE roles (
                       id UUID PRIMARY KEY,
                       name VARCHAR(50) UNIQUE NOT NULL,
                       description TEXT,
                       created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX idx_roles_name ON roles(name);

-- Permissions table
CREATE TABLE permissions (
                             id UUID PRIMARY KEY,
                             name VARCHAR(100) UNIQUE NOT NULL,
                             description TEXT,
                             resource VARCHAR(50),
                             action VARCHAR(50),
                             created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX idx_permissions_name ON permissions(name);

-- User-Roles join table
CREATE TABLE user_roles (
                            user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                            role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
                            PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);

-- Role-Permissions join table
CREATE TABLE role_permissions (
                                  role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
                                  permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
                                  PRIMARY KEY (role_id, permission_id)
);

CREATE INDEX idx_role_permissions_role_id ON role_permissions(role_id);
CREATE INDEX idx_role_permissions_permission_id ON role_permissions(permission_id);

-- Refresh tokens table
CREATE TABLE refresh_tokens (
                                token VARCHAR(100) PRIMARY KEY,
                                user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                issued_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                revoked BOOLEAN NOT NULL DEFAULT false,
                                replaced_by VARCHAR(100),
                                device_info TEXT,
                                ip_address VARCHAR(45),
                                user_agent TEXT
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
CREATE INDEX idx_refresh_tokens_revoked ON refresh_tokens(revoked);

-- Audit logs table
CREATE TABLE audit_logs (
                            id UUID PRIMARY KEY,
                            event_type VARCHAR(50) NOT NULL,
                            user_id UUID,
                            username VARCHAR(50),
                            ip_address VARCHAR(45),
                            user_agent TEXT,
                            timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
                            metadata TEXT,
                            risk_level VARCHAR(20) NOT NULL,
                            success BOOLEAN NOT NULL
);

CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp);
CREATE INDEX idx_audit_logs_event_type ON audit_logs(event_type);
CREATE INDEX idx_audit_logs_risk_level ON audit_logs(risk_level);

