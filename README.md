# aitools-service

- Track your ai usage, tokens, costs and more
- check past and present data, analyze future prediction

---

- This is for
  - individual users
  - Mid sized
  - Enterprise companies

- This will help user by giving
  - Visibility
  - control
  - Action
  - Get insights
  - Integration
  - Relevant data or analytics, plan spend, usage

- This backend app will be
  - multi-user
  - analytics-heavy
  - scheduled-job-heavy
  - integration-heavy

- Technologies using currently
  - SpringBoot for main backend
  - FastAPI for analytics and future prediction
  - Different AI APIs - OpenAI API, Claude, Gemini, GitHub Copilot, and more
  - PostgreSQL TimescaleDB
  - AWS ECS for hosting

---

## Pipeline

```
OpenAI APIs
     ↓
Background Pollers
     ↓
Normalize + Aggregate
     ↓
Store Historical Snapshots
     ↓
Serve Fast Dashboard Queries
```

```
Cron Job
   ↓
Fetch OpenAI Usage
   ↓
Fetch Costs
   ↓
Normalize
   ↓
Store Snapshots
   ↓
Analytics API
   ↓
Dashboard
```

---

## MVP Architecture

- integrations
- analytics
- ingestion pipelines
- schedulers
- forecasting systems

---

- Core Features
  - Authentication
  - users
  - organizations
  - roles
  - API key ownership

- OpenAI Integration
  - project keys
  - admin keys
  - telemetry ingestion

- Analytics
  - usage
  - tokens
  - requests
  - costs
  - model analytics
  - trends

- Forecasting
  - future spend
  - token growth
  - anomaly detection

- Dashboard
  - realtime-ish charts
  - organization overview
  - project breakdown

---

### Base Structure

Base app structure com/example/aitracker

- auth
  - authentication logic
  - login
  - signup
  - refresh tokens
  - password hashing

- user
  - Manages user data

- organization
  - Example:
    - OpenAI startup team
    - freelance developer
    - AI agency
    - enterprise company

- Each organization:
  - has users
  - has API keys
  - has analytics

- project

- apikey
  - storing keys
  - encrypting keys
  - validating keys
  - key metadata

- openai
  - talks directly to OpenAI APIs
  - Example:
    - fetch usage
    - fetch costs
    - fetch analytics

- analytics
  - business intelligence layer
  - Example:
    - token trends
    - cost aggregation
    - charts
    - statistics

- forecasting
  - prediction engine
  - Example:
    - next month cost prediction
    - token growth prediction
    - anomaly detection

- ingestion
  - collecting data from OpenAI
  - Example:
    - pull usage data
    - normalize responses
    - save snapshots

- scheduler
  - Runs automated background jobs.
  - Example:
    - every 6 hours:
    - fetch latest analytics

- security

- common
  - Reusable/shared utilities.
  - Example:
    - exceptions
    - helpers
    - constants
    - utilities

- config
  - Application configuration.
  - Example:
    - WebClient config
    - OpenAI config
    - CORS config
    - Swagger config

## Each feature/module usually contains

- Controller
  - Receives HTTP requests.

- Service
  - Business logic.

- Repository
  - Database operations.

- Entity
  - Database table model.

- DTO
  - Request/response objects.

> ### Philosophy

- every module has ONE responsibility, not everything inside one controller

## Phase 1

- auth
- organization
- apikey
- openai
- ingestion

MVP backend foundation

1. User saves API key
2. System validates key
3. Background job fetches usage/costs
4. Store snapshots in DB
5. Dashboard reads from DB

MVP v1

- usage charts
- token tracking
- cost tracking
- model analytics

## Phase 2

- analytics
- forecasting

---

## Challenge

- time-series aggregation
- APIs are delayed
- data is paginated
- buckets can change
- costs update asynchronously

---

### OpenAI

I have two types of openai api key -

- sk-proj-...
- sk-admin-...

### Api key Architecture

Platform users/companies will:

- Create account
- Create organization
- Add:
  - sk-proj
  - sk-admin
- backend encrypts them
- Store them dynamically in the database
- Store encrypted keys in PostgreSQL
- Scheduler fetches usage/costs periodically

### Potential Analytics Dimensions

- Total tokens
- Input tokens
- Output tokens
- Cached tokens
- Cost
- Daily spend
- Weekly spend
- Monthly spend
- Requests count
- Model breakdown
- Usage trends
- Cost trends
- Growth rate
- Forecasting
- Burn rate
- Budget prediction
- Cost anomaly detection
- Usage anomaly detection
- Team/org analytics

### For MVP

- Total Cost
- Total Requests
- Total Tokens
- Input vs Output Tokens
- Daily Usage Graph
- Daily Cost Graph
- Model Breakdown
- 30-Day Forecast
- Budget Burn Rate
- Cost Anomaly Alerts
