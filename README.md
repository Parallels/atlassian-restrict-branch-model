# Restrict Development to Branch Model

[![Build Status](https://travis-ci.org/Parallels/atlassian-restrict-branch-model.svg?branch=master)](https://travis-ci.org/Parallels/atlassian-restrict-branch-model)

[Atlassian Bitbucket Server](https://www.atlassian.com/software/bitbucket/server) Plugin

Deny creating branches that do not conform to repository's branch model.

Decline `git push` that creates a branch outside of repository's branching model and
cancel creating such branches via UI/REST.

* Configure repository's branching model

Repository Settings (the cog) -> Workflow (Branching Model) -> Enable

* Enable repository's hook

Repository Settings (the cog) -> Workflow (Hooks) -> Restrict Development to Branch Model -> Enable


## Build and pack the plugin
```
atlas-compile
atlas-package
```

Run Bitbucket Server with the plugin installed (Atlassian SDK):
```
atlas-run
```
