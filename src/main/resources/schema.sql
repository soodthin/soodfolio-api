-- ===================================================
-- SoodFolio Database Schema (PostgreSQL)
-- Architecture: Hybrid Relational + JSONB, Soft Delete, UUID PK
-- ===================================================

-- 1. Bảng USERS (Tài khoản người dùng)
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    full_name VARCHAR(255),
    avatar_url TEXT,
    role VARCHAR(50) NOT NULL DEFAULT 'ROLE_USER',
    auth_provider VARCHAR(50) NOT NULL DEFAULT 'LOCAL', -- LOCAL, GITHUB, GOOGLE
    provider_id VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Bảng PORTFOLIOS (1-to-1 với User)
CREATE TABLE IF NOT EXISTS portfolios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    username VARCHAR(100) NOT NULL UNIQUE, -- vd: 'alex' -> soodfolio.vercel.app/alex
    title VARCHAR(255) NOT NULL,          -- vd: 'ALEX NGUYEN'
    tagline VARCHAR(255),                 -- vd: 'SENIOR FULLSTACK DEVELOPER'
    bio TEXT,                             -- Tiểu sử ngắn ở slide Hero
    status_text VARCHAR(100) DEFAULT 'AVAILABLE FOR WORK',
    resume_url TEXT,
    theme_mode VARCHAR(20) DEFAULT 'DARK',-- DARK, LIGHT
    show_experience BOOLEAN NOT NULL DEFAULT TRUE,
    show_projects BOOLEAN NOT NULL DEFAULT TRUE,
    show_skills BOOLEAN NOT NULL DEFAULT TRUE,
    show_education BOOLEAN NOT NULL DEFAULT TRUE,
    show_contact BOOLEAN NOT NULL DEFAULT TRUE,
    is_published BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Bảng SOCIAL_LINKS (Liên kết mạng xã hội của portfolio)
CREATE TABLE IF NOT EXISTS social_links (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    portfolio_id UUID NOT NULL REFERENCES portfolios(id) ON DELETE CASCADE,
    platform VARCHAR(50) NOT NULL, -- GITHUB, LINKEDIN, TWITTER, EMAIL
    url TEXT NOT NULL,
    label VARCHAR(100),            -- vd: '[ GitHub ]'
    order_index INT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. Bảng EXPERIENCES (Kinh nghiệm làm việc)
CREATE TABLE IF NOT EXISTS experiences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    portfolio_id UUID NOT NULL REFERENCES portfolios(id) ON DELETE CASCADE,
    role_title VARCHAR(255) NOT NULL,
    company_name VARCHAR(255) NOT NULL,
    company_url TEXT,
    location VARCHAR(255),
    period_text VARCHAR(100) NOT NULL,   -- vd: '2024 — PRESENT'
    description TEXT,
    highlights JSONB DEFAULT '[]'::jsonb,-- Các gạch đầu dòng STAR bullets
    skills_used JSONB DEFAULT '[]'::jsonb,-- Tags công nghệ dùng trong cty đó
    order_index INT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5. Bảng PROJECTS (Dự án hiển thị theo slide)
CREATE TABLE IF NOT EXISTS projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    portfolio_id UUID NOT NULL REFERENCES portfolios(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,        -- vd: 'FlexiConnect'
    subtitle VARCHAR(255),              -- vd: 'B2B SaaS Platform'
    role VARCHAR(100),                  -- vd: 'Lead Fullstack'
    period_text VARCHAR(100),           -- vd: '2024'
    description TEXT,
    highlights JSONB DEFAULT '[]'::jsonb,-- Gạch đầu dòng thành tựu
    tech_stack JSONB DEFAULT '[]'::jsonb,-- Tags: ['NEXT.JS', 'SPRING BOOT', 'POSTGRESQL']
    live_url TEXT,
    github_url TEXT,
    image_url TEXT,                     -- Lưu link ảnh Cloudinary
    order_index INT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 6. Bảng SKILLS (Kỹ năng chia theo nhóm)
CREATE TABLE IF NOT EXISTS skills (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    portfolio_id UUID NOT NULL REFERENCES portfolios(id) ON DELETE CASCADE,
    category VARCHAR(100) NOT NULL,     -- vd: 'FRONTEND', 'BACKEND', 'DATABASE'
    name VARCHAR(100) NOT NULL,         -- vd: 'TypeScript', 'Spring Boot'
    order_index INT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 7. Bảng EDUCATIONS (Học vấn & Chứng chỉ)
CREATE TABLE IF NOT EXISTS educations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    portfolio_id UUID NOT NULL REFERENCES portfolios(id) ON DELETE CASCADE,
    institution VARCHAR(255) NOT NULL,
    degree VARCHAR(255) NOT NULL,
    field_of_study VARCHAR(255),
    period_text VARCHAR(100) NOT NULL,
    highlights TEXT,
    order_index INT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 8. Bảng ANALYTICS_VIEWS (Theo dõi lượt xem slide)
CREATE TABLE IF NOT EXISTS analytics_views (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    portfolio_id UUID NOT NULL REFERENCES portfolios(id) ON DELETE CASCADE,
    slide_id VARCHAR(100),
    event_type VARCHAR(50) DEFAULT 'VIEW',
    duration_seconds INT DEFAULT 0,
    clicked_link TEXT,
    referrer TEXT,
    country VARCHAR(100),
    user_agent TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Chỉ mục tối ưu truy vấn (Indexes)
CREATE INDEX IF NOT EXISTS idx_portfolios_username ON portfolios(username) WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_projects_portfolio ON projects(portfolio_id, order_index) WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_experiences_portfolio ON experiences(portfolio_id, order_index) WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_skills_portfolio ON skills(portfolio_id, category, order_index) WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_educations_portfolio ON educations(portfolio_id, order_index) WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_social_links_portfolio ON social_links(portfolio_id, order_index) WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_analytics_portfolio ON analytics_views(portfolio_id, created_at);


-- 9. Bảng REFRESH_TOKENS (Lưu phiên đăng nhập đa thiết bị)
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL UNIQUE,
    expiry_date TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user ON refresh_tokens(user_id);
