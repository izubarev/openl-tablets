package org.openl.rules.webstudio.web.diff;

import org.openl.rules.diff.tree.DiffStatus;
import org.openl.rules.diff.tree.DiffTreeNode;

public class PropertyNode extends TreeNode {
    private final String title;

    public PropertyNode(DiffTreeNode node, String title) {
        super(node, true);
        this.title = title;
    }

    // @Override
    @Override
    public DiffStatus getStatus() {
        return DiffStatus.DIFFERS;
    }

    // @Override
    @Override
    public String getIcon() {
        // TODO use UiConst.ICON_DIFF_DIFFERS
        return "/webresource/images/diff/propmodified.gif";
    }

    // @Override
    @Override
    public String getName() {
        return title;
    }

    // @Override
    @Override
    public String getType() {
        return "property";
    }
}
