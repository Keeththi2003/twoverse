#!/usr/bin/env bash
# Prepares a release: bumps the app version, the README badge and the changelog.
# Usage: scripts/release.sh X.Y.Z
# It edits files only. It never commits, tags or pushes; it prints the commands to run.
set -euo pipefail

REPO_URL="https://github.com/Keeththi2003/twoverse"

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GRADLE_FILE="$ROOT/android/app/build.gradle.kts"
README_FILE="$ROOT/README.md"
CHANGELOG_FILE="$ROOT/CHANGELOG.md"

fail() {
  echo "error: $1" >&2
  exit 1
}

[ $# -eq 1 ] || fail "usage: scripts/release.sh X.Y.Z"
NEW="$1"
[[ "$NEW" =~ ^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$ ]] \
  || fail "'$NEW' is not a version in the form X.Y.Z (no leading zeros, no suffix)"

CURRENT="$(sed -n 's/.*versionName = "\(.*\)".*/\1/p' "$GRADLE_FILE")"
CODE="$(sed -n 's/.*versionCode = \([0-9][0-9]*\).*/\1/p' "$GRADLE_FILE")"
[[ "$CURRENT" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || fail "could not read versionName from $GRADLE_FILE"
[[ "$CODE" =~ ^[0-9]+$ ]] || fail "could not read versionCode from $GRADLE_FILE"

is_greater() {
  local IFS=.
  local -a a=($1) b=($2)
  local i
  for i in 0 1 2; do
    if ((a[i] > b[i])); then return 0; fi
    if ((a[i] < b[i])); then return 1; fi
  done
  return 1
}
is_greater "$NEW" "$CURRENT" || fail "$NEW must be greater than the current version $CURRENT"

if git -C "$ROOT" rev-parse --git-dir >/dev/null 2>&1 \
  && [ -n "$(git -C "$ROOT" tag -l "v$NEW")" ]; then
  fail "tag v$NEW already exists"
fi

grep -q '^## \[Unreleased\]' "$CHANGELOG_FILE" || fail "CHANGELOG.md has no '## [Unreleased]' section"
grep -q "^## \[$NEW\]" "$CHANGELOG_FILE" && fail "CHANGELOG.md already has a section for $NEW"
grep -q '<!-- version:start -->' "$README_FILE" && grep -q '<!-- version:end -->' "$README_FILE" \
  || fail "README.md is missing the version:start / version:end markers"

# The notes under Unreleased, up to the next version heading or the link list.
NOTES="$(awk '
  /^## \[Unreleased\]/ { in_section = 1; next }
  in_section && (/^## \[/ || /^\[[^]]+\]: /) { exit }
  in_section { print }
' "$CHANGELOG_FILE")"
echo "$NOTES" | grep -q '[^[:space:]]' \
  || fail "the Unreleased section of CHANGELOG.md is empty; release.yml needs release notes"

TODAY="$(date +%F)"
NEW_CODE=$((CODE + 1))
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

# Build every new file first, so a failure leaves the working tree untouched.
sed -E \
  -e "s/(versionName = \")[^\"]*(\")/\1$NEW\2/" \
  -e "s/(versionCode = )[0-9]+/\1$NEW_CODE/" \
  "$GRADLE_FILE" > "$TMP/build.gradle.kts"

awk -v badge="<img src=\"https://img.shields.io/badge/version-$NEW-informational\" alt=\"Version $NEW\">" '
  /<!-- version:start -->/ { print; print badge; skip = 1; next }
  /<!-- version:end -->/ { skip = 0 }
  !skip { print }
' "$README_FILE" > "$TMP/README.md"

EXISTING_LINKS="$(grep -E '^\[[^]]+\]: ' "$CHANGELOG_FILE" | grep -v '^\[Unreleased\]: ' || true)"
{
  awk -v new="$NEW" -v today="$TODAY" '
    /^\[[^]]+\]: / { next }
    { lines[++n] = $0 }
    END {
      while (n > 0 && lines[n] ~ /^[[:space:]]*$/) n--
      for (i = 1; i <= n; i++) {
        print lines[i]
        if (lines[i] ~ /^## \[Unreleased\]/) {
          print ""
          print "## [" new "] - " today
        }
      }
    }
  ' "$CHANGELOG_FILE"
  echo
  echo "[Unreleased]: $REPO_URL/compare/v$NEW...HEAD"
  echo "[$NEW]: $REPO_URL/compare/v$CURRENT...v$NEW"
  if ! echo "$EXISTING_LINKS" | grep -q "^\[$CURRENT\]: "; then
    echo "[$CURRENT]: $REPO_URL/releases/tag/v$CURRENT"
  fi
  [ -z "$EXISTING_LINKS" ] || echo "$EXISTING_LINKS"
} > "$TMP/CHANGELOG.md"

cp "$TMP/build.gradle.kts" "$GRADLE_FILE"
cp "$TMP/README.md" "$README_FILE"
cp "$TMP/CHANGELOG.md" "$CHANGELOG_FILE"

BRANCH="$(git -C "$ROOT" rev-parse --abbrev-ref HEAD 2>/dev/null || echo '<branch>')"

cat <<MSG
Prepared $NEW (previous $CURRENT, versionCode $CODE -> $NEW_CODE).

Updated:
  android/app/build.gradle.kts
  README.md
  CHANGELOG.md

Review the changes:
  git diff

Then run these yourself. This script does not commit, tag or push.

  git add android/app/build.gradle.kts README.md CHANGELOG.md
  git commit -m "chore: release $NEW"
  git push origin $BRANCH

If main is protected, merge that branch into main through a pull request first, then:

  git checkout main && git pull
  git tag -a v$NEW -m "Twoverse $NEW"
  git push origin v$NEW

Pushing the tag starts the Release workflow.
MSG
