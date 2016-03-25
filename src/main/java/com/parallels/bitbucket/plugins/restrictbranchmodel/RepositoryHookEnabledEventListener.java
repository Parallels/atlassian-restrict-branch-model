package com.parallels.bitbucket.plugins.restrictbranchmodel;

import com.atlassian.bitbucket.branch.model.BranchModel;
import com.atlassian.bitbucket.branch.model.BranchModelService;

import com.atlassian.bitbucket.repository.*;

import com.atlassian.bitbucket.event.hook.RepositoryHookEnabledEvent;
import com.atlassian.event.api.EventListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryHookEnabledEventListener {
    private static final Logger log = LoggerFactory.getLogger(
        RepositoryHookEnabledEvent.class);

    private static final String HOOK_KEY = 
        "com.parallels.bitbucket.plugins.restrictbranchmodel:restrict-branch-model-pre-receive-hook";

    private RepositoryService repoService;
    private BranchModelService branchModelService;

    public RepositoryHookEnabledEventListener(
        RepositoryService repoService,
        BranchModelService branchModelService
    ) {
        this.repoService = repoService;
        this.branchModelService = branchModelService;
    }

    @EventListener
    public void onRepositoryHookEnabled(RepositoryHookEnabledEvent event) {
        // Warn if the hook is enabled before the branching model
        // is configured for the repository

        String hookKey = event.getRepositoryHookKey();

        if (!hookKey.equals(HOOK_KEY)) {
            return;
        }

        Repository repo = event.getRepository();

        // Empty repository does not have a branching model
        if (repoService.isEmpty(repo)) {
            return;
        }

        BranchModel branchModel = branchModelService.getModel(repo);

        if (branchModel == null) {
            log.error("Branching model is not defined for repository ID={}",
                event.getRepository().getId());
            // TODO Render a pop-up notification
        }
    }

}
