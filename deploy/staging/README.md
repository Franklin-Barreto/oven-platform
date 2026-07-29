# AWS staging runtime

Unless a section says otherwise, run these commands from `deploy/staging` on the staging host.

## Publish the application image

The deployment workflow publishes the application automatically. For recovery or diagnostics,
publish an image manually from an authenticated workstation with the same script used by CI.

From the repository root:

```bash
AWS_PROFILE=oven-terraform-scoped \
  ./deploy/staging/publish-image.sh \
    us-east-1 \
    oven-platform \
    "$(git rev-parse HEAD)"
```

The script authenticates Docker to ECR, builds for `linux/amd64`, disables unsupported build
attestations, and publishes an immutable full-SHA tag. Repeating publication for an existing
compatible tag succeeds without trying to overwrite it.

Use the resulting immutable reference as `APPLICATION_IMAGE`:

```text
<account-id>.dkr.ecr.us-east-1.amazonaws.com/oven-platform:<git-sha>
```

## Authenticate the staging host

The EC2 instance role grants permission to pull from ECR, but Docker still requires registry
authentication.

On the staging host:

```bash
export AWS_REGION='us-east-1'
export ECR_REGISTRY='<account-id>.dkr.ecr.us-east-1.amazonaws.com'

aws ecr get-login-password \
  --region "$AWS_REGION" \
  | sudo docker login \
      --username AWS \
      --password-stdin "$ECR_REGISTRY"
```

Verify access before starting the composition:

```bash
sudo docker pull '<full immutable APPLICATION_IMAGE reference>'
```

ECR authorization tokens expire, so repeat the login before a later manual pull or deployment.

## Configure DuckDNS

Register a dedicated staging subdomain at DuckDNS. Keep the account token outside source control.

After the repository files are available under `/opt/oven-platform` on the staging host, create the
root-readable environment file:

```bash
sudo install -d -m 755 /etc/oven-platform

sudo install \
  -m 600 \
  -o root \
  -g root \
  /opt/oven-platform/deploy/staging/duckdns.env.example \
  /etc/oven-platform/duckdns.env

sudo vi /etc/oven-platform/duckdns.env
```

Configure the subdomain without the `.duckdns.org` suffix:

```dotenv
DUCKDNS_DOMAIN=oven-platform-staging
DUCKDNS_TOKEN='<DuckDNS account token>'
```

Install and validate the systemd units:

```bash
sudo install \
  -m 644 \
  /opt/oven-platform/deploy/staging/oven-platform-duckdns.service \
  /etc/systemd/system/oven-platform-duckdns.service

sudo install \
  -m 644 \
  /opt/oven-platform/deploy/staging/oven-platform-duckdns.timer \
  /etc/systemd/system/oven-platform-duckdns.timer

sudo systemd-analyze verify \
  /etc/systemd/system/oven-platform-duckdns.service \
  /etc/systemd/system/oven-platform-duckdns.timer

sudo systemctl daemon-reload
sudo systemctl enable --now oven-platform-duckdns.timer
sudo systemctl start oven-platform-duckdns.service
```

Verify the update:

```bash
sudo systemctl status \
  oven-platform-duckdns.service \
  oven-platform-duckdns.timer

sudo journalctl \
  --unit oven-platform-duckdns.service \
  --lines 50 \
  --no-pager

getent ahostsv4 oven-platform-staging.duckdns.org
```

From the workstation, confirm that the resolved IPv4 address matches the Terraform `public_ip`
output:

```bash
AWS_PROFILE=oven-terraform-scoped \
  terraform \
    -chdir=infra/terraform/staging \
    output -raw public_ip
```

Do not start Caddy with the public hostname until DNS resolves to the staging Elastic IP.

## Configure

Create a root-readable runtime environment file and keep it outside source control:

```bash
sudo install -m 600 -o root -g root .env.example .env
sudo openssl rand -base64 32
sudo openssl rand -base64 48
sudoedit .env
```

Put the first generated value in `POSTGRES_PASSWORD` and the second in `JWT_SECRET`. Set
`APPLICATION_IMAGE` to the full ECR image reference with an immutable tag, then fill the initial
owner values. Do not reuse development or production credentials.

