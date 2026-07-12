CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) DEFAULT 'USER',
    is_builtin_admin BIT(1) NOT NULL DEFAULT b'0',
    must_change_password BIT(1) NOT NULL DEFAULT b'0',
    last_login_at DATETIME(6) NULL,
    created_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE votes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    description TEXT NULL,
    creator_id BIGINT NOT NULL,
    start_time DATETIME(6) NULL,
    end_time DATETIME(6) NULL,
    vote_type VARCHAR(255) DEFAULT 'CHOICE',
    min_choices INTEGER DEFAULT 1,
    max_choices INTEGER DEFAULT 1,
    force_all_options BIT(1) DEFAULT b'0',
    allow_custom_options BIT(1) DEFAULT b'0',
    is_active BIT(1) DEFAULT b'1',
    created_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_votes PRIMARY KEY (id),
    CONSTRAINT fk_votes_creator FOREIGN KEY (creator_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_votes_creator_id ON votes (creator_id);

CREATE TABLE vote_options (
    id BIGINT NOT NULL AUTO_INCREMENT,
    vote_id BIGINT NOT NULL,
    option_text VARCHAR(200) NOT NULL,
    sort_order INTEGER DEFAULT 0,
    max_score INTEGER DEFAULT 100,
    creator_id BIGINT NULL,
    created_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_vote_options PRIMARY KEY (id),
    CONSTRAINT fk_vote_options_vote FOREIGN KEY (vote_id) REFERENCES votes (id),
    CONSTRAINT fk_vote_options_creator FOREIGN KEY (creator_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_vote_options_vote_id ON vote_options (vote_id);
CREATE INDEX idx_vote_options_creator_id ON vote_options (creator_id);

CREATE TABLE vote_invites (
    vote_id BIGINT NOT NULL,
    is_enabled BIT(1) NOT NULL DEFAULT b'0',
    code_version INTEGER NOT NULL DEFAULT 1,
    code_hash VARCHAR(64) NOT NULL,
    code_ciphertext TEXT NULL,
    expires_at DATETIME(6) NULL,
    max_members INTEGER NOT NULL DEFAULT 100,
    reset_at DATETIME(6) NULL,
    reset_by BIGINT NULL,
    created_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_vote_invites PRIMARY KEY (vote_id),
    CONSTRAINT uk_vote_invites_code_hash UNIQUE (code_hash),
    CONSTRAINT fk_vote_invites_vote FOREIGN KEY (vote_id) REFERENCES votes (id),
    CONSTRAINT fk_vote_invites_reset_by FOREIGN KEY (reset_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_vote_invites_reset_by ON vote_invites (reset_by);

CREATE TABLE vote_memberships (
    id BIGINT NOT NULL AUTO_INCREMENT,
    vote_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    joined_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    left_at DATETIME(6) NULL,
    joined_code_version INTEGER NULL,
    created_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_vote_memberships PRIMARY KEY (id),
    CONSTRAINT uk_vote_membership_vote_user UNIQUE (vote_id, user_id),
    CONSTRAINT fk_vote_memberships_vote FOREIGN KEY (vote_id) REFERENCES votes (id),
    CONSTRAINT fk_vote_memberships_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_vote_memberships_vote_id ON vote_memberships (vote_id);
CREATE INDEX idx_vote_memberships_user_id ON vote_memberships (user_id);

CREATE TABLE vote_records (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    vote_id BIGINT NOT NULL,
    option_id BIGINT NOT NULL,
    score INTEGER NULL,
    voted_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_vote_records PRIMARY KEY (id),
    CONSTRAINT uk_user_vote_option UNIQUE (user_id, vote_id, option_id),
    CONSTRAINT fk_vote_records_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_vote_records_vote FOREIGN KEY (vote_id) REFERENCES votes (id),
    CONSTRAINT fk_vote_records_option FOREIGN KEY (option_id) REFERENCES vote_options (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_vote_records_vote_id ON vote_records (vote_id);
CREATE INDEX idx_vote_records_option_id ON vote_records (option_id);

CREATE TABLE admin_audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    operator_id BIGINT NOT NULL,
    operator_role VARCHAR(20) NOT NULL,
    action VARCHAR(64) NOT NULL,
    target_type VARCHAR(64) NOT NULL,
    target_id VARCHAR(128) NULL,
    detail TEXT NULL,
    confirmed BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_admin_audit_logs PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_admin_audit_logs_operator_id ON admin_audit_logs (operator_id);
CREATE INDEX idx_admin_audit_logs_action ON admin_audit_logs (action);

CREATE TABLE vote_deletion_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    vote_id BIGINT NOT NULL,
    vote_title VARCHAR(200) NOT NULL,
    operator_id BIGINT NOT NULL,
    reason VARCHAR(255) NULL,
    deleted_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_vote_deletion_logs PRIMARY KEY (id),
    CONSTRAINT fk_vote_deletion_logs_operator FOREIGN KEY (operator_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_vote_deletion_logs_vote_id ON vote_deletion_logs (vote_id);
CREATE INDEX idx_vote_deletion_logs_operator_id ON vote_deletion_logs (operator_id);
