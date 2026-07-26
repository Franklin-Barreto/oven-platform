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

Verify the configuration before starting containers:

```bash
sudo docker compose --env-file .env config --quiet
```

Docker Compose stops with a `... is required` error when any required value is absent.

## Start and inspect

Start PostgreSQL and the application:

```bash
sudo docker compose --env-file .env up --detach postgres application
sudo docker compose --env-file .env ps
sudo docker compose --env-file .env logs --follow application
```

Liquibase applies pending migrations while the application starts. Verify readiness from the host:

```bash
curl --fail http://127.0.0.1:8080/actuator/health/readiness
```

The application and PostgreSQL ports are not published on a public interface. PostgreSQL is
reachable only on the private Compose network, and the application listens on the host loopback
interface for controlled validation and the future HTTPS proxy.

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
  http://127.0.0.1:8080/auth/login
```

Copy the `token` field from the response and verify an authenticated tenant request:

```bash
export TOKEN='<token from login response>'
curl --fail-with-body \
  --header "Authorization: Bearer $TOKEN" \
  --header 'X-API-Version: 1.0' \
  http://127.0.0.1:8080/users
```

## Routine operations

Inspect status and recent logs:

```bash
sudo docker compose --env-file .env ps
sudo docker compose --env-file .env logs --tail 200 application
sudo docker compose --env-file .env logs --tail 200 postgres
```

Stop containers without deleting the persistent PostgreSQL volume:

```bash
sudo docker compose --env-file .env down
```

Start them again:

```bash
sudo docker compose --env-file .env up --detach postgres application
```

Do not pass `--volumes` to `docker compose down`; that would delete the staging database volume.
