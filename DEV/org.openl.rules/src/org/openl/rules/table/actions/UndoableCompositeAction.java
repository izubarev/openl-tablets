/**
 * Created Feb 17, 2007
 */
package org.openl.rules.table.actions;

import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

import org.openl.rules.table.IGridTable;

/**
 * @author snshor
 *
 */
public class UndoableCompositeAction implements IUndoableGridTableAction {

    final List<IUndoableGridTableAction> actions;

    public UndoableCompositeAction(List<IUndoableGridTableAction> actions) {
        this.actions = actions;
    }

    public UndoableCompositeAction(IUndoableGridTableAction... gridActions) {
        this.actions = Arrays.asList(gridActions);
    }

    @Override
    public void doAction(IGridTable table) {
        for (IUndoableGridTableAction action : actions) {
            action.doAction(table);
        }
    }

    @Override
    public void undoAction(IGridTable table) {
        for (ListIterator<IUndoableGridTableAction> iter = actions.listIterator(actions.size()); iter.hasPrevious();) {
            IUndoableGridTableAction action = iter.previous();
            action.undoAction(table);
        }
    }

}
