package com.parallels.bitbucket.plugins.restrictbranchmodel;

import com.atlassian.bitbucket.hook.repository.PreRepositoryHook;
import com.atlassian.bitbucket.hook.repository.PreRepositoryHookContext;
import com.atlassian.bitbucket.hook.repository.RepositoryHookRequest;
import com.atlassian.bitbucket.hook.repository.RepositoryHookResult;
import com.atlassian.bitbucket.hook.repository.RepositoryHookTrigger;
import com.atlassian.bitbucket.hook.repository.StandardRepositoryHookTrigger;

import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.repository.RepositoryService;
import com.atlassian.bitbucket.repository.RefChange;
import com.atlassian.bitbucket.repository.MinimalRef;
import com.atlassian.bitbucket.repository.RefChangeType;
import com.atlassian.bitbucket.repository.StandardRefType;

import com.atlassian.bitbucket.branch.model.BranchModelService;
import com.atlassian.bitbucket.branch.model.BranchModel;
import com.atlassian.bitbucket.i18n.I18nService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;

import java.util.ArrayList;
import java.util.List;
import com.google.common.base.Joiner;


@Component("restrictBranchModelPreReceiveHook")
public class RestrictBranchModelPreReceiveHook
    implements PreRepositoryHook<RepositoryHookRequest> {

    private final I18nService i18nService;
    private final RepositoryService repoService;
    private final BranchModelService branchModelService;

    @Autowired
    public RestrictBranchModelPreReceiveHook(
        @ComponentImport I18nService i18nService,
        @ComponentImport RepositoryService repoService,
        @ComponentImport BranchModelService branchModelService
    ) {
        this.i18nService = i18nService;
        this.repoService = repoService;
        this.branchModelService = branchModelService;
    }

    /**
     * Restrict development to repository's branching model.
     */
    @Override
    public RepositoryHookResult preUpdate(PreRepositoryHookContext context,
        RepositoryHookRequest request) {

        RepositoryHookTrigger t = request.getTrigger();
        if (t != StandardRepositoryHookTrigger.BRANCH_CREATE && t != StandardRepositoryHookTrigger.REPO_PUSH) {
            return RepositoryHookResult.accepted();
        }

        Repository repo = request.getRepository();

        // Cannot construct branch model for empty repository
        if (repoService.isEmpty(repo)) {
            return RepositoryHookResult.accepted();
        }

        //@Nonnull
        BranchModel branchModel = this.branchModelService.getModel(repo);
        // No branch types configured
        if (branchModel.getTypes().isEmpty()) {
            return RepositoryHookResult.accepted();
        }

        List<String> prefixList = PluginUtils.getBranchTypePrefixList(branchModel);
        List<String> refsDeclined = new ArrayList<String>();

        for (RefChange refChange : request.getRefChanges()) {
            // Indicates whether the ref can be classified by the branch model
            Boolean classified = false;

            if (refChange.getType() == RefChangeType.ADD) {
                MinimalRef ref = refChange.getRef();

                if (ref.getType() != StandardRefType.BRANCH) {
                    // ref is not branch
                    continue;
                }

                // Cannot use BranchClassifier.getType() because it wants a Branch instance,
                // and not SimpleMinimalRef (which is refChange.getRef())
                // So, have to classify manually

                for (String prefix : prefixList) {
                    if (ref.getDisplayId().startsWith(prefix)) {
                        classified = true;
                        break;
                    }
                }

                if (!classified) {
                    refsDeclined.add(ref.getDisplayId());
                }
            }
        }

        if (!refsDeclined.isEmpty()) {
            return RepositoryHookResult.rejected(
                i18nService.getMessage("com.parallels.bitbucket.plugins.restrictbranchmodel.branch-creation-declined", Joiner.on(", ").join(refsDeclined)),
                i18nService.getMessage("com.parallels.bitbucket.plugins.restrictbranchmodel.branch-prefix-list", Joiner.on(", ").join(prefixList))
            );
        }

        return RepositoryHookResult.accepted();
    }
}
