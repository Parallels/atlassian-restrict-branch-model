package com.parallels.bitbucket.plugins.restrictbranchmodel;

import com.atlassian.bitbucket.hook.repository.*;
import com.atlassian.bitbucket.branch.model.*;

import com.atlassian.bitbucket.event.branch.BranchCreationRequestedEvent;
import com.atlassian.event.api.EventListener;

import com.atlassian.bitbucket.i18n.I18nService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;

public class BranchCreationRequestedEventListener {
    private static final Logger log = LoggerFactory.getLogger(
        BranchCreationRequestedEvent.class);

    private static final String HOOK_KEY =
        "com.parallels.bitbucket.plugins.restrictbranchmodel:restrict-branch-model-pre-receive-hook";

    private final I18nService i18nService;
    private RepositoryHookService repositoryHookService;
    private BranchModelService branchModelService;

    public BranchCreationRequestedEventListener(
        I18nService i18nService,
        RepositoryHookService repositoryHookService,
        BranchModelService branchModelService
    ) {
        this.i18nService = i18nService;
        this.repositoryHookService = repositoryHookService;
        this.branchModelService = branchModelService;
    }

    @EventListener
    public void onBranchCreationRequested(BranchCreationRequestedEvent event) {
        // Check if a branch created via UI/REST conforms
        // to the repository's branching model
        // Run only if the corresponding pre-receive hook is enabled
        RepositoryHook repositoryHook =
            repositoryHookService.getByKey(event.getRepository(), HOOK_KEY);

        if (repositoryHook == null) {
            log.error("Hook {} not found for repository ID={}", HOOK_KEY,
                event.getRepository().getId());
            return;
        }

        if (!repositoryHook.isEnabled()) {
            log.info("Hook {} is disabled", HOOK_KEY);
            return;
        }

        BranchModel branchModel = this.branchModelService.getModel(event.getRepository());

        if (branchModel == null) {
            log.error("Branching model is not configured for repository ID={}",
                event.getRepository().getId());
            return;
        }

        String prefixList = Joiner.on(", ").join(PluginUtils.getBranchTypePrefixList(branchModel));

        if (branchModel.getClassifier().getType(event.getBranch()) == null) {
            event.cancel(i18nService.createKeyedMessage("com.parallels.bitbucket.plugins.restrictbranchmodel.branch-creation-declined",
                event.getBranch().getDisplayId()));

            event.cancel(i18nService.createKeyedMessage("com.parallels.bitbucket.plugins.restrictbranchmodel.branch-prefix-list",
                prefixList));
        }
    }
}
