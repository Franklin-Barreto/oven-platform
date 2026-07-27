#!/bin/sh

set -eu

if [ "$#" -ne 3 ]; then
  echo "Usage: deploy.sh <application-image> <commit-sha> <aws-region>" >&2
  exit 64
fi

application_image="$1"
commit_sha="$2"
aws_region="$3"

staging_directory="/opt/oven-platform/deploy/staging"
environment_file="${staging_directory}/.env"
current_image_file="${staging_directory}/.current-application-image"
last_known_good_image_file="${staging_directory}/.last-known-good-application-image"
pending_image_file="${staging_directory}/.pending-application-image"

if [ "$(id -u)" -ne 0 ]; then
  echo "deploy.sh must run as root" >&2
  exit 77
fi

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

case "$application_image" in
  *.dkr.ecr."${aws_region}".amazonaws.com/oven-platform:"${commit_sha}")
    ;;
  *)
    echo "Application image must use the exact commit SHA tag in the staging ECR repository" >&2
    exit 64
    ;;
esac

if [ ! -f "$environment_file" ]; then
  echo "Missing staging environment file: ${environment_file}" >&2
  exit 66
fi

configured_image="$(
  awk -F= '
    $1 == "APPLICATION_IMAGE" {
      sub(/^[^=]*=/, "")
      print
      exit
    }
  ' "$environment_file"
)"

if [ -z "$configured_image" ]; then
  echo "APPLICATION_IMAGE is missing from ${environment_file}" >&2
  exit 65
fi

write_image_state() {
  destination="$1"
  image="$2"
  temporary="$(mktemp "${destination}.XXXXXX")"
  printf '%s\n' "$image" > "$temporary"
  chmod 600 "$temporary"
  chown root:root "$temporary"
  mv "$temporary" "$destination"
}

if [ ! -s "$current_image_file" ]; then
  write_image_state "$current_image_file" "$configured_image"
fi

approved_current_image="$(sed -n '1p' "$current_image_file")"

if [ -z "$approved_current_image" ]; then
  echo "Current approved application image is empty" >&2
  exit 65
fi

registry="${application_image%%/*}"

aws ecr get-login-password \
  --region "$aws_region" \
  | docker login \
      --username AWS \
      --password-stdin "$registry"

docker pull "$application_image"

write_image_state "$last_known_good_image_file" "$approved_current_image"
write_image_state "$pending_image_file" "$application_image"

environment_temporary="$(mktemp "${environment_file}.XXXXXX")"
trap 'rm -f "$environment_temporary"' EXIT HUP INT TERM

awk \
  -v replacement="APPLICATION_IMAGE=${application_image}" '
    BEGIN {
      replaced = 0
    }

    /^APPLICATION_IMAGE=/ {
      print replacement
      replaced = 1
      next
    }

    {
      print
    }

    END {
      if (!replaced) {
        exit 1
      }
    }
  ' "$environment_file" > "$environment_temporary"

chmod 600 "$environment_temporary"
chown root:root "$environment_temporary"
mv "$environment_temporary" "$environment_file"
trap - EXIT HUP INT TERM

cd "$staging_directory"

docker compose \
  --env-file .env \
  config --quiet

docker compose \
  --env-file .env \
  up \
  --detach \
  --no-build \
  --remove-orphans \
  postgres application caddy

attempt=1
while [ "$attempt" -le 36 ]; do
  if curl \
    --fail \
    --silent \
    --show-error \
    http://127.0.0.1:8080/actuator/health/readiness \
    >/dev/null; then
    echo "Deployment readiness passed commitSha=${commit_sha} image=${application_image}"
    exit 0
  fi

  sleep 5
  attempt=$((attempt + 1))
done

echo "Application did not become ready within 180 seconds" >&2
docker compose --env-file .env ps >&2
docker compose --env-file .env logs --tail 100 application >&2
exit 1