Set `STAGING_HOSTNAME` to the complete DuckDNS hostname without a URL scheme. Set `ACME_EMAIL` to
the operational contact used for ACME certificate notifications:

```dotenv
STAGING_HOSTNAME=oven-platform-staging.duckdns.org
ACME_EMAIL='<operational email>'
```

Using a hostname without `http://` enables Caddy automatic HTTPS and HTTP-to-HTTPS redirects.

Configure the private S3 bucket and the public CloudFront base URL from the Terraform outputs:

```bash
AWS_PROFILE=oven-terraform-scoped \
  terraform -chdir=infra/terraform/staging output -raw media_bucket_name

AWS_PROFILE=oven-terraform-scoped \
  terraform -chdir=infra/terraform/staging output -raw media_public_base_url
```

Store the returned values in `.env`:

```dotenv
AWS_REGION=us-east-1
MEDIA_AWS_BUCKET='<media_bucket_name output>'
MEDIA_DELIVERY_BASE_URL='<media_public_base_url output>'
```

The application uses its EC2 instance role to authorize direct uploads to the private bucket.
Public image URLs resolve through CloudFront; no AWS credentials or presigned download URL is
exposed to clients.

Verify the configuration before starting containers:

```bash
sudo docker compose --env-file .env config --quiet
```

Docker Compose stops with a `... is required` error when any required value is absent.

## Start and inspect

Start PostgreSQL, the application, and Caddy:

```bash
sudo docker compose --env-file .env up --detach postgres application caddy
sudo docker compose --env-file .env ps
sudo docker compose --env-file .env logs --follow caddy
```

Liquibase applies pending migrations while the application starts. Verify readiness from the host:

```bash
curl --fail http://127.0.0.1:8080/actuator/health/readiness
```

The application and PostgreSQL ports are not published on a public interface. PostgreSQL is
reachable only on the private Compose network. The application host binding remains restricted to
loopback for controlled validation, while Caddy reaches `application:8080` through the private
Compose network.

Caddy obtains and renews the public certificate automatically. Verify HTTPS without bypassing
certificate validation:

```bash
export STAGING_HOSTNAME='oven-platform-staging.duckdns.org'

curl --fail-with-body \
  "https://${STAGING_HOSTNAME}/actuator/health/readiness"
```

Verify the HTTP redirect:

```bash
curl --head \
  "http://${STAGING_HOSTNAME}/actuator/health/readiness"
```

The HTTP response must redirect to the equivalent HTTPS URL.

Inspect the public certificate:

```bash
openssl s_client \
  -connect "${STAGING_HOSTNAME}:443" \
  -servername "$STAGING_HOSTNAME" \
  </dev/null 2>/dev/null \
  | openssl x509 \
      -noout \
      -subject \
      -issuer \
      -dates
```

The issuer must be publicly trusted and the certificate names must cover the DuckDNS hostname.
Never use `curl --insecure` or `openssl` options that disable certificate verification.

From a workstation outside AWS, verify that internal ports cannot be reached directly:

```bash
export STAGING_PUBLIC_IP="$(
  AWS_PROFILE=oven-terraform-scoped \
    terraform \
      -chdir=infra/terraform/staging \
      output -raw public_ip
)"

nc -vz -w 5 "$STAGING_PUBLIC_IP" 8080
nc -vz -w 5 "$STAGING_PUBLIC_IP" 5432
```

Both commands must time out or report that the connection was refused. A successful connection is
a deployment blocker.

Also confirm that the public proxy does not expose Actuator Prometheus anonymously:

```bash
curl \
  --output /dev/null \
  --silent \
  --write-out '%{http_code}\n' \
  "https://${STAGING_HOSTNAME}/actuator/prometheus"
```

The response must be `401`, `403`, or `404`, never `200`. The readiness endpoint is the only
Actuator endpoint intentionally used for unauthenticated availability checks.

## Provision the initial owner

Run the one-shot bootstrap after the application has completed its first startup:

```bash
sudo docker compose --env-file .env run --rm bootstrap-owner
```

The final log line reports the tenant and user IDs. Save the tenant ID for validation:

```bash
export TENANT_ID='<tenantId from the bootstrap log>'
```

