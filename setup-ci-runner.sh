#!/bin/bash
set -e
# Fix WSL DNS for GitLab runner
if ! grep -q '223.5.5.5' /etc/resolv.conf 2>/dev/null; then
  printf 'nameserver 223.5.5.5\nnameserver 114.114.114.114\n' | sudo tee /etc/resolv.conf >/dev/null
fi
# Runner config: concurrent + pull_policy
if ! grep -q 'pull_policy' /etc/gitlab-runner/config.toml; then
  sudo sed -i '/network_mode = "gitlab-runner-net"/a\    pull_policy = "if-not-present"' /etc/gitlab-runner/config.toml
fi
sudo sed -i 's/^concurrent = .*/concurrent = 2/' /etc/gitlab-runner/config.toml
# Pre-pull CI images via mirror
for img in maven:3.9-eclipse-temurin-17 node:20-alpine docker:24-cli \
  docker.m.daocloud.io/library/maven:3.9-eclipse-temurin-17 \
  docker.m.daocloud.io/library/eclipse-temurin:17-jre \
  docker.m.daocloud.io/library/node:20-alpine \
  docker.m.daocloud.io/library/nginx:1.27-alpine; do
  docker pull "$img" || true
done
sudo gitlab-runner restart
sudo gitlab-runner verify 2>&1 | tail -2