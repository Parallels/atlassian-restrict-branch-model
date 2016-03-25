package com.parallels.bitbucket.plugins.restrictbranchmodel;

import com.atlassian.bitbucket.hook.*;
import com.atlassian.bitbucket.hook.repository.*;

import com.atlassian.bitbucket.repository.*;
import com.atlassian.bitbucket.branch.model.*;

import com.atlassian.bitbucket.i18n.I18nService;

import java.util.Collection;
import java.util.regex.*;

import com.google.common.base.Joiner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestrictBranchModelPreReceiveHook
    implements PreReceiveRepositoryHook {
    private static final Logger log = LoggerFactory.getLogger(
        PreReceiveRepositoryHook.class);

    private final I18nService i18nService;

    private RepositoryService repoService;
    private BranchModelService branchModelService;

    public RestrictBranchModelPreReceiveHook(
        I18nService i18nService,
        RepositoryService repoService,
        BranchModelService branchModelService
    ) {
        this.i18nService = i18nService;
        this.repoService = repoService;
        this.branchModelService = branchModelService;
    }

    /**
     * Restrict development to repository's branching model.
     */
    @Override
    public boolean onReceive(
        RepositoryHookContext context,
        Collection<RefChange> refChanges,
        HookResponse hookResponse
    ) {
        Repository repo = context.getRepository();

        // Empty repository does not have a branching model
        if (repoService.isEmpty(repo)) {
            return true;
        }

        BranchModel branchModel = this.branchModelService.getModel(repo);

        if (branchModel == null) {
            log.error("Branching model is not defined for repository ID={}", repo.getId());
            hookResponse.err().printf("error: %s\n", i18nService.getMessage(
                "com.parallels.bitbucket.plugins.restrictbranchmodel.model-not-defined",
                repo.getName()));
            return true;
        }

        // Check if added refs conform to the repository's branching model

        // Indicates whether the hook allows the push to continue
        Boolean permit = true;

        for (RefChange refChange : refChanges) {
            MinimalRef ref = refChange.getRef();

            // Indicated whether the ref conforms to the repository's branching model
            Boolean conforms = false;

            if (refChange.getType() == RefChangeType.ADD) {
                if (ref.getType() != StandardRefType.BRANCH) {
                    // ref is not branch
                    continue;
                }
                log.debug("Detected new branch {}", ref.toString());

                // Cannot use BranchClassifier.getType() because it wants a Branch instance,
                // not SimpleMinimalRef (which is refChange.getRef())
                // Classify manually
                for (BranchType branchType : branchModel.getTypes()) {
                    String prefix = PluginUtils.getBranchTypePrefix(branchType);

                    if (prefix == null) {
                        log.error("getBranchTypePrefix({}) returned null", branchType.toString());
                        hookResponse.err().printf("error: %s\n", i18nService.getMessage(
                            "com.parallels.bitbucket.plugins.restrictbranchmodel.model-classify-failed",
                            ref.getDisplayId()));
                        continue;
                    }

                    if (ref.getDisplayId().startsWith(prefix)) {
                        conforms = true;
                    }
                }

                if (!conforms) {
                    hookResponse.err().printf("error: %s\n", i18nService.getMessage(
                        "com.parallels.bitbucket.plugins.restrictbranchmodel.branch-creation-declined",
                        ref.getDisplayId()));

                    String prefixList = Joiner.on(", ").join(PluginUtils.getBranchTypePrefixList(branchModel));
                    hookResponse.err().printf("error: %s\n", i18nService.getMessage(
                        "com.parallels.bitbucket.plugins.restrictbranchmodel.branch-prefix-list",
                        prefixList));

                    permit = false;
                }
            }
        }

        return permit;
    }

}