Run the same bootstrap command again. Its final log line must report
`outcome=ALREADY_PROVISIONED`, confirming that provisioning is idempotent.

Store the returned tenant ID in the root-readable `.env` file. Automated smoke tests use this value
without sending it or the owner credentials to GitHub:

```dotenv
OVEN_STAGING_TENANT_ID='<tenantId from the bootstrap log>'
```

## Validate authentication

Login from the host with the owner values stored in `.env`:

```bash
curl --fail-with-body \
  --header 'Content-Type: application/json' \
  --data "{\"tenantId\":\"$TENANT_ID\",\"email\":\"<owner email>\",\"password\":\"<owner password>\"}" \
  "https://${STAGING_HOSTNAME}/auth/login"
```

Copy the `token` field from the response and verify an authenticated tenant request:

```bash
export TOKEN='<token from login response>'
curl --fail-with-body \
  --header "Authorization: Bearer $TOKEN" \
  --header 'X-API-Version: 1.0' \
  "https://${STAGING_HOSTNAME}/users"
```

## Automated deployment

The `Deploy AWS staging` GitHub Actions workflow runs for pushes to the protected `main` branch. It
may also be started explicitly with `workflow_dispatch` after the workflow exists on the default
branch.

The GitHub environment named `staging` must define:

```text
AWS_REGION
AWS_ROLE_ARN
```

`AWS_ROLE_ARN` identifies `oven-platform-staging-github-deploy`. GitHub obtains short-lived
credentials through OIDC; no AWS access key is stored in GitHub.

The deployment sequence is:

```text
check out the exact main commit without AWS credentials
→ run ./mvnw verify
→ assume the staging deployment role through OIDC
→ publish oven-platform:<full-git-sha>
→ resolve the single tagged staging instance
→ start it when stopped
→ wait for EC2 status checks and Session Manager
→ fetch and detach-checkout the exact commit on the host
→ pull the immutable image through the EC2 instance role
→ preserve the current approved image as the rollback candidate
→ record the new image as pending
→ update only APPLICATION_IMAGE in the host-owned .env
→ reconcile PostgreSQL, application, and Caddy
→ wait for local readiness
→ restart Caddy so the recreated application upstream is resolved again
→ verify public HTTPS health, login, and an authenticated API v1 request
→ promote the pending image to current
```

Deployments use the `aws-staging-deployment` concurrency group and never cancel an in-progress
deployment. A stopped instance is started automatically and remains running after a successful or
failed deployment so the environment and logs can be inspected. Stop it explicitly when testing is
finished.

Runtime secrets remain only in `/opt/oven-platform/deploy/staging/.env`. The workflow and SSM
output contain no database password, JWT secret, DuckDNS token, owner password, or access token.

The validation job has only `contents: read`. The separate deployment job receives
`id-token: write` only after validation succeeds. The workflow does not use the privileged
`workflow_run` trigger and never checks out pull-request or fork code with AWS credentials.

### Inspect deployment status

From a workstation with GitHub CLI access:

```bash
gh run list \
  --workflow deploy-staging.yml \
  --limit 10

gh run view <run-id>
gh run view <run-id> --log-failed
```

On the staging host:

```bash
cd /opt/oven-platform/deploy/staging

sudo sed -n \
  's/^APPLICATION_IMAGE=//p' \
  .env

sudo docker compose --env-file .env ps
sudo docker compose --env-file .env logs --tail 200 application
sudo ./smoke-test.sh

sudo cat .current-application-image
sudo cat .last-known-good-application-image
```

`.current-application-image` contains only an image that completed local readiness and the public
authenticated smoke tests. `.last-known-good-application-image` contains the approved image from
before the latest deployment attempt. A failed attempt remains in `.pending-application-image` for
diagnosis and never replaces either approved reference.

The application image tag is the deployed full Git SHA. The successful GitHub run summary also
records the commit, image, EC2 instance, and public endpoint.

### Manual dispatch

After this workflow is present on `main`, select **Actions → Deploy AWS staging → Run workflow**.
A manual dispatch deploys the current `main` commit and still executes the full validation,
immutable publication, readiness, and smoke-test sequence.

Pull requests never trigger this workflow and never receive AWS deployment credentials.

### Roll back the application

Before each attempt, the current approved image is preserved in the root-readable:

