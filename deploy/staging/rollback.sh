#!/bin/sh

set -eu

if [ "$#" -gt 1 ]; then
  echo "Usage: rollback.sh [aws-region]" >&2
  exit 64
fi

aws_region="${1:-us-east-1}"
staging_directory="/opt/oven-platform/deploy/staging"
last_known_good_image_file="${staging_directory}/.last-known-good-application-image"
deploy_script="${staging_directory}/deploy.sh"
smoke_test_script="${staging_directory}/smoke-test.sh"
mark_successful_script="${staging_directory}/mark-deployment-successful.sh"

if [ "$(id -u)" -ne 0 ]; then
  echo "rollback.sh must run as root" >&2
  exit 77
fi

if [ ! -s "$last_known_good_image_file" ]; then
  echo "No last-known-good application image is recorded" >&2
  exit 66
fi

last_known_good_image="$(sed -n '1p' "$last_known_good_image_file")"
last_known_good_commit_sha="${last_known_good_image##*:}"

case "$last_known_good_commit_sha" in
  *[!0-9a-f]* | "")
    echo "Last-known-good image does not contain a valid commit SHA tag" >&2
    exit 65
    ;;
esac

if [ "${#last_known_good_commit_sha}" -ne 40 ]; then
  echo "Last-known-good image does not contain a full commit SHA tag" >&2
  exit 65
fi

rollback_source_image="$last_known_good_image"

"$deploy_script" \
  "$rollback_source_image" \
  "$last_known_good_commit_sha" \
  "$aws_region"

"$smoke_test_script"
"$mark_successful_script" "$rollback_source_image"

echo "Rollback finished image=${rollback_source_image}"
