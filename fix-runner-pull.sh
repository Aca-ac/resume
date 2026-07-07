#!/bin/bash
if ! grep -q 'pull_policy' /etc/gitlab-runner/config.toml; then
  sudo sed -i '/network_mode = "gitlab-runner-net"/a\    pull_policy = "if-not-present"' /etc/gitlab-runner/config.toml
fi
grep pull_policy /etc/gitlab-runner/config.toml
sudo gitlab-runner restart
sudo gitlab-runner verify 2>&1 | tail -2