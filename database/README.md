# Database Migrations

This directory contains SQL migration scripts for the PandaDocs API database.

## How to Apply Migrations

Since the project uses `spring.jpa.hibernate.ddl-auto=validate`, you need to manually run these migrations in Supabase.

### Option 1: Supabase Dashboard (Recommended)

1. Go to your Supabase project dashboard
2. Navigate to **SQL Editor**
3. Open the migration file (e.g., `V001__create_chat_tables.sql`)
4. Copy the SQL content
5. Paste into Supabase SQL Editor
6. Click **Run** to execute

### Option 2: psql Command Line

```bash
# Connect to Supabase PostgreSQL
psql "<your-supabase-connection-string>"

# Run the migration
\i database/migrations/V001__create_chat_tables.sql
```

### Option 3: Using SQL Client (DBeaver, pgAdmin, etc.)

1. Connect to your Supabase database using the connection string
2. Open the migration file
3. Execute the SQL

## Migration Files

| File | Description | Date |
|------|-------------|------|
| `V001__create_chat_tables.sql` | Create chat_conversations and chat_quota tables for AI chatbox feature | 2025-11-02 |

## Migration Naming Convention

- Format: `V{version}__{description}.sql`
- Example: `V001__create_chat_tables.sql`
- Version numbers are sequential (V001, V002, V003, etc.)

## Rollback

To rollback a migration, create a corresponding rollback script:

```sql
-- Example: V001__create_chat_tables_rollback.sql
DROP TABLE IF EXISTS chat_quota CASCADE;
DROP TABLE IF EXISTS chat_conversations CASCADE;
```

## Verification

After running a migration, verify the tables were created:

```sql
-- Check if tables exist
SELECT tablename FROM pg_tables WHERE schemaname = 'public' AND tablename LIKE 'chat_%';

-- Check table structure
\d chat_conversations
\d chat_quota
```

## Important Notes

- Always backup your database before running migrations
- Test migrations in a staging environment first
- Migrations are **NOT** automatically applied - you must run them manually
- The application will fail to start if the database schema doesn't match the JPA entities
