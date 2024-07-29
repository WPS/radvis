#!/usr/bin/env bash

## Initialisierungsskript zum Konfigurieren des lokalen Git-Repositories
## Es wird durch das npm prepare-Skript ausgeführt.

GIT=$(type -P git)
## Überprüft ob git installiert ist
if [ "$GIT" ] ; then
  GIT_HOOK_DIR="./etc/git-hooks/"
  if [ "$GIT_HOOK_DIR" != "$($GIT config core.hooksPath)" ]; then
    echo "Set git hooks path to $GIT_HOOK_DIR"
    $GIT config core.hooksPath $GIT_HOOK_DIR
    echo "\"Installed\" git hooks"
  else
    echo "Git hooks already set"
  fi
else
  echo "Git installation not found via \"type -P git\"."
fi
