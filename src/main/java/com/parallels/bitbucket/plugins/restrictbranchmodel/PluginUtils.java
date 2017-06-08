package com.parallels.bitbucket.plugins.restrictbranchmodel;

import com.atlassian.bitbucket.branch.model.BranchType;
import com.atlassian.bitbucket.branch.model.BranchModel;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;
import java.util.List;

import static java.util.logging.Level.SEVERE;
import java.util.logging.Logger;


public class PluginUtils {

    private static Logger logger = Logger.getLogger(PluginUtils.class.getName());

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

    public static List<String> getBranchTypePrefixList(
        BranchModel branchModel
    ) {
        List<String> branchTypePrefixList = new ArrayList<String>();

        for (BranchType branchType : branchModel.getTypes()) {
            String p = getBranchTypePrefix(branchType);
            if (p != null) {
                branchTypePrefixList.add(p);
            }
        }

        return branchTypePrefixList;
    }

}
