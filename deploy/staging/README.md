# AWS staging runtime

Run these commands from `deploy/staging` on the staging host.

## Publish the application image

Until automated deployment is implemented, publish the image manually from an authenticated
workstation.

From the repository root:

```bash
export AWS_REGION='us-east-1'
export ECR_REPOSITORY="$(
  terraform -chdir=infra/terraform/staging output -raw ecr_repository_url
)"
export ECR_REGISTRY="${ECR_REPOSITORY%%/*}"
export IMAGE_TAG="$(git rev-parse HEAD)"

aws ecr get-login-password \
  --profile oven-admin \
  --region "$AWS_REGION" \
  | docker login \
      --username AWS \
      --password-stdin "$ECR_REGISTRY"

docker build \
  --platform linux/amd64 \
  --tag "$ECR_REPOSITORY:$IMAGE_TAG" \
  .

docker push "$ECR_REPOSITORY:$IMAGE_TAG"
```

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
DUCKDNS_TOKEN=<DuckDNS account token>
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
ACME_EMAIL=<operational email>
```

Using a hostname without `http://` enables Caddy automatic HTTPS and HTTP-to-HTTPS redirects.

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
