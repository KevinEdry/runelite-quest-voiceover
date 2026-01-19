#!/usr/bin/env bash
set -euo pipefail

# Publish Quest Voiceover plugin to RuneLite Plugin Hub
# Usage: ./scripts/publish-plugin.sh

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(dirname "$SCRIPT_DIR")"
PLUGIN_HUB_DIR="$REPO_DIR/../plugin-hub"
PLUGIN_NAME="quest-voiceover"
GITHUB_REPO="KevinEdry/runelite-quest-voiceover"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."

    if ! command -v gh &> /dev/null; then
        log_error "GitHub CLI (gh) is not installed"
        exit 1
    fi

    if ! gh auth status -h github.com &> /dev/null; then
        log_error "GitHub CLI is not authenticated. Run 'gh auth login' first"
        exit 1
    fi

    if [[ ! -d "$PLUGIN_HUB_DIR" ]]; then
        log_error "Plugin hub fork not found at $PLUGIN_HUB_DIR"
        log_error "Clone it first: git clone https://github.com/KevinEdry/plugin-hub.git ../plugin-hub"
        exit 1
    fi

    log_success "Prerequisites check passed"
}

# Pull latest from main
pull_latest() {
    log_info "Pulling latest from main..."
    cd "$REPO_DIR"
    git pull origin main
    log_success "Pulled latest changes"
}

# Get version from build.gradle
get_version() {
    VERSION=$(grep "^version = " "$REPO_DIR/build.gradle" | sed "s/version = '\(.*\)'.*/\1/")
    if [[ -z "$VERSION" ]]; then
        log_error "Could not extract version from build.gradle"
        exit 1
    fi
    log_info "Current version: $VERSION"
}

# Get HEAD commit hash
get_head_commit() {
    cd "$REPO_DIR"
    HEAD_COMMIT=$(git rev-parse HEAD)
    log_info "HEAD commit: $HEAD_COMMIT"
}

# Get currently published commit
get_published_commit() {
    log_info "Fetching currently published commit..."
    PLUGIN_HUB_URL="https://raw.githubusercontent.com/runelite/plugin-hub/master/plugins/$PLUGIN_NAME"
    PLUGIN_CONTENT=$(curl -sf "$PLUGIN_HUB_URL" || echo "")

    if [[ -z "$PLUGIN_CONTENT" ]]; then
        log_warn "Plugin not found in plugin-hub (first publish?)"
        PUBLISHED_COMMIT=""
    else
        PUBLISHED_COMMIT=$(echo "$PLUGIN_CONTENT" | grep "^commit=" | cut -d'=' -f2)
        log_info "Published commit: $PUBLISHED_COMMIT"
    fi
}

# Check if GitHub release exists
check_release() {
    log_info "Checking for GitHub release v$VERSION..."
    if gh release view "v$VERSION" --repo "$GITHUB_REPO" &> /dev/null; then
        log_success "Release v$VERSION exists"
    else
        log_error "Release v$VERSION not found. Wait for CI to complete before publishing."
        exit 1
    fi
}

# Check for existing PR
check_existing_pr() {
    log_info "Checking for existing plugin-hub PR..."
    EXISTING_PR=$(gh pr list --head update-$PLUGIN_NAME --repo runelite/plugin-hub --json number,url)

    if [[ "$EXISTING_PR" != "[]" ]]; then
        PR_NUMBER=$(echo "$EXISTING_PR" | jq -r '.[0].number')
        PR_URL=$(echo "$EXISTING_PR" | jq -r '.[0].url')
        log_info "Found existing PR #$PR_NUMBER: $PR_URL"
    else
        PR_NUMBER=""
        PR_URL=""
        log_info "No existing PR found"
    fi
}

