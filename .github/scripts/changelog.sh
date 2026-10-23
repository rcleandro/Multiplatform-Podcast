#!/usr/bin/env bash
# Release notes for tag $1 (20.6): the Conventional Commits since the previous tag, grouped by type.
# Run locally with any tag or commit: .github/scripts/changelog.sh HEAD
set -euo pipefail

tag="$1"
previous=$(git describe --tags --abbrev=0 "$tag^" 2>/dev/null || true)
subjects=$(git log --no-merges --pretty=%s ${previous:+"$previous.."}"$tag")

section() {
    local title="$1" pattern="$2" lines
    lines=$(printf '%s\n' "$subjects" | grep -E "$pattern" | sed -E 's/^[a-z]+(\([^)]*\))?!?: /- /' || true)
    if [ -n "$lines" ]; then
        printf '## %s\n\n%s\n\n' "$title" "$lines"
    fi
}

section "Features" '^feat(\(|!|:)'
section "Fixes" '^fix(\(|!|:)'
section "Performance" '^perf(\(|!|:)'
if [ -n "$previous" ]; then
    echo "**Full changelog:** $previous...$tag"
fi
