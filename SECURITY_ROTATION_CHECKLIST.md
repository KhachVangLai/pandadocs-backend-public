# Security Rotation Checklist

Use this checklist before publishing code or after discovering that sensitive material may have been committed in a private repository or PR history.

## Rotate Immediately

- Firebase service account / storage credentials
- Database credentials
- JWT secret
- Google OAuth client secret
- PayOS client ID / API key / checksum key
- Mail password
- Supabase keys if still active
- Gemini or other AI/API keys if ever committed

## Validate After Rotation

- old credentials no longer work
- secret managers contain the new values
- local `.env` files are updated
- CI/CD or Cloud Run secrets are updated
- deployment docs no longer show real values

## Public Repo Checks

- no `*.json` service account files are tracked
- no `*.backup` config files are tracked
- no real domains or callback URLs are hardcoded unless intentionally public
- no logs print secret values, lengths, or previews
- no screenshots expose keys, tokens, or internal dashboards

## Git History Reminder

Deleting a file in the latest commit does **not** remove it from old history.  
If the original private repo ever needs to become public, rewrite history first.  
For portfolio publication, prefer a fresh sanitized repo with clean history.