# Get changelog (commits between published and HEAD)
get_changelog() {
    log_info "Generating changelog..."
    cd "$REPO_DIR"

    FEATURES=""
    FIXES=""

    if [[ -n "$PUBLISHED_COMMIT" ]]; then
        # Get commit messages between published and HEAD
        while IFS= read -r line; do
            if [[ "$line" == feat:* ]] || [[ "$line" == "feat("* ]]; then
                # Extract message after "feat:" or "feat(scope):"
                msg=$(echo "$line" | sed 's/^feat[^:]*: //')
                FEATURES="${FEATURES}- ${msg}\n"
            elif [[ "$line" == fix:* ]] || [[ "$line" == "fix("* ]]; then
                msg=$(echo "$line" | sed 's/^fix[^:]*: //')
                FIXES="${FIXES}- ${msg}\n"
            fi
        done < <(git log "$PUBLISHED_COMMIT..HEAD" --pretty=format:"%s")
    fi

    if [[ -z "$FEATURES" ]]; then
        FEATURES="- No new features\n"
    fi

    if [[ -z "$FIXES" ]]; then
        FIXES="- No bug fixes\n"
    fi
}

# Sync plugin-hub fork with upstream
sync_plugin_hub() {
    log_info "Syncing plugin-hub fork with upstream..."
    cd "$PLUGIN_HUB_DIR"

    # Add upstream if not exists
    if ! git remote | grep -q "^upstream$"; then
        git remote add upstream https://github.com/runelite/plugin-hub.git
    fi

    git fetch upstream
    git checkout master
    git reset --hard upstream/master

    log_success "Plugin-hub fork synced"
}

# Update plugin file
update_plugin_file() {
    log_info "Updating plugin file..."
    cd "$PLUGIN_HUB_DIR"

    PLUGIN_FILE="plugins/$PLUGIN_NAME"

    # Create or update plugin file
    cat > "$PLUGIN_FILE" << EOF
repository=https://github.com/$GITHUB_REPO.git
commit=$HEAD_COMMIT
jarSizeLimitMiB=15
EOF

    log_success "Plugin file updated"
}

# Create branch and commit
create_commit() {
    log_info "Creating branch and commit..."
    cd "$PLUGIN_HUB_DIR"

    git checkout -B "update-$PLUGIN_NAME"
    git add "plugins/$PLUGIN_NAME"
    git commit -m "$PLUGIN_NAME $VERSION"

    log_success "Commit created"
}

# Push branch
push_branch() {
    log_info "Pushing branch..."
    cd "$PLUGIN_HUB_DIR"
    git push -u origin "update-$PLUGIN_NAME" --force
    log_success "Branch pushed"
}

# Create or update PR
create_or_update_pr() {
    log_info "Creating/updating PR..."
    cd "$PLUGIN_HUB_DIR"

    PR_TITLE="$PLUGIN_NAME $VERSION"
    PR_BODY="Update $PLUGIN_NAME plugin to version $VERSION.

**Release:** https://github.com/$GITHUB_REPO/releases/tag/v$VERSION

### Features
$(echo -e "$FEATURES")
### Bug Fixes
$(echo -e "$FIXES")"

    if [[ -n "$PR_NUMBER" ]]; then
        # Update existing PR
        log_info "Updating existing PR #$PR_NUMBER..."
        gh api "repos/runelite/plugin-hub/pulls/$PR_NUMBER" \
            -X PATCH \
            -f title="$PR_TITLE" \
            -f body="$PR_BODY" > /dev/null
        FINAL_PR_URL="$PR_URL"
        log_success "PR updated: $FINAL_PR_URL"
    else
        # Create new PR
        log_info "Creating new PR..."
        FINAL_PR_URL=$(gh pr create \
            --repo runelite/plugin-hub \
            --title "$PR_TITLE" \
            --body "$PR_BODY" \
            --head "KevinEdry:update-$PLUGIN_NAME" \
            --base master)
        log_success "PR created: $FINAL_PR_URL"
    fi
}

# Main
main() {
    echo ""
    echo "======================================"
    echo "  Quest Voiceover Plugin Publisher"
    echo "======================================"
    echo ""

    check_prerequisites
    pull_latest
    get_version
    get_head_commit
    get_published_commit

    # Check if already published
    if [[ "$HEAD_COMMIT" == "$PUBLISHED_COMMIT" ]]; then
        log_warn "HEAD commit is already published. Nothing to do."
        exit 0
    fi

    check_release
    check_existing_pr
    get_changelog
    sync_plugin_hub
    update_plugin_file
    create_commit
    push_branch
    create_or_update_pr

    cd "$REPO_DIR"

    echo ""
    echo "======================================"
    log_success "Plugin published successfully!"
    echo "PR URL: $FINAL_PR_URL"
    echo "======================================"
}

main "$@"
