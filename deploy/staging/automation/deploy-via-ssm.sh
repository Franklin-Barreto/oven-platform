#!/bin/sh

set -eu

if [ "$#" -ne 4 ]; then
  echo \
    "Usage: deploy-via-ssm.sh <instance-id> <application-image> <commit-sha> <aws-region>" \
    >&2
  exit 64
fi

instance_id="$1"
application_image="$2"
commit_sha="$3"
aws_region="$4"

repository_slug="${GITHUB_REPOSITORY:-Franklin-Barreto/oven-platform}"
maximum_attempts=60
interval_seconds=10

case "$instance_id" in
  i-*)
    ;;
  *)
    echo "Invalid EC2 instance ID" >&2
    exit 64
    ;;
esac

case "$commit_sha" in
  *[!0-9a-f]* | "")
    echo "Commit SHA must contain only lowercase hexadecimal characters" >&2
    exit 64
    ;;
esac

if [ "${#commit_sha}" -ne 40 ]; then
  echo "Commit SHA must contain exactly 40 characters" >&2
  exit 64
fi

case "$aws_region" in
  *[!a-z0-9-]* | "")
    echo "AWS region contains invalid characters" >&2
    exit 64
    ;;
esac

case "$repository_slug" in
  *[!A-Za-z0-9._/-]* | */*/* | /* | */ | "")
    echo "GitHub repository slug is invalid" >&2
    exit 64
    ;;
esac

case "$application_image" in
  *.dkr.ecr."${aws_region}".amazonaws.com/oven-platform:"${commit_sha}")
    ;;
  *)
    echo "Application image does not match the commit SHA and staging repository" >&2
    exit 64
    ;;
esac

remote_script="$(cat <<EOF
set -eu

repository_directory="/opt/oven-platform"

if [ ! -d "\${repository_directory}/.git" ]; then
  sudo -u ec2-user git clone \
    --no-checkout \
    "https://github.com/${repository_slug}.git" \
    "\${repository_directory}"
fi

sudo -u ec2-user git \
  -C "\${repository_directory}" \
  fetch \
  --no-tags \
  --depth=1 \
  origin \
  "${commit_sha}"

sudo -u ec2-user git \
  -C "\${repository_directory}" \
  checkout \
  --detach \
  --force \
  "${commit_sha}"

"\${repository_directory}/deploy/staging/deploy.sh" \
  "${application_image}" \
  "${commit_sha}" \
  "${aws_region}"

"\${repository_directory}/deploy/staging/smoke-test.sh"

"\${repository_directory}/deploy/staging/mark-deployment-successful.sh" \
  "${application_image}"
EOF
)"

command_parameters="$(
  jq \
    --null-input \
    --arg command "$remote_script" \
    '{commands: [$command]}'
)"

command_id="$(
  aws ssm send-command \
    --instance-ids "$instance_id" \
    --document-name AWS-RunShellScript \
    --comment "Deploy Oven Platform ${commit_sha}" \
    --parameters "$command_parameters" \
    --query 'Command.CommandId' \
    --output text
)"

if [ -n "${GITHUB_OUTPUT:-}" ]; then
  echo "command-id=${command_id}" >> "$GITHUB_OUTPUT"
fi

echo "SSM deployment command submitted: ${command_id}"

attempt=1

while [ "$attempt" -le "$maximum_attempts" ]; do
  if ! invocation="$(
    aws ssm get-command-invocation \
      --command-id "$command_id" \
      --instance-id "$instance_id" \
      --output json \
      2>/dev/null
  )"; then
    echo \
      "Waiting for SSM command invocation: attempt ${attempt}/${maximum_attempts}"
    sleep "$interval_seconds"
    attempt=$((attempt + 1))
    continue
  fi

  status="$(
    printf '%s' "$invocation" \
      | jq --raw-output '.Status'
  )"

  case "$status" in
    Success)
      printf '%s' "$invocation" \
        | jq --raw-output '.StandardOutputContent'
      exit 0
      ;;
    Failed | Cancelled | TimedOut | Cancelling)
      printf '%s' "$invocation" \
        | jq '{
            status: .Status,
            stdout: .StandardOutputContent,
            stderr: .StandardErrorContent
          }'
      exit 1
      ;;
    Pending | InProgress | Delayed)
      echo \
        "Deployment status: ${status}, attempt ${attempt}/${maximum_attempts}"
      sleep "$interval_seconds"
      ;;
    *)
      echo "Unexpected SSM command status: ${status}" >&2
      exit 1
      ;;
  esac

  attempt=$((attempt + 1))
done

echo "Deployment did not finish within ten minutes" >&2
exit 1
