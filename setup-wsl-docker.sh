#!/bin/bash
set -e
for i in $(seq 1 36); do
  if sudo fuser /var/lib/dpkg/lock-frontend >/dev/null 2>&1; then
    echo "Waiting for apt lock... $i"
    sleep 5
  else
    break
  fi
done
if ! docker info >/dev/null 2>&1; then
  echo "Installing docker.io..."
  sudo apt-get update -qq
  sudo DEBIAN_FRONTEND=noninteractive apt-get install -y docker.io
fi
sudo systemctl enable docker
sudo systemctl start docker
sudo usermod -aG docker gitlab-runner 2>/dev/null || true
docker info | head -10