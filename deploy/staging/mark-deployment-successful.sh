#!/bin/sh

set -eu

if [ "$#" -ne 1 ]; then
  echo "Usage: mark-deployment-successful.sh <application-image>" >&2
  exit 64
fi

application_image="$1"
staging_directory="/opt/oven-platform/deploy/staging"
environment_file="${staging_directory}/.env"
current_image_file="${staging_directory}/.current-application-image"
pending_image_file="${staging_directory}/.pending-application-image"

if [ "$(id -u)" -ne 0 ]; then
  echo "mark-deployment-successful.sh must run as root" >&2
  exit 77
fi

if [ ! -s "$pending_image_file" ]; then
  echo "No pending application image is recorded" >&2
  exit 66
fi

pending_image="$(sed -n '1p' "$pending_image_file")"

if [ "$pending_image" != "$application_image" ]; then
  echo "Pending application image does not match the successful deployment" >&2
  exit 65
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

if [ "$configured_image" != "$application_image" ]; then
  echo "Configured application image does not match the successful deployment" >&2
  exit 65
fi

current_image_temporary="$(mktemp "${current_image_file}.XXXXXX")"
trap 'rm -f "$current_image_temporary"' EXIT HUP INT TERM

printf '%s\n' "$application_image" > "$current_image_temporary"
chmod 600 "$current_image_temporary"
chown root:root "$current_image_temporary"
mv "$current_image_temporary" "$current_image_file"
trap - EXIT HUP INT TERM

rm -f "$pending_image_file"

echo "Deployment marked successful image=${application_image}"
