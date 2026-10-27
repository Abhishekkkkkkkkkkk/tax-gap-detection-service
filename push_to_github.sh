#!/usr/bin/env bash
# ==============================================================================
# Helper Script to Push Tax Gap Detection Service to GitHub
# ==============================================================================

set -e

REPO_NAME="tax-gap-detection-service"
GITHUB_USER="Abhishekkkkkkkkkkk"
HTTPS_URL="https://github.com/${GITHUB_USER}/${REPO_NAME}.git"
SSH_URL="git@github.com:${GITHUB_USER}/${REPO_NAME}.git"

echo "======================================================================"
echo "Pushing ${REPO_NAME} to https://github.com/${GITHUB_USER}"
echo "======================================================================"

echo ""
echo "Please ensure you have created an empty repository on GitHub first:"
echo "👉 https://github.com/new"
echo "   Repository name: ${REPO_NAME}"
echo "   (Do not initialize with README, .gitignore, or license)"
echo ""

echo "Choose your push method:"
echo "1) HTTPS using Personal Access Token (PAT)"
echo "2) SSH (git@github.com:...)"
echo "3) GitHub CLI (gh)"
read -p "Enter choice [1-3]: " choice

if [ "$choice" == "1" ]; then
    read -p "Enter your GitHub Personal Access Token (PAT): " GITHUB_TOKEN
    git remote set-url origin "https://${GITHUB_USER}:${GITHUB_TOKEN}@github.com/${GITHUB_USER}/${REPO_NAME}.git"
    git branch -M main
    git push -u origin main
    # Reset remote back to clean URL to avoid storing token in .git/config
    git remote set-url origin "$HTTPS_URL"
    echo ""
    echo "✅ Successfully pushed to $HTTPS_URL!"

elif [ "$choice" == "2" ]; then
    git remote set-url origin "$SSH_URL"
    git branch -M main
    git push -u origin main
    echo ""
    echo "✅ Successfully pushed to $SSH_URL!"

elif [ "$choice" == "3" ]; then
    gh auth login
    gh repo create "${GITHUB_USER}/${REPO_NAME}" --public --source=. --remote=origin --push || git push -u origin main
    echo ""
    echo "✅ Successfully pushed via GitHub CLI!"
else
    echo "Invalid choice. Exiting."
    exit 1
fi
