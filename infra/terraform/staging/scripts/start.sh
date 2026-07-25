#!/usr/bin/env bash

set -euo pipefail

AWS_PROFILE_NAME="${AWS_PROFILE:-oven-terraform-scoped}"
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
TERRAFORM_DIR="$(dirname "$SCRIPT_DIR")"

INSTANCE_ID="$(
  AWS_PROFILE="$AWS_PROFILE_NAME" \
    terraform -chdir="$TERRAFORM_DIR" output -raw instance_id
)"

echo "Starting staging instance $INSTANCE_ID..."

AWS_PROFILE="$AWS_PROFILE_NAME" aws ec2 start-instances \
  --instance-ids "$INSTANCE_ID" \
  --output table

AWS_PROFILE="$AWS_PROFILE_NAME" aws ec2 wait instance-running \
  --instance-ids "$INSTANCE_ID"

AWS_PROFILE="$AWS_PROFILE_NAME" aws ec2 wait instance-status-ok \
  --instance-ids "$INSTANCE_ID"

echo "Staging instance started and healthy."
