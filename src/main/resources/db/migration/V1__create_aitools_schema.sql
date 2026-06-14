-- V1: aitools-service schema
-- Stores customer AI tool configurations and usage snapshots.
-- Soft-references users and organizations from the shared costroomdb
-- (managed by auth-service / org-service) — no FK constraints so each
-- service can be deployed independently.

-- ── AI Tools ──────────────────────────────────────────────────────────────────
-- One row per AI tool a customer registers (OpenAI, Claude, Gemini, Copilot).
-- A single customer can register multiple tools (different providers or keys).
CREATE TABLE ai_tools (
    id              BINARY(16)    NOT NULL,
    user_id         BINARY(16)    NOT NULL  COMMENT 'Cognito sub from auth-service',
    org_id          BINARY(16)    NOT NULL  COMMENT 'organizations.id from org-service',
    ai_name         VARCHAR(100)  NOT NULL  COMMENT 'Provider slug: openai | claude | gemini | github_copilot',
    display_name    VARCHAR(255)            COMMENT 'Optional human-friendly label, e.g. "Production OpenAI key"',
    encrypted_key   VARCHAR(2048) NOT NULL  COMMENT 'AES-256-GCM encrypted API key',
    key_type        VARCHAR(50)   NOT NULL DEFAULT 'default' COMMENT 'admin | project | user | default',
    is_active       BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_ai_tools_user_id     ON ai_tools (user_id);
CREATE INDEX idx_ai_tools_org_id      ON ai_tools (org_id);
CREATE INDEX idx_ai_tools_org_ai_name ON ai_tools (org_id, ai_name);

-- ── AI Usage Snapshots ────────────────────────────────────────────────────────
-- Raw usage/cost snapshots fetched from provider APIs every 6 hours.
-- One row per (org, tool, provider, model, time-bucket, metric-type).
CREATE TABLE ai_usage_snapshots (
    id                  BINARY(16)      NOT NULL,
    org_id              BINARY(16)      NOT NULL  COMMENT 'Organization that owns these metrics',
    ai_tool_id          BINARY(16)      NOT NULL  COMMENT 'ai_tools.id that sourced this snapshot',
    provider            VARCHAR(100)    NOT NULL  COMMENT 'openai | claude | gemini | github_copilot',
    model_id            VARCHAR(255)              COMMENT 'Model identifier, e.g. gpt-4o, claude-3-5-sonnet',
    snapshot_type       VARCHAR(100)    NOT NULL  COMMENT 'completions | embeddings | cost | seats | etc.',
    source_type         VARCHAR(50)     NOT NULL  COMMENT 'usage | cost | seats',
    bucket_start_time   BIGINT          NOT NULL  COMMENT 'Unix epoch seconds — start of provider time bucket',
    -- Token/usage metrics (NULL for non-token providers like Copilot)
    input_tokens        BIGINT,
    output_tokens       BIGINT,
    input_cached_tokens BIGINT,
    total_requests      BIGINT,
    -- Cost in USD (precision 10 to handle micro-cent values from OpenAI)
    cost_usd            DECIMAL(18, 10),
    -- GitHub Copilot specific (seat-based billing)
    total_seats         INT,
    active_seats        INT,
    ingested_at         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_snapshots_org_bucket  ON ai_usage_snapshots (org_id, bucket_start_time);
CREATE INDEX idx_snapshots_tool_id     ON ai_usage_snapshots (ai_tool_id);
CREATE INDEX idx_snapshots_provider    ON ai_usage_snapshots (provider, model_id);
CREATE INDEX idx_snapshots_org_provider ON ai_usage_snapshots (org_id, provider, bucket_start_time);
