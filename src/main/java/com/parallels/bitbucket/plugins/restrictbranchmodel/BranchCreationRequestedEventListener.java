package com.parallels.bitbucket.plugins.restrictbranchmodel;

import com.atlassian.bitbucket.repository.Repository;

import com.atlassian.bitbucket.hook.repository.*;
import com.atlassian.bitbucket.branch.model.*;

import com.atlassian.bitbucket.user.SecurityService;
import com.atlassian.bitbucket.util.UncheckedOperation;
import com.atlassian.bitbucket.permission.Permission;

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
    private final SecurityService securityService;
    private RepositoryHookService repositoryHookService;
    private BranchModelService branchModelService;

    public BranchCreationRequestedEventListener(
        I18nService i18nService,
        SecurityService securityService,
        RepositoryHookService repositoryHookService,
        BranchModelService branchModelService
    ) {
        this.i18nService = i18nService;
        this.securityService = securityService;
        this.repositoryHookService = repositoryHookService;
        this.branchModelService = branchModelService;
    }

    @EventListener
    public void onBranchCreationRequested(BranchCreationRequestedEvent event) {
        // Check if a branch created via UI/REST conforms
        // to the repository's branching model
        // Run only if the corresponding pre-receive hook is enabled

        final Repository repository = event.getRepository();

        final RepositoryHook repositoryHook = securityService.withPermission(Permission.REPO_ADMIN, "Get hook configuration").call(
            new UncheckedOperation<RepositoryHook>() {
                public RepositoryHook perform() {
                    return repositoryHookService.getByKey(repository, HOOK_KEY);
                }
            });

        if (repositoryHook == null) {
            log.error("Hook {} not found for repository ID={}", HOOK_KEY,
                repository.getId());
            return;
        }

        if (!repositoryHook.isEnabled()) {
            log.info("Hook {} is disabled", HOOK_KEY);
            return;
        }

        BranchModel branchModel = this.branchModelService.getModel(repository);

        if (branchModel == null) {
            log.error("Branching model is not configured for repository ID={}",
                repository.getId());
            return;
        }

        String prefixList = Joiner.on(", ").join(PluginUtils.getBranchTypePrefixList(branchModel));

        if (branchModel.getClassifier().getType(event.getBranch()) == null) {
            event.cancel(i18nService.createKeyedMessage("com.parallels.bitbucket.plugins.restrictbranchmodel.branch-creation-declined",
                event.getBranch().getDisplayId()));

            event.cancel(i18nService.createKeyedMessage("com.parallels.bitbucket.plugins.restrictbranchmodel.branch-prefix-list",
                prefixList));

            log.warn("Cancel creating branch {}", event.getBranch().getDisplayId());
        }
    }
}
