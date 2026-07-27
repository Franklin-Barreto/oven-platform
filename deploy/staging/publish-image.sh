#!/bin/sh

set -eu

if [ "$#" -ne 3 ]; then
  echo "Usage: publish-image.sh <aws-region> <ecr-repository-name> <commit-sha>" >&2
  exit 64
fi

aws_region="$1"
repository_name="$2"
commit_sha="$3"

case "$aws_region" in
  *[!a-z0-9-]* | "")
    echo "AWS region contains invalid characters" >&2
    exit 64
    ;;
esac

case "$repository_name" in
  *[!a-z0-9._/-]* | "")
    echo "ECR repository name contains invalid characters" >&2
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

script_directory="$(
  CDPATH='' cd -- "$(dirname -- "$0")" && pwd
)"
repository_root="$(
  CDPATH='' cd -- "${script_directory}/../.." && pwd
)"

account_id="$(
  aws sts get-caller-identity \
    --query Account \
    --output text
)"

case "$account_id" in
  *[!0-9]* | "")
    echo "AWS account ID is invalid" >&2
    exit 65
    ;;
esac

if [ "${#account_id}" -ne 12 ]; then
  echo "AWS account ID must contain exactly 12 digits" >&2
  exit 65
fi

if [ "${GITHUB_ACTIONS:-false}" = "true" ]; then
  echo "::add-mask::${account_id}"
fi

ecr_registry="${account_id}.dkr.ecr.${aws_region}.amazonaws.com"
application_image="${ecr_registry}/${repository_name}:${commit_sha}"
existing_media_type="$(
  aws ecr batch-get-image \
    --region "$aws_region" \
    --repository-name "$repository_name" \
    --image-ids "imageTag=${commit_sha}" \
    --query 'images[0].imageManifestMediaType' \
    --output text
)"

case "$existing_media_type" in
  application/vnd.docker.distribution.manifest.v2+json|application/vnd.oci.image.manifest.v1+json)
    echo "Immutable application image already exists: ${application_image}"

    if [ -n "${GITHUB_OUTPUT:-}" ]; then
      {
        echo "registry=${ecr_registry}"
        echo "image=${application_image}"
      } >> "$GITHUB_OUTPUT"
    fi

    exit 0
    ;;
  None | "")
    ;;
  *)
    echo \
      "Existing image uses unsupported manifest type: ${existing_media_type}" \
      >&2
    exit 65
    ;;
esac

aws ecr get-login-password \
  --region "$aws_region" \
  | docker login \
      --username AWS \
      --password-stdin "$ecr_registry"

docker buildx build \
  --platform linux/amd64 \
  --provenance=false \
  --sbom=false \
  --label "org.opencontainers.image.revision=${commit_sha}" \
  --label "org.opencontainers.image.source=https://github.com/Franklin-Barreto/oven-platform" \
  --tag "$application_image" \
  --push \
  "$repository_root"

if [ -n "${GITHUB_OUTPUT:-}" ]; then
  {
    echo "registry=${ecr_registry}"
    echo "image=${application_image}"
  } >> "$GITHUB_OUTPUT"
fi

echo "Published immutable application image: ${application_image}"