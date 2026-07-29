# AWS staging host

This Terraform root provisions the minimum AWS infrastructure required to run Oven Platform in a
testable staging environment. It deliberately uses one EC2 host instead of production-scale
managed services to preserve the account's limited promotional credits.

The host is not the application deployment itself. Application, PostgreSQL, and reverse-proxy
containers are introduced by later delivery work.

## Architecture

Terraform manages:

- one dedicated VPC and public subnet;
- an internet gateway and public route;
- a security group that exposes only TCP ports 80 and 443;
- one `t3.small` Amazon Linux 2023 EC2 instance;
- one encrypted 20 GiB gp3 root volume;
- one stable Elastic IP;
- one immutable, scan-on-push ECR repository that retains the five newest images;
- one private S3 media bucket and a separate access-log bucket with 30-day retention;
- one CloudFront distribution with Origin Access Control for public media delivery;
- an EC2 instance role for Session Manager, ECR pull, and tenant-scoped media object access.

The environment has no SSH ingress, NAT Gateway, load balancer, RDS, or multi-AZ resources. EC2
Instance Metadata Service requires IMDSv2 tokens. Its response hop limit is two so the application
container can obtain short-lived instance-role credentials without storing AWS access keys.

The bootstrap installs Docker, enables the SSM agent, creates `/opt/oven-platform`, and configures
2 GiB of persistent swap.

## Prerequisites

- Terraform and AWS CLI installed locally.
- The Session Manager plugin installed locally.
- A valid `oven-admin` login session.
- The `oven-terraform-scoped` profile configured to assume the project provisioner role.
- The guardrails root already applied.
- An ignored `infra/terraform/staging.backend.hcl` containing the state bucket name.

Authenticate when the temporary login expires:

```bash
aws login --profile oven-admin --region us-east-1
```

Verify the scoped identity:

```bash
AWS_PROFILE=oven-terraform-scoped aws sts get-caller-identity \
  --query Arn \
  --output text
```

The returned ARN must contain:

```text
assumed-role/oven-platform-terraform-provisioner/
```

## Initialize and validate

From the repository root:

```bash
terraform -chdir=infra/terraform/staging init \
  -backend-config=../staging.backend.hcl

terraform -chdir=infra/terraform/staging fmt -check
terraform -chdir=infra/terraform/staging validate
```

Create and review a saved plan:

```bash
AWS_PROFILE=oven-terraform-scoped \
  terraform -chdir=infra/terraform/staging plan \
  -out=staging.tfplan

terraform -chdir=infra/terraform/staging show -no-color staging.tfplan
```

Saved plans capture the resolved AMI and the state at planning time. Generate a new plan if the
state changes or a plan is kept long enough for its inputs to become outdated.

Apply exactly the reviewed plan:

```bash
AWS_PROFILE=oven-terraform-scoped \
  terraform -chdir=infra/terraform/staging apply staging.tfplan
```

Saved plan files are ignored and must not be committed.

### Host upgrades

Routine plans do not replace the singleton EC2 host when AWS publishes a newer Amazon Linux AMI.
Changes to `user-data.sh` also update the instance metadata without forcing replacement because
user data is a creation-time bootstrap and is not rerun on an existing host.

When an operating-system or bootstrap upgrade requires rebuilding the host, review the replacement
explicitly:

```bash
AWS_PROFILE=oven-terraform-scoped \
  terraform -chdir=infra/terraform/staging plan \
  -replace=aws_instance.host \
  -out=staging-host-upgrade.tfplan
```

Replacing the host recreates its root volume and causes staging downtime. Preserve any required
host-local configuration before applying the reviewed plan. The Elastic IP remains managed
separately and is reassociated with the replacement instance.

## Routine operation

The helper scripts default to `oven-terraform-scoped`. Override it with `AWS_PROFILE` when needed.

Inspect the host:

```bash
infra/terraform/staging/scripts/status.sh
```

Start the host and wait for both EC2 health checks:

```bash
infra/terraform/staging/scripts/start.sh
```

Stop the host and wait for the final state:

```bash
infra/terraform/staging/scripts/stop.sh
```

Stopping preserves the host, root volume, stable address, ECR repository, and Terraform state. It
stops EC2 compute charges, but the EBS volume and public IPv4 address can continue to incur charges.
Use `terraform destroy` when no issue-scoped billable resources should remain.

## Session Manager access

Start the host before opening a session. Obtain the command without manually handling the instance
ID:

```bash
AWS_PROFILE=oven-terraform-scoped \
  terraform -chdir=infra/terraform/staging output -raw ssm_start_session_command
```

Run the printed command. On the host, useful bootstrap checks are:

```bash
sudo systemctl is-active docker amazon-ssm-agent
docker --version
swapon --show
test -d /opt/oven-platform && echo "directory prepared"
```

Leave the shell with `exit`. Confirm that no session remains active:

```bash
AWS_PROFILE=oven-terraform-scoped aws ssm describe-sessions \
  --state Active \
  --query 'Sessions[].SessionId' \
  --output text
```

An empty result means there are no active sessions.

## Drift check

After an apply or operational validation:

```bash
AWS_PROFILE=oven-terraform-scoped \
  terraform -chdir=infra/terraform/staging plan
```

The expected result is:

```text
No changes. Your infrastructure matches the configuration.
```

Starting or stopping the instance is an intentional operational state change and is not managed as
Terraform drift.

## Destroy and recreate

Review the destroy plan before applying it:

```bash
AWS_PROFILE=oven-terraform-scoped \
  terraform -chdir=infra/terraform/staging plan \
  -destroy \
  -out=staging-destroy.tfplan

terraform -chdir=infra/terraform/staging show -no-color staging-destroy.tfplan
```

Destroy exactly the reviewed resources:

```bash
AWS_PROFILE=oven-terraform-scoped \
  terraform -chdir=infra/terraform/staging apply staging-destroy.tfplan
```

Destroying this root removes the EC2 instance, root volume, Elastic IP, ECR repository, media and
access-log buckets, CloudFront distribution, and staging network. The remote state bucket, budgets,
access guardrails, GitHub OIDC configuration, and their separate Terraform state remain intact.

Because the ECR repository has `force_delete` enabled, destroying staging also deletes its stored
container images. The S3 buckets use `force_destroy`, so stored images and access logs are also
deleted. Data on the instance root volume is likewise deleted. Recreate the empty environment with
a newly reviewed normal plan and apply.

## Cost controls

- Stop the host whenever staging is not actively being tested.
- Remember that S3 storage, CloudFront requests, and the Elastic IP can still incur charges while
  the host is stopped.
- Destroy the staging root for longer idle periods or to verify complete reproducibility.
- Check Billing, Budgets, and promotional credit balance regularly.
- Treat budgets and anomaly alerts as delayed notifications, not hard spending limits.
- Do not add NAT Gateway, load balancer, RDS, or multi-AZ resources without a separate cost review.

The account-level cost and access procedures are documented in the parent
[`README.md`](../README.md).
