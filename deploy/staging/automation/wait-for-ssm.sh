#!/bin/sh

set -eu

if [ "$#" -lt 1 ] || [ "$#" -gt 2 ]; then
  echo "Usage: wait-for-ssm.sh <instance-id> [maximum-attempts]" >&2
  exit 64
fi

instance_id="$1"
maximum_attempts="${2:-36}"
interval_seconds=5

case "$instance_id" in
  i-*)
    ;;
  *)
    echo "Invalid EC2 instance ID" >&2
    exit 64
    ;;
esac

case "$maximum_attempts" in
  *[!0-9]* | 0 | "")
    echo "Maximum attempts must be a positive integer" >&2
    exit 64
    ;;
esac

attempt=1

while [ "$attempt" -le "$maximum_attempts" ]; do
  ping_status="$(
    aws ssm describe-instance-information \
      --filters "Key=InstanceIds,Values=${instance_id}" \
      --query 'InstanceInformationList[0].PingStatus' \
      --output text
  )"

  if [ "$ping_status" = "Online" ]; then
    echo "Session Manager is online for ${instance_id}"
    exit 0
  fi

  echo \
    "Waiting for Session Manager: attempt ${attempt}/${maximum_attempts}, status=${ping_status}"

  sleep "$interval_seconds"
  attempt=$((attempt + 1))
done

echo \
  "Session Manager did not become available for ${instance_id}" \
  >&2
exit 1