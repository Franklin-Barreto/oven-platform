#!/bin/sh

set -eu

if [ "$#" -ne 1 ]; then
  echo "Usage: start-host.sh <instance-id>" >&2
  exit 64
fi

instance_id="$1"

case "$instance_id" in
  i-*)
    ;;
  *)
    echo "Invalid EC2 instance ID" >&2
    exit 64
    ;;
esac

instance_state="$(
  aws ec2 describe-instances \
    --instance-ids "$instance_id" \
    --query 'Reservations[0].Instances[0].State.Name' \
    --output text
)"

case "$instance_state" in
  stopping)
    echo "Waiting for staging instance to stop"
    aws ec2 wait instance-stopped \
      --instance-ids "$instance_id"
    instance_state="stopped"
    ;;
  stopped | pending | running)
    ;;
  *)
    echo "Unsupported staging instance state: ${instance_state}" >&2
    exit 65
    ;;
esac

if [ "$instance_state" = "stopped" ]; then
  echo "Starting staging instance: ${instance_id}"

  aws ec2 start-instances \
    --instance-ids "$instance_id" \
    >/dev/null
fi

echo "Waiting for staging instance to enter running state"

aws ec2 wait instance-running \
  --instance-ids "$instance_id"

echo "Waiting for EC2 status checks"

aws ec2 wait instance-status-ok \
  --instance-ids "$instance_id"

echo "Staging instance is ready: ${instance_id}"