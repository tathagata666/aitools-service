
so the whole structure will look like this:
**React Frontend**
**auth-service**
:8081 (exists)
- Cognito JWT
- User create
- /api/me
**org-service**
:8082 (exists)
- Org CRUD
- Onboarding
**aitools-service**
:8083  (just created)
- API key storage (encrypted)
- Tool configuration
- Snapshot storage (raw data)
- @Scheduled fetch every 6h
**aianalytics-service**
:8084  FastAPI + Python
- Aggregations (daily/weekly/monthly)
- Forecasting
- Anomaly detection
- Cost prediction
- Burn rate / budget alerts
---

# Together these ai tools analytics will

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

---

## Example of ai tool

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


Current goal: For now aitools-service and aianalytics-service only provide tracking for openai, claude, gemini, github copilot, these 4 ai tools. Later as user demands, I will integrate new ai tools to track.
