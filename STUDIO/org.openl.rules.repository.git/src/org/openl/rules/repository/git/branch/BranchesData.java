package org.openl.rules.repository.git.branch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BranchesData {
    private Map<String, List<String>> projectBranches = new HashMap<>();
    private List<BranchDescription> descriptions = new ArrayList<>();

    public void copyFrom(BranchesData copy) {
        this.projectBranches = copy.projectBranches;
        this.descriptions = copy.descriptions;
    }

    public Map<String, List<String>> getProjectBranches() {
        return projectBranches;
    }

    public void setProjectBranches(Map<String, List<String>> projectBranches) {
        this.projectBranches = projectBranches == null ? new HashMap<>() : projectBranches;
    }

    public List<BranchDescription> getDescriptions() {
        return descriptions;
    }

    public void setDescriptions(List<BranchDescription> descriptions) {
        this.descriptions = descriptions == null ? new ArrayList<>() : descriptions;
    }

    public void addBranch(String projectPath, String branch, String commit) {
        Map<String, List<String>> branches = getProjectBranches();
        List<String> projectBranches = branches.computeIfAbsent(projectPath, k -> new ArrayList<>());

        if (!projectBranches.contains(branch)) {
            projectBranches.add(branch);
        }

        if (commit != null && descriptions.stream().noneMatch(b -> b.getName().equals(branch))) {
            descriptions.add(new BranchDescription(branch, commit));
        }
    }

    public boolean removeBranch(String projectPath, String branch) {
        Map<String, List<String>> branches = getProjectBranches();
        if (projectPath == null) {
            // Remove the branch from all mappings.
            boolean removed = false;
            for (List<String> projectBranchesList : branches.values()) {
                removed |= projectBranchesList.remove(branch);
            }

            descriptions.removeIf(b -> b.getName().equals(branch));

            return removed;
        } else {
            // Remove branch mapping for specific project only.
            List<String> projectBranchesList = branches.get(projectPath);
            if (projectBranchesList != null) {
                return projectBranchesList.remove(branch);
            } else {
                return false;
            }
        }
    }

}
