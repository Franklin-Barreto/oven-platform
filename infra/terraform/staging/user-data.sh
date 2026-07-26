#!/bin/bash

set -euxo pipefail

COMPOSE_VERSION="v5.1.4"
COMPOSE_SHA256="33b208d7e76639db742fae84b966cc01dacae58ca3fc4dabbc907045aefdf0c4"
COMPOSE_DOWNLOAD="$(mktemp)"
trap 'rm -f "${COMPOSE_DOWNLOAD}"' EXIT

dnf upgrade -y
dnf install -y docker

curl \
  --fail \
  --show-error \
  --location \
  "https://github.com/docker/compose/releases/download/${COMPOSE_VERSION}/docker-compose-linux-x86_64" \
  --output "${COMPOSE_DOWNLOAD}"

echo "${COMPOSE_SHA256}  ${COMPOSE_DOWNLOAD}" | sha256sum --check -

install \
  --directory \
  --mode 755 \
  /usr/local/lib/docker/cli-plugins

install \
  --mode 755 \
  "${COMPOSE_DOWNLOAD}" \
  /usr/local/lib/docker/cli-plugins/docker-compose

systemctl enable --now docker
systemctl enable --now amazon-ssm-agent

usermod -aG docker ec2-user

mkdir -p /opt/oven-platform
chown ec2-user:ec2-user /opt/oven-platform

if ! swapon --show | grep -q /swapfile; then
  fallocate -l 2G /swapfile
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  echo "/swapfile none swap sw 0 0" >> /etc/fstab
fi