```text
/opt/oven-platform/deploy/staging/.last-known-good-application-image
```

The candidate is never derived from the image merely configured in `.env`. An image becomes
`.current-application-image` only after readiness and all public smoke tests succeed. Consecutive
failed deployments therefore cannot replace the known-good rollback reference.

Inspect the candidate without printing any runtime secret:

```bash
sudo cat \
  /opt/oven-platform/deploy/staging/.last-known-good-application-image
```

Run the deterministic rollback on the staging host:

```bash
cd /opt/oven-platform/deploy/staging

sudo ./rollback.sh us-east-1
```

Rollback reuses `deploy.sh`: it authenticates through the EC2 instance role, pulls the recorded
immutable image, updates only `APPLICATION_IMAGE`, waits for readiness, executes the public smoke
tests, and marks the rollback image as current only after they pass.

The ECR lifecycle policy retains the five newest images to bound storage while preserving recent
rollback candidates. Rollback changes application code only; it does not reverse Liquibase
migrations or restore PostgreSQL data. Do not roll back across an incompatible database migration.

### Common deployment failures

- **OIDC authentication fails:** verify the `staging` environment, `AWS_ROLE_ARN`, repository
  identity, and role trust policy.
- **Image publication is denied:** verify the inline `oven-platform-staging-deploy` policy and that
  the target repository is `oven-platform`.
- **An existing tag has an unsupported manifest:** the tag was not produced by
  `publish-image.sh`; immutable tags must not be overwritten.
- **No staging instance is found:** verify the Terraform state and the `project`, `environment`,
  and `managed-by` EC2 tags.
- **The instance cannot start:** inspect its EC2 state and the workflow's `Start staging host`
  step.
- **Session Manager remains offline:** verify the instance role, SSM agent, network egress, and EC2
  status checks.
- **The remote checkout fails:** verify outbound GitHub access and that Git is installed by the
  host bootstrap.
- **Readiness times out:** inspect application and PostgreSQL container status, application logs,
  Liquibase output, and available disk or memory.
- **HTTPS smoke tests fail:** verify DuckDNS resolution, Caddy logs, certificate status,
  `OVEN_STAGING_TENANT_ID`, and the host-owned owner credentials. The automated deploy restarts
  Caddy after recreating the application container and waits up to 60 seconds for public readiness,
  allowing Docker DNS to resolve the new upstream.
- **Rollback candidate is absent:** no prior automated full-SHA deployment has been recorded on the
  current host.

## Routine operations

Inspect status and recent logs:

```bash
sudo docker compose --env-file .env ps
sudo docker compose --env-file .env logs --tail 200 application
sudo docker compose --env-file .env logs --tail 200 postgres
sudo docker compose --env-file .env logs --tail 200 caddy
```

Validate and reload the Caddy configuration without restarting the application:

```bash
sudo docker compose \
  --env-file .env \
  exec caddy \
  caddy validate --config /etc/caddy/Caddyfile

sudo docker compose \
  --env-file .env \
  exec caddy \
  caddy reload --config /etc/caddy/Caddyfile
```

Stop containers without deleting the persistent PostgreSQL volume:

```bash
sudo docker compose --env-file .env down
```

Start them again:

```bash
sudo docker compose --env-file .env up --detach postgres application caddy
```

Do not pass `--volumes` to `docker compose down`; that would delete the staging database volume.

## DNS and certificate troubleshooting

Inspect the DuckDNS updater:

```bash
sudo systemctl status oven-platform-duckdns.timer
sudo journalctl --unit oven-platform-duckdns.service --since today --no-pager
```

Confirm DNS before investigating ACME:

```bash
dig +short "$STAGING_HOSTNAME" A
```

Inspect Caddy certificate and renewal activity:

```bash
sudo docker compose --env-file .env logs --tail 300 caddy
```

Certificate issuance requires:

- the hostname to resolve to the staging Elastic IP;
- public TCP ports 80 and 443 to reach Caddy;
- the host clock to be correct;
- outbound HTTPS access to the ACME provider;
- persistent `caddy-data` storage.

Do not delete the `caddy-data` volume during routine restarts. Caddy stores certificates, private
keys, and renewal state in that volume.
