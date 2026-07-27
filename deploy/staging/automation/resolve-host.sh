#!/bin/sh

set -eu

if [ "$#" -ne 0 ]; then
  echo "Usage: resolve-host.sh" >&2
  exit 64
fi

instance_ids="$(
  aws ec2 describe-instances \
    --filters \
      Name=tag:project,Values=oven-platform \
      Name=tag:environment,Values=staging \
      Name=tag:managed-by,Values=terraform \
      Name=instance-state-name,Values=pending,running,stopping,stopped \
    --query 'Reservations[].Instances[].InstanceId' \
    --output text
)"

set -- $instance_ids

if [ "$#" -eq 0 ]; then
  echo "No Terraform-managed staging instance was found" >&2
  exit 66
fi

if [ "$#" -ne 1 ]; then
  echo "Expected exactly one staging instance, but found $#" >&2
  exit 65
fi

instance_id="$1"

case "$instance_id" in
  i-*)
    ;;
  *)
    echo "AWS returned an invalid EC2 instance ID" >&2
    exit 65
    ;;
esac

if [ -n "${GITHUB_OUTPUT:-}" ]; then
  echo "instance-id=${instance_id}" >> "$GITHUB_OUTPUT"
fi

echo "Resolved staging instance: ${instance_id}"