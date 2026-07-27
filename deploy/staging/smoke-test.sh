#!/bin/sh

set -eu

staging_directory="/opt/oven-platform/deploy/staging"
environment_file="${staging_directory}/.env"

if [ "$(id -u)" -ne 0 ]; then
  echo "smoke-test.sh must run as root" >&2
  exit 77
fi

if [ ! -f "$environment_file" ]; then
  echo "Missing staging environment file: ${environment_file}" >&2
  exit 66
fi

read_environment_value() {
  key="$1"

  awk \
    -v expected_key="$key" '
      index($0, expected_key "=") == 1 {
        sub(/^[^=]*=/, "")
        print
        exit
      }
    ' "$environment_file"
}

tenant_id="$(read_environment_value OVEN_STAGING_TENANT_ID)"
owner_email="$(read_environment_value OVEN_BOOTSTRAP_OWNER_EMAIL)"
owner_password="$(read_environment_value OVEN_BOOTSTRAP_OWNER_PASSWORD)"
staging_hostname="$(read_environment_value STAGING_HOSTNAME)"

if [ -z "$tenant_id" ]; then
  echo "OVEN_STAGING_TENANT_ID is required" >&2
  exit 65
fi

if [ -z "$owner_email" ]; then
  echo "OVEN_BOOTSTRAP_OWNER_EMAIL is required" >&2
  exit 65
fi

if [ -z "$owner_password" ]; then
  echo "OVEN_BOOTSTRAP_OWNER_PASSWORD is required" >&2
  exit 65
fi

if [ -z "$staging_hostname" ]; then
  echo "STAGING_HOSTNAME is required" >&2
  exit 65
fi

case "$staging_hostname" in
  *://*)
    echo "STAGING_HOSTNAME must not include a URL scheme" >&2
    exit 65
    ;;
esac

base_url="https://${staging_hostname}"

curl \
  --fail \
  --silent \
  --show-error \
  "${base_url}/actuator/health/readiness" \
  >/dev/null

login_payload="$(
  TENANT_ID="$tenant_id" \
  OWNER_EMAIL="$owner_email" \
  OWNER_PASSWORD="$owner_password" \
    python3 -c '
import json
import os

print(json.dumps({
    "tenantId": os.environ["TENANT_ID"],
    "email": os.environ["OWNER_EMAIL"],
    "password": os.environ["OWNER_PASSWORD"],
}))
'
)"

login_response="$(
  printf '%s' "$login_payload" \
    | curl \
        --fail-with-body \
        --silent \
        --show-error \
        --header 'Content-Type: application/json' \
        --data-binary @- \
        "${base_url}/auth/login"
)"

token="$(
  printf '%s' "$login_response" \
    | python3 -c '
import json
import sys

token = json.load(sys.stdin).get("token")
if not token:
    raise SystemExit("Login response does not contain a token")
print(token)
'
)"

printf \
  'header = "Authorization: Bearer %s"\n' \
  "$token" \
  | curl \
      --fail-with-body \
      --silent \
      --show-error \
      --config - \
      --header 'X-API-Version: 1.0' \
      "${base_url}/users" \
      >/dev/null

echo "Staging smoke tests passed hostname=${staging_hostname}"
