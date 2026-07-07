#!/bin/bash
set -e
sudo mkdir -p /etc/docker /etc/systemd/system/docker.service.d
sudo tee /etc/docker/daemon.json > /dev/null << 'EOF'
{
  "registry-mirrors": [
    "https://docker.m.daocloud.io",
    "https://docker.1ms.run"
  ]
}
EOF
sudo systemctl daemon-reload
sudo systemctl restart docker
sleep 2
docker info 2>/dev/null | grep -A3 "Registry Mirrors"
echo "--- pulling images ---"
docker pull maven:3.9-eclipse-temurin-17
docker pull node:20-alpine
docker pull docker:24-cli
echo "ALL IMAGES PULLED OK"