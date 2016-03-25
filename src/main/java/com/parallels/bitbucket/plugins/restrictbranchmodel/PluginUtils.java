package com.parallels.bitbucket.plugins.restrictbranchmodel;

import com.atlassian.bitbucket.branch.model.*;

import java.util.ArrayList;
import java.util.List;

import java.util.regex.*;

public class PluginUtils {

    public static String getBranchTypePrefix(
        BranchType branchType
    ) {
        Pattern prefixPattern = Pattern.compile("prefix=(.*?(?=\\s*,\\s*|\\s*}))");

        // Parse user-defined branch prefix from branchType.toString()
        // SimpleInternalBranchType{id=FEATURE, displayName=Feature, prefix=feature/}
        // No API to retrieve branch prefix, see: https://jira.atlassian.com/browse/BSERV-8167
        Matcher matcher = prefixPattern.matcher(branchType.toString());

        if (!matcher.find()) {
            return null;
        }

        return matcher.group(1);
    }

    public static List getBranchTypePrefixList(
        BranchModel branchModel
    ) {
        List<String> branchTypePrefixList = new ArrayList<String>();

        for (BranchType branchType : branchModel.getTypes()) {
            branchTypePrefixList.add(getBranchTypePrefix(branchType));
        }

        return branchTypePrefixList;
    }

}
