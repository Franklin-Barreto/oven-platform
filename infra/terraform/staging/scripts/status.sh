#!/usr/bin/env bash

set -euo pipefail

AWS_PROFILE_NAME="${AWS_PROFILE:-oven-terraform-scoped}"
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
TERRAFORM_DIR="$(dirname "$SCRIPT_DIR")"

INSTANCE_ID="$(
  AWS_PROFILE="$AWS_PROFILE_NAME" \
    terraform -chdir="$TERRAFORM_DIR" output -raw instance_id
)"

AWS_PROFILE="$AWS_PROFILE_NAME" aws ec2 describe-instances \
  --instance-ids "$INSTANCE_ID" \
  --query 'Reservations[0].Instances[0].{
    InstanceId:InstanceId,
    State:State.Name,
    PublicIp:PublicIpAddress,
    Type:InstanceType
  }' \
  --output table
