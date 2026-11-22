# Idea Morgue API (starter)

This folder contains the secure-backend integration plan. The Android app must never contain a provider API key.

## Required production services
- Authentication and user profile
- Case/content catalogue with editorial review and source metadata
- AI orchestration endpoint with rate limits and audit logs
- Saved cases and learning progress sync
- Push notification scheduler with user-controlled quiet hours

## AI endpoint contract (planned)
`POST /v1/assistant/ask`
```json
{
  "question": "Explain the differential diagnosis of...",
  "specialty": "Emergency Medicine",
  "level": "intern",
  "case_id": "optional"
}
```
Response:
```json
{
  "answer": "Educational explanation...",
  "sources": [
    {"title": "Guideline title", "url": "https://...", "published": "YYYY"}
  ],
  "safety_note": "Verify against current local guidance."
}
```

Do not deploy clinical answers without source verification, privacy controls, monitoring, and a clinician review process. No patient-identifiable data should be sent to an AI provider.
