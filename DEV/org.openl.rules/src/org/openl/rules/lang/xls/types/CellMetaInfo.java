package org.openl.rules.lang.xls.types;

import java.util.List;

import org.openl.binding.impl.NodeUsage;
import org.openl.types.IOpenClass;
import org.openl.util.CollectionUtils;

public class CellMetaInfo {
    private static final int MULTI = 1;
    private static final int RETURN_CELL = 1 << 1;

    private final IOpenClass domain;
    private int flags;
    private List<? extends NodeUsage> usedNodes;

    public CellMetaInfo(IOpenClass domain, boolean multiValue) {
        this(domain, multiValue, null);
    }

    public CellMetaInfo(IOpenClass domain, boolean multiValue, List<? extends NodeUsage> usedNodes) {
        this(domain, multiValue, usedNodes, false);
    }

    public CellMetaInfo(IOpenClass domain,
            boolean multiValue,
            List<? extends NodeUsage> usedNodes,
            boolean returnHeader) {
        this.domain = domain;
        this.usedNodes = usedNodes;
        if (multiValue) {
            this.flags = this.flags | MULTI;
        }
        if (returnHeader) {
            this.flags = this.flags | RETURN_CELL;
        }
    }

    public boolean isReturnCell() {
        return (flags & RETURN_CELL) > 0;
    }

    public IOpenClass getDataType() {
        return domain;
    }

    public boolean isMultiValue() {
        return (flags & MULTI) > 0;
    }

    public List<? extends NodeUsage> getUsedNodes() {
        return usedNodes;
    }

    public void setUsedNodes(List<? extends NodeUsage> usedNodes) {
        this.usedNodes = usedNodes;
    }

    private boolean hasNodeUsagesInCell() {
        return CollectionUtils.isNotEmpty(getUsedNodes());
    }

    public static boolean isCellContainsNodeUsages(CellMetaInfo metaInfo) {
        return metaInfo != null && metaInfo.hasNodeUsagesInCell();
    }
}
