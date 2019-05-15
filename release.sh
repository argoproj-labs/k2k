#!/bin/bash
###############################################################################
# Script to create a new template release published in Git
# @author: oazmon
###############################################################################
#
# determine the script base directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
BASE_DIR="${SCRIPT_DIR}"
OWNER="argoproj-labs"
REPO="ksonnet2kustomize"
GITHUB_BASEURL="https://github.com/api"
AUTH_TOKEN=` cat $HOME/.ssh/public-git.token `

# Setting script to exit on error
set -e

# -----------------------------------------------------------------------------
# Handle arguments
# -----------------------------------------------------------------------------
new_version=${1}
if [ X"${new_version}" = X"" ]; then
  echo usage: "$0 {version}"
  exit 1
fi

# -----------------------------------------------------------------------------
# Check we are in the right place
# -----------------------------------------------------------------------------
if [ ! -f metadata.yaml ]; then
  echo "ERROR: Wrong current directory or missing 'metadata.yaml' file"
  exit 1
fi

# -----------------------------------------------------------------------------
# Move to project root directory (where script is)
# -----------------------------------------------------------------------------
cd ${BASE_DIR}

# -----------------------------------------------------------------------------
# Get most current version on git master
# -----------------------------------------------------------------------------
git checkout master
git pull

# -----------------------------------------------------------------------------
# Bump the version
# -----------------------------------------------------------------------------
command='s/version: ".*"/version: "'"${new_version}"'"/'
sed -i.sedcopy "${command}" metadata.yaml
if [ "` grep "version: " metadata.yaml `" != "version: \"${new_version}\"" ]; then
  echo ERROR: failed to update version to ${new_version}
  exit 1
fi
rm -f metadata.yaml.sedcopy

git add metadata.yaml
git commit -m "bump version for release"
git push

# -----------------------------------------------------------------------------
# Create release
# -----------------------------------------------------------------------------
release_id=` curl -s -X POST \
     -H "Authorization: token ${AUTH_TOKEN}" \
     -H "Content-Type: application/json" \
     -d "{
          \"tag_name\": \"v${new_version}\",
          \"target_commitish\": \"master\",
          \"name\": \"v${new_version}\",
          \"body\": \"Ksonnet 2 Kustomize Release v${new_version}\",
          \"draft\": false,
          \"prerelease\": false
        } " \
     ${GITHUB_BASEURL}/v3/repos/${OWNER}/${REPO}/releases |
jq '.id' `
if [ X"${release_id}" = X"" ]; then
  echo ERROR: failed to create release version ${new_version} in Github
  exit 1
fi

# -----------------------------------------------------------------------------
# Create k2k.jar and attach to release
# -----------------------------------------------------------------------------
mvn clean install
curl -s -X POST \
     -H "Authorization: token ${AUTH_TOKEN}" \
     -H "Content-Type: application/zip" \
     --data-binary @target/k2k-0.0.1-SNAPSHOT.jar \
     ${GITHUB_BASEURL}/uploads/repos/${OWNER}/${REPO}/releases/${release_id}/assets?name=k2k.jar

# -----------------------------------------------------------------------------
# Attach metadata.yaml to release
# -----------------------------------------------------------------------------
curl -s -X POST \
     -H "Authorization: token ${AUTH_TOKEN}" \
     -H "Content-Type: text/yaml" \
     --data-binary @metadata.yaml \
     ${GITHUB_BASEURL}/uploads/repos/${OWNER}/${REPO}/releases/${release_id}/assets?name=metadata.yaml

# >>>>>> EOF release.sh <<<<<<
