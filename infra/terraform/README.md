# AWS staging guardrails

This directory contains the cost and access guardrails that must exist before Oven Platform
staging infrastructure is provisioned.

## Account baseline

- Account plan: AWS Free plan.
- Staging region: `us-east-1`.
- Initial promotional credits: USD 140.
- Recorded credit expiration: 2027-07-21.
- Required resource tags:
  - `project = oven-platform`
  - `environment = staging`
  - `managed-by = terraform`

Do not enable AWS Organizations, Control Tower, or an IAM Identity Center organization instance for
this account. During initial setup, AWS warned that creating an organization would upgrade the
account to pay-as-you-go pricing and expire the Free plan credits immediately.

The Free plan period and the promotional credit expiration are separate limits. Check the Billing
console before creating resources because the effective benefit ends when the first applicable
limit is reached.

## Cost controls

Terraform manages:

- a USD 5 monthly budget;
- actual-cost notifications at 50%, 85%, and 100%;
- a forecasted-cost notification at 100%;
- a service-level cost anomaly monitor;
- a daily anomaly subscription with USD 1 and 20% thresholds.

Free Tier usage alerts are enabled at account level in the Billing console. The notification email
is provided through the ignored local `terraform.tfvars`; it must not be committed.

Budgets and anomaly notifications are visibility controls, not hard spending limits. AWS billing
data and notifications may be delayed.

To inspect usage:

1. Open **Billing and Cost Management**.
2. Use **Bills** or **Cost Explorer** for current spend.
3. Use **Credits** to inspect promotional balance and expiration.
4. Use **Budgets** to inspect notification status.
5. Use **Cost Anomaly Detection** to inspect monitors, subscriptions, and detected anomalies.

Cost Explorer API requests may themselves have a small charge. Prefer the existing budget and
anomaly notifications instead of polling the API frequently.

## Human access

The root user is reserved for account recovery and privileged IAM changes. It has MFA and no access
keys.

Day-to-day access uses the `oven-platform-admin` IAM user with console login, MFA, and no long-lived
access keys. Local AWS CLI access is established with `aws login`:

```bash
aws login --profile oven-admin --region us-east-1
```

The local profiles form this chain:

```text
oven-admin
  -> oven-admin-process
  -> oven-terraform-scoped
  -> oven-platform-terraform-provisioner
```

`oven-admin-process` exposes the short-lived login session through `credential_process`.
`oven-terraform-scoped` assumes the provisioner role. These profiles are workstation configuration
and are never committed.

Confirm the active identity before running Terraform:

```bash
aws sts get-caller-identity \
  --profile oven-terraform-scoped \
  --query Arn \
  --output text
```

The ARN must contain:

```text
assumed-role/oven-platform-terraform-provisioner/
```

## Least-privilege boundary

The provisioner role has only the `oven-platform-terraform-guardrails` managed policy. It can:

- use the exact S3 state object and lock file;
- inspect the exact state bucket;
- manage the Oven Platform budget and anomaly configuration;
- manage the GitHub OIDC provider and staging deployment role;
- read its own role and managed policy.

It cannot update its own policy or attach broader permissions to itself. This deliberately prevents
self-escalation.

When a later issue requires a new AWS action:

1. Add the smallest action and resource scope to `provisioner-policy.tf`.
2. Format, validate, and review the Terraform plan.
3. As root, temporarily attach `AdministratorAccess` to the provisioner role.
4. Apply only the reviewed saved plan.
5. Immediately detach `AdministratorAccess`.
6. Start or configure a fresh assumed-role session and verify the change with `terraform plan`.

The temporary administrative attachment is a break-glass procedure, not the normal Terraform
workflow. Verify afterward that the role has only:

```text
oven-platform-terraform-guardrails
```

## GitHub Actions access

AWS trusts GitHub Actions through the `token.actions.githubusercontent.com` OIDC provider with the
`sts.amazonaws.com` audience. No AWS access key is stored in GitHub.

The `oven-platform-staging-github-deploy` role trust policy accepts only tokens whose subject is:

```text
repo:Franklin-Barreto/oven-platform:environment:staging
```

The GitHub `staging` environment permits deployments only from `main`. Its repository-visible
variables contain the AWS region and role ARN; neither value is a credential. Application secrets
must remain in AWS-managed runtime storage introduced by a later issue.

The deployment role starts with no AWS permissions. Permissions are added only when a deployment
issue demonstrates a concrete requirement.

To disable GitHub deployment access immediately:

1. Open **IAM > Roles > oven-platform-staging-github-deploy**.
2. Remove the GitHub OIDC federated principal from the role trust policy, or delete the role.
3. Remove or disable the GitHub `staging` environment until the incident is resolved.

Restore access through a reviewed Terraform change. Changing only GitHub configuration is not a
complete AWS-side revocation.

## Terraform state bootstrap

`bootstrap/` is an independent Terraform root that creates the S3 state bucket. Its state remains
local and ignored because a backend cannot store the state used to create itself.

The bucket has:

- S3-managed AES-256 encryption;
- versioning;
- public-access blocking;
- bucket-owner-enforced ownership;
- a TLS-only bucket policy;
- `prevent_destroy`.

Initialize the bootstrap root with a local ignored variables file:

```bash
cd infra/terraform/bootstrap
terraform init
terraform plan
```

The main root uses the bucket through an ignored `staging.backend.hcl`:

```bash
cd infra/terraform
terraform init -backend-config=staging.backend.hcl
```

Never commit:

- `terraform.tfvars`;
- `staging.backend.hcl`;
- Terraform state files or state lock files (`.tflock`);
- saved plan files;
- credentials or account identifiers.

Commit both `.terraform.lock.hcl` files so provider selections and checksums remain reproducible.

Do not use `terraform state pull` in shared logs because state may contain sensitive values.

## Routine validation

From `infra/terraform`:

```bash
terraform fmt -check -recursive
terraform validate
AWS_PROFILE=oven-terraform-scoped terraform plan
```

Validate the bootstrap root separately:

```bash
cd bootstrap
terraform validate
AWS_PROFILE=oven-terraform-scoped terraform plan
```

Both plans should report:

```text
No changes. Your infrastructure matches the configuration.
```
