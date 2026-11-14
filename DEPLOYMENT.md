# NETRAVA Deployment Runbook

**Next-generation Visual & Investigative Intelligence Platform.**

This project is prepared for a Vercel frontend and a Railway Spring Boot service with Railway PostgreSQL. Deployment is not complete until a private GitHub repository and provider accounts are connected and the production checks below pass.

## 1. Connect the user-owned repository

An empty private GitHub repository named `NETRAVA` has been created under the `Sparsh0325` account.

The local `main` branch contains the project history and current NETRAVA changes. Review, stage, commit, and push the changes after completing the pre-upload checks.

The configured Git remote is:

https://github.com/Sparsh0325/NETRAVA.git

## 2. Deploy PostgreSQL and the backend on Railway

1. Create a Railway project and add a PostgreSQL service. Leave its database private; the backend connects over Railway's internal network.
2. Add a backend service from the GitHub repository. Railway detects the root `Dockerfile` and builds the Spring Boot image.
3. Add these backend variables in Railway's service Variables screen. Use Railway reference variables for PostgreSQL values; replace `Postgres` with the exact name of your database service.

   ```text
   DB_URL=jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}?sslmode=require
   DB_USERNAME=${{Postgres.PGUSER}}
   DB_PASSWORD=${{Postgres.PGPASSWORD}}
   FLYWAY_ENABLED=true
   JWT_SECRET=<random Base64 value from at least 32 random bytes>
   CORS_ALLOWED_ORIGINS=https://<your-vercel-domain>
   ```

   Railway sets `PORT` for the service. Do not expose PostgreSQL through a public TCP proxy for this app.
4. Optional: set `INTELLISHARE_AI_GEMINI_API_KEY` and `INTELLISHARE_AI_GEMINI_MODEL=gemini-3.8-flash` in the backend service only. The scanner sends only the user's prompt text to Google Gemini after consent; it does not send case records, photos, or uploaded files.
5. Wait for Flyway to apply migrations V1–V7 and confirm the backend health route returns success at `https://<your-railway-domain>/api/health`.

## 3. Deploy the frontend on Vercel

1. Import the same GitHub repository as a Vercel project.
2. Set the project Root Directory to `frontend`. Use the Vite defaults: install with `npm ci`, build with `npm run build`, and publish `dist`.
3. Add the build-time environment variable `VITE_API_BASE_URL=https://<your-railway-domain>/api`.
4. Deploy once, copy the production Vercel URL, then update Railway's `CORS_ALLOWED_ORIGINS` to that exact origin and redeploy the backend.

The SPA route rewrite is configured in `frontend/vercel.json`.

## 4. Provision the first administrator

Public registration always creates a `Read-Only` account. After registering the intended owner account, use Railway's authorized database query flow to promote only that account. Use a parameterized query (email as a bound value), and verify exactly one row was updated:

```sql
UPDATE users SET role = 'Administrator' WHERE email = ?;
```

There is no public role-change endpoint. Never allow a registration request to set an elevated role.

## 5. Verify before sharing the URL

- Open the deployed frontend and confirm login and registration work over HTTPS.
- Confirm a normal new registration is Read-Only, then validate the administrator account separately.
- Create a synthetic case, add evidence and a timeline event, search for the case, and verify dashboard/analytics counts.
- Import a synthetic CSV and confirm invalid rows fail without partial imports.
- Verify a permitted case share works, a denied share is rejected, and revocation removes access.
- Check the persisted audit feed/export and administrator hash-chain verification.
- If Gemini is configured, test an opted-in prompt, verify its response is labeled AI-generated, and review returned source links. Do not use real case data in prompts.
- Confirm the criminal registry uses synthetic records only. Face matching remains unavailable as a real identification feature.

Use synthetic data only in the public demo. Do not upload real case, personal, evidence, or biometric data. The hash chain helps detect changes, but it cannot prevent a database administrator from changing both records and hashes.

## Provider references

- [Railway PostgreSQL networking and variables](https://docs.railway.com/databases/postgresql)
- [Railway Dockerfile builds](https://docs.railway.com/builds/dockerfiles)
- [Vercel Vite deployment](https://vercel.com/docs/frameworks/frontend/vite)
- [Vercel build configuration](https://vercel.com/docs/builds/configure-a-build)
