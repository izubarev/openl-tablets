package org.openl.rules.table.actions;

import org.openl.rules.table.GridRegion;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;

public class GridRegionAction implements IUndoableGridTableAction {

    public enum ActionType {
        MOVE,
        EXPAND
    }

    private final IGridRegion region;
    private final ActionType actionType;
    private final boolean isInsert;
    private final boolean isColumns;
    private final int nRowsOrColumns;

    public GridRegionAction(IGridRegion region,
            boolean isColumns,
            boolean isInsert,
            ActionType actionType,
            int nRowsOrColumns) {
        this.region = region;
        this.actionType = actionType;
        this.isColumns = isColumns;
        this.isInsert = isInsert;
        this.nRowsOrColumns = nRowsOrColumns;
    }

    @Override
    public void doAction(IGridTable table) {
        if (ActionType.EXPAND.equals(actionType)) {
            resizeRegion(isInsert, isColumns, nRowsOrColumns, region);
        } else if (ActionType.MOVE.equals(actionType)) {
            moveRegion(isInsert, isColumns, nRowsOrColumns, region);
        }
    }

    @Override
    public void undoAction(IGridTable table) {
        if (ActionType.EXPAND.equals(actionType)) {
            resizeRegion(!isInsert, isColumns, nRowsOrColumns, region);
        } else if (ActionType.MOVE.equals(actionType)) {
            moveRegion(!isInsert, isColumns, nRowsOrColumns, region);
        }
    }

    public void resizeRegion(boolean isInsert, boolean isColumns, int rowsOrColumns, IGridRegion r) {
        int inc = isInsert ? rowsOrColumns : -rowsOrColumns;
        if (isColumns) {
            ((GridRegion) r).setRight(r.getRight() + inc);
        } else {
            ((GridRegion) r).setBottom(r.getBottom() + inc);
        }
    }

    public void moveRegion(boolean isInsert, boolean isColumns, int rowsOrColumns, IGridRegion r) {
        int inc = isInsert ? rowsOrColumns : -rowsOrColumns;
        if (isColumns) {
            ((GridRegion) r).setLeft(r.getLeft() + inc);
            ((GridRegion) r).setRight(r.getRight() + inc);
        } else {
            ((GridRegion) r).setTop(r.getTop() + inc);
            ((GridRegion) r).setBottom(r.getBottom() + inc);
        }
    }
}
