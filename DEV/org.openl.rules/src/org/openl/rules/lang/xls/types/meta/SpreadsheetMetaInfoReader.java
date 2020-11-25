package org.openl.rules.lang.xls.types.meta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.binding.MethodUtil;
import org.openl.binding.impl.NodeType;
import org.openl.binding.impl.NodeUsage;
import org.openl.binding.impl.SimpleNodeUsage;
import org.openl.rules.calc.SpreadsheetBoundNode;
import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.table.CellKey;
import org.openl.rules.table.ICell;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.CompositeMethod;
import org.openl.types.java.JavaOpenClass;

public class SpreadsheetMetaInfoReader extends AMethodMetaInfoReader<SpreadsheetBoundNode> {
    // Typically too few header cells have any meta info. Header may not contain any cell at all.
    // It's more convenient to store existing header meta info in the Map.
    // Remove it in the future: retrieve header meta info from compiled data.
    private final Map<CellKey, CellMetaInfo> headerMetaInfo = new HashMap<>();

    public SpreadsheetMetaInfoReader(SpreadsheetBoundNode boundNode) {
        super(boundNode);
    }

    @Override
    public CellMetaInfo getBodyMetaInfo(int row, int col) {
        SpreadsheetCell[][] cells = getBoundNode().getCells();
        if (cells == null || cells.length == 0 || cells[0].length == 0) {
            return null;
        }
        ICell firstCell = cells[0][0].getSourceCell();

        int r = row - firstCell.getAbsoluteRow();
        int c = col - firstCell.getAbsoluteColumn();

        if (r < 0 || c < 0) {
            return headerMetaInfo.get(CellKey.CellKeyFactory.getCellKey(col, row));
        }

        SpreadsheetCell spreadsheetCell = findCell(cells, row, col);
        if (spreadsheetCell == null || spreadsheetCell.isEmpty()) {
            return null;
        }

        ICell sourceCell = spreadsheetCell.getSourceCell();
        IOpenClass type = spreadsheetCell.getType();

        String stringValue = sourceCell.getStringValue();
        if (stringValue != null) {
            List<NodeUsage> nodeUsages = null;
            if (stringValue.startsWith("=") || (stringValue.startsWith("{") && stringValue.endsWith("}"))) {
                nodeUsages = new ArrayList<>();
                int from = stringValue.indexOf('=');
                if (from >= 0 && type != null) {
                    String description = "Cell type: " + MethodUtil.printType(type);
                    nodeUsages.add(new SimpleNodeUsage(from, from, description, null, NodeType.OTHER));
                }

                IOpenMethod method = spreadsheetCell.getMethod();
                if (method instanceof CompositeMethod) {
                    int startIndex = from + 1;
                    List<NodeUsage> parsedNodeUsages = MetaInfoReaderUtils
                        .getNodeUsages((CompositeMethod) method, stringValue.substring(startIndex), startIndex);
                    nodeUsages.addAll(parsedNodeUsages);
                }
            }
            boolean isRet = false;
            if (getBoundNode().getComponentsBuilder().isExistsReturnHeader()) {
                isRet = getBoundNode().getComponentsBuilder().getReturnHeaderDefinition().isReturnCell(spreadsheetCell);
            }

            return new CellMetaInfo(JavaOpenClass.STRING, false, nodeUsages, isRet);
        }

        return null;
    }

    /**
     * Is invoked from binder
     */
    public void addHeaderMetaInfo(int row, int col, CellMetaInfo metaInfo) {
        headerMetaInfo.put(CellKey.CellKeyFactory.getCellKey(col, row), metaInfo);
    }

    private SpreadsheetCell findCell(SpreadsheetCell[][] cells, int row, int col) {
        ICell firstCell = cells[0][0].getSourceCell();
        int r = row - firstCell.getAbsoluteRow();
        int c = col - firstCell.getAbsoluteColumn();

        if (r >= cells.length) {
            r = cells.length - 1;
        }

        if (c >= cells[0].length) {
            c = cells[0].length - 1;
        }

        // Optimistic approach: check that we already found needed cell
        SpreadsheetCell spreadsheetCell = cells[r][c];
        ICell sourceCell = spreadsheetCell.getSourceCell();
        if (sourceCell.getAbsoluteRow() == row && sourceCell.getAbsoluteColumn() == col) {
            return spreadsheetCell;
        }

        // Exist some merged cells.
        // Search in previous columns.
        while (col < sourceCell.getAbsoluteColumn() && c > 0) {
            c--;
            spreadsheetCell = cells[r][c];
            sourceCell = spreadsheetCell.getSourceCell();
        }

        // Search in previous rows.
        while (row < sourceCell.getAbsoluteRow() && r > 0) {
            r--;
            spreadsheetCell = cells[r][c];
            sourceCell = spreadsheetCell.getSourceCell();
        }

        // Check that found needed cell
        if (sourceCell.getAbsoluteRow() == row && sourceCell.getAbsoluteColumn() == col) {
            return spreadsheetCell;
        }

        return null;
    }
}
