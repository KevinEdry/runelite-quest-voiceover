# Publish Plugin

Publish a new version of the Quest Voiceover plugin to the RuneLite Plugin Hub.

**Input:** $ARGUMENTS (optional: additional context or instructions)

## Prerequisites

- The plugin-hub fork must be cloned at `../plugin-hub`
- GitHub CLI (`gh`) must be authenticated
- All plugin changes must be committed and pushed to main
- Version bump should already be done by CI (release-please)

## Important Rules

- **NEVER use AI attestation** - no "Generated with Claude Code" footers or Co-Authored-By lines in any commits or PRs
- **NEVER bump the version manually** - CI (release-please) handles versioning automatically
- **Always pull main before starting** - ensure you have the latest version bump from CI

## Instructions

1. **Pull latest from main:**
   - Run `git pull origin main` to get the latest changes (including CI version bumps)

2. **Get current version:**
   - Read the version from `build.gradle` (line with `version = 'X.X.X'`)

3. **Get the currently published commit:**
   - Fetch `https://raw.githubusercontent.com/runelite/plugin-hub/master/plugins/quest-voiceover`
   - Extract the `commit=` value

4. **Get the HEAD commit hash:**
   - Run `git rev-parse HEAD` to get the full 40-character hash

5. **Check for existing GitHub release:**
   - Run `gh release view v{version}` to check if release exists
   - If no release exists, warn the user that CI may not have completed yet

6. **Check for existing plugin-hub PR:**
   - Run `gh pr list --head update-quest-voiceover --repo runelite/plugin-hub --json number,url`
   - Note whether a PR already exists

7. **Sync plugin-hub fork with upstream:**
   - Navigate to `../plugin-hub`
   - Add upstream remote if not exists: `git remote add upstream https://github.com/runelite/plugin-hub.git`
   - Fetch upstream: `git fetch upstream`
   - Checkout master: `git checkout master`
   - Reset to upstream: `git reset --hard upstream/master`

8. **Update the plugin file:**
   - Edit `plugins/quest-voiceover`
   - Update the `commit=` line with the new commit hash

9. **Create branch and commit:**
   - Create branch: `git checkout -B update-quest-voiceover`
   - Stage the plugin file
   - Commit with message: `quest-voiceover {version}`

10. **Push branch:**
    - Push branch: `git push -u origin update-quest-voiceover --force`

11. **Create or update PR:**
    - If PR already exists: Update title and body using `gh api repos/runelite/plugin-hub/pulls/{pr_number} -X PATCH`
    - If no PR exists: Create PR using `gh pr create`
    - Title: `quest-voiceover {version}`
    - Body: Use template below

12. **PR body template:**
```
Update quest-voiceover plugin to version {version}.

**Release:** https://github.com/KevinEdry/runelite-quest-voiceover/releases/tag/v{version}

### Features
- [List feat: commits between published commit and HEAD]

### Bug Fixes
- [List fix: commits between published commit and HEAD]
```

13. **Output the PR URL** when complete

## Additional Notes

- The plugin-hub fork should be at `../plugin-hub` relative to this repo
- After creating/updating the PR, return to the original repo directory
- If the GitHub release doesn't exist yet, wait for CI to complete before running this command
