# Documentation Guide

This folder contains curated project documentation for the public PandaDocs Backend snapshot.

## Start Here

- [setup.md](./setup.md): step-by-step local setup and runtime expectations
- [../README.md](../README.md): public project overview
- [../PROJECT_REPORT.md](../PROJECT_REPORT.md): detailed bilingual project report
- [../.env.example](../.env.example): environment variable reference list
- `../src/main/resources/application-local.example.properties`: local profile template to copy and keep untracked

## AI Chat Guides

- [ai-chat/setup.md](./ai-chat/setup.md): original AI chat setup guide
- [ai-chat/flow.md](./ai-chat/flow.md): chat flow and interaction design
- [ai-chat/frontend-integration.md](./ai-chat/frontend-integration.md): frontend integration contract
- [ai-chat/debug-guide.md](./ai-chat/debug-guide.md): debugging notes
- [ai-chat/rag-explanation.md](./ai-chat/rag-explanation.md): RAG explanation

## Reference Material

- [reference/password-requirements.md](./reference/password-requirements.md): password validation rules
- [../database/README.md](../database/README.md): database migration notes

## Archived / Historical Docs

- [archive/spring-starter-help.md](./archive/spring-starter-help.md): default Spring starter help file kept only for historical context

## Notes

- Some AI-chat documents are preserved from the original development period and may reflect the implementation state at that time.
- The public repo is intentionally a **portfolio snapshot**, so docs prioritize understanding the system over pretending every deployment path is currently maintained.
- The safest local workflow is: keep real values in local-only files or environment variables, never in tracked Markdown or Spring config backups.
