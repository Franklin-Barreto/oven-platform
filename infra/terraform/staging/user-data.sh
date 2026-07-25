#!/bin/bash

set -euxo pipefail

dnf upgrade -y
dnf install -y docker

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
