#!/usr/bin/env bash
set -euo pipefail

# Placeholder release notes generator (to be expanded for master pipeline)

PREV_TAG="${1:-}"
NEW_TAG="${2:-}"

if [[ -z "$NEW_TAG" ]]; then
  echo "Usage: $0 <previous-tag> <new-tag>" >&2
  exit 2
fi

echo "# Release Notes $NEW_TAG"
echo
if [[ -n "$PREV_TAG" ]]; then
  echo "## Changes since $PREV_TAG"
  git --no-pager log --pretty=format:'- %h %s (%an)' "$PREV_TAG"..HEAD
else
  echo "## Changes"
  git --no-pager log --pretty=format:'- %h %s (%an)' -n 50
fi
