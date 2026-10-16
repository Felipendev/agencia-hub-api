#!/usr/bin/env bash
set -euo pipefail

base="${1:?Pass the current integration branch ref}"
path=src/main/resources/db/migration

# Compare the complete candidate tree to current main, not only changes since branching.
# This also blocks a stale branch that is missing migrations already integrated.
changed=$(git diff --no-renames --name-only --diff-filter=MD "$base" HEAD -- "$path")
if [[ -n "$changed" ]]; then
  echo "Published migrations were modified or removed; update the branch without rewriting history:"
  echo "$changed"
  exit 1
fi

versions=$(git ls-tree -r --name-only HEAD -- "$path" | sed -nE 's|.*/V([0-9]+)__.*\.sql$|\1|p' | sort -n)
duplicates=$(echo "$versions" | uniq -d)
if [[ -n "$duplicates" ]]; then
  echo "Duplicate migration versions: $duplicates"
  exit 1
fi

last=$(git ls-tree -r --name-only "$base" -- "$path" | sed -nE 's|.*/V([0-9]+)__.*\.sql$|\1|p' | sort -n | tail -1)
expected=$(( ${last:-0} + 1 ))
added=$(git diff --no-renames --name-only --diff-filter=A "$base" HEAD -- "$path" | sed -nE 's|.*/V([0-9]+)__.*\.sql$|\1|p' | sort -n)
while IFS= read -r version; do
  [[ -z "$version" ]] && continue
  if [[ "$version" -ne "$expected" ]]; then
    echo "Migration V$version cannot be integrated yet: expected V$expected. Follow the reservations and merge order."
    exit 1
  fi
  expected=$((expected + 1))
done <<< "$added"
echo "Migration history preserved; new versions are sequential."
