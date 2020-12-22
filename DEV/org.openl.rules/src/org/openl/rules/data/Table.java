package org.openl.rules.data;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.openl.binding.IBindingContext;
import org.openl.exception.OpenLCompilationException;
import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.OpenlToolAdaptor;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.LogicalTableHelper;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.rules.table.xls.XlsUrlParser;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
import org.openl.util.BiMap;
import org.openl.util.MessageUtils;
import org.openl.vm.IRuntimeEnv;

public class Table implements ITable {

    private ILogicalTable logicalTable;
    private ITableModel dataModel;

    private String tableName;
    private TableSyntaxNode tableSyntaxNode;

    private Object dataArray;
    private List<DatatypeArrayMultiRowElementContext> dataContextCache;

    private BiMap<Integer, Object> rowIndexMap;
    private BiMap<Integer, String> primaryIndexMap;
    private Map<Integer, Integer> dataIdxToTableRowNum;
    private XlsNodeTypes xlsNodeType;
    private String uri;

    public Table(ITableModel dataModel, ILogicalTable data) {
        this.dataModel = dataModel;
        this.logicalTable = data;
    }

    public Table(String tableName, TableSyntaxNode tsn) {
        this.tableName = tableName;
        this.tableSyntaxNode = tsn;
        this.xlsNodeType = tsn.getNodeType();
        this.uri = tsn.getUri();
    }

    @Override
    public void clearOddDataForExecutionMode() {
        this.tableSyntaxNode = null;
    }

    @Override
    public String getUri() {
        return uri;
    }

    @Override
    public XlsNodeTypes getXlsNodeType() {
        return xlsNodeType;
    }

    @Override
    public void setData(ILogicalTable dataWithHeader) {
        logicalTable = dataWithHeader;
    }

    @Override
    public ILogicalTable getData() {
        return logicalTable;
    }

    @Override
    public void setModel(ITableModel dataModel) {
        this.dataModel = dataModel;
    }

    @Override
    public String getColumnDisplay(int n) {
        return dataModel.getDescriptor(n).getDisplayName();
    }

    @Override
    public int getColumnIndex(String columnName) {
        for (ColumnDescriptor descriptor : dataModel.getDescriptors()) {
            if (descriptor.getName().equals(columnName)) {
                return descriptor.getColumnIdx();
            }
        }

        return -1;
    }

    @Override
    public String getColumnName(int n) {
        ColumnDescriptor columnDescriptor = dataModel.getDescriptor(n);
        return columnDescriptor != null ? columnDescriptor.getName() : null;
    }

    @Override
    public IOpenClass getColumnType(int n) {
        ColumnDescriptor descriptor = dataModel.getDescriptor(n);

        if (!descriptor.isConstructor()) {
            return descriptor.getType();
        }

        return null;
    }

    @Override
    public Object getData(int row) {
        return Array.get(dataArray, row);
    }

    @Override
    public Object getDataArray() {
        return dataArray;
    }

    @Override
    public ITableModel getDataModel() {
        return dataModel;
    }

    @Override
    public IGridTable getHeaderTable() {
        return logicalTable.getRow(0).getSource();
    }

    @Override
    public String getName() {
        return tableName;
    }

    @Override
    public int getNumberOfColumns() {
        return dataModel.getDescriptors().length;
    }

    @Override
    public ColumnDescriptor getColumnDescriptor(int i) {
        return dataModel.getDescriptor(i);
    }

    @Override
    public int getNumberOfRows() {
        return logicalTable.getHeight() - 1;
    }

    @Override
    public synchronized String getPrimaryIndexKey(int row) {
        if (primaryIndexMap == null) {
            return null;
        }
        return primaryIndexMap.get(row);
    }

    @Override
    public Integer getRowIndex(Object target) {
        return rowIndexMap.getKey(target);
    }

    @Override
    public IGridTable getRowTable(int row) {
        return logicalTable.getRow(row + 1).getSource();
    }

    @Override
    public int getSize() {
        return Array.getLength(dataArray);
    }

    @Override
    public TableSyntaxNode getTableSyntaxNode() {
        return tableSyntaxNode;
    }

    @Override
    public Object getValue(int col, int row) {
        int startRows = getStartRowForData();
        int idx = row - startRows;
        Object rowObject = rowIndexMap == null ? Array.get(dataArray, idx) : rowIndexMap.get(idx);

        return dataModel.getDescriptor(col).getColumnValue(rowObject);
    }

    @Override
    public Map<String, Integer> makeUniqueIndex(int colIdx, IBindingContext cxt) {
        Map<String, Integer> index = new HashMap<>();

        if (dataIdxToTableRowNum == null || dataIdxToTableRowNum.isEmpty()) {
            return Collections.emptyMap();
        }

        for (Map.Entry<Integer, Integer> entry : dataIdxToTableRowNum.entrySet()) {
            IGridTable gridTable = logicalTable.getSubtable(colIdx, entry.getValue(), 1, 1).getSource();
            String key = gridTable.getCell(0, 0).getStringValue();

            if (key == null) {
                SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(MessageUtils.EMPTY_UNQ_IDX_KEY,
                    new GridCellSourceCodeModule(gridTable));
                cxt.addError(error);
                break;
            }

            key = key.trim();

            if (index.containsKey(key)) {
                SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(
                    MessageUtils.getDuplicatedKeyIndexErrorMessage(key),
                    new GridCellSourceCodeModule(gridTable));
                cxt.addError(error);
                break;
            }

            index.put(key, entry.getKey());
        }

        return Collections.unmodifiableMap(index);
    }

    @Override
    public List<Object> getUniqueValues(int colIdx) throws SyntaxNodeException {

        List<Object> values = new ArrayList<>();

        if (dataIdxToTableRowNum == null || dataIdxToTableRowNum.isEmpty()) {
            return Collections.emptyList();
        }

        for (Map.Entry<Integer, Integer> entry : dataIdxToTableRowNum.entrySet()) {

            IGridTable gridTable = logicalTable.getSubtable(colIdx, entry.getValue(), 1, 1).getSource();
            Object value = gridTable.getCell(0, 0).getObjectValue();

            if (value == null) {
                throw SyntaxNodeExceptionUtils.createError(MessageUtils.EMPTY_UNQ_IDX_KEY,
                    new GridCellSourceCodeModule(gridTable));
            }

            if (values.contains(value)) {
                throw SyntaxNodeExceptionUtils.createError(
                    MessageUtils.getDuplicatedKeyIndexErrorMessage(String.valueOf(value)),
                    new GridCellSourceCodeModule(gridTable));
            }

            values.add(value);
        }

        return values;
    }

    @Override
    public void populate(IDataBase dataBase, IBindingContext bindingContext) throws Exception {

        int rows = logicalTable.getHeight();
        int columns = logicalTable.getWidth();

        boolean hasError = validateOnErrors(bindingContext, dataBase, columns);

        if (hasError) {
            return;
        }

        int dataArrayLength = Array.getLength(dataArray);
        for (int i = 0; i < dataArrayLength; i++) {
            IRuntimeEnv env = bindingContext.getOpenL().getVm().getRuntimeEnv();
            Object target = Array.get(dataArray, i);
            env.pushThis(target);

            int rowNum = dataIdxToTableRowNum.get(i);
            // calculate height
            int height;
            if (i + 1 < dataArrayLength) {
                height = dataIdxToTableRowNum.get(i + 1) - rowNum;
            } else {
                height = rows - rowNum;
            }

            DatatypeArrayMultiRowElementContext context = getCachedContext(i);
            if (context == null) {
                context = new DatatypeArrayMultiRowElementContext();
            }
            env.pushLocalFrame(new Object[] { context });
            for (int j = 0; j < columns; j++) {
                ColumnDescriptor descriptor = dataModel.getDescriptor(j);

                if (descriptor instanceof ForeignKeyColumnDescriptor) {
                    ForeignKeyColumnDescriptor fkDescriptor = (ForeignKeyColumnDescriptor) descriptor;

                    if (fkDescriptor.isReference()) {
                        try {
                            if (descriptor.isConstructor()) {
                                target = fkDescriptor.getLiteralByForeignKey(dataModel.getType(),
                                    logicalTable.getSubtable(j, rowNum, 1, height),
                                    dataBase,
                                    bindingContext);
                            } else {
                                fkDescriptor.populateLiteralByForeignKey(target,
                                    logicalTable.getSubtable(j, rowNum, 1, height),
                                    dataBase,
                                    bindingContext,
                                    env);
                            }
                        } catch (SyntaxNodeException e) {
                            bindingContext.addError(e);
                        }
                    }
                }
            }
            env.popLocalFrame();
            env.popThis();
        }
        // clear cache
        dataContextCache = null;
    }

    private boolean validateOnErrors(IBindingContext bindingContext, IDataBase dataBase, int columns) {
        boolean hasError = false;
        // Validation
        for (int j = 0; j < columns; j++) {
            SyntaxNodeException ex = null;
            ColumnDescriptor descriptor = dataModel.getDescriptor(j);
            if (descriptor instanceof ForeignKeyColumnDescriptor) {
                ForeignKeyColumnDescriptor fkDescriptor = (ForeignKeyColumnDescriptor) descriptor;
                if (fkDescriptor.isReference()) {
                    IdentifierNode foreignKeyTable = fkDescriptor.getForeignKeyTable();
                    IdentifierNode foreignKey = fkDescriptor.getForeignKey();
                    String foreignKeyTableName = foreignKeyTable.getIdentifier();
                    ITable foreignTable = dataBase.getTable(foreignKeyTableName);

                    if (foreignTable == null) {
                        String message = MessageUtils.getTableNotFoundErrorMessage(foreignKeyTableName);
                        ex = SyntaxNodeExceptionUtils.createError(message, null, foreignKeyTable);
                    } else {
                        if (foreignKey != null) {
                            String columnName = foreignKey.getIdentifier();
                            int foreignKeyIndex = foreignTable.getColumnIndex(columnName);
                            if (foreignKeyIndex == -1) {
                                String message = MessageUtils.getColumnNotFoundErrorMessage(columnName);
                                ex = SyntaxNodeExceptionUtils.createError(message, null, foreignKey);
                            } else {
                                foreignTable.getColumnDescriptor(foreignKeyIndex)
                                    .getUniqueIndex(foreignTable, foreignKeyIndex, bindingContext);
                            }
                        } else {
                            // we don't have defined PK lets use first key as PK
                            int foreignKeyIndex = 0;
                            ITableModel dataModel = foreignTable.getDataModel();
                            ColumnDescriptor d1 = dataModel.getDescriptors()[0];
                            if (d1.isPrimaryKey()) {
                            } else {
                                ColumnDescriptor firstColDescriptor = dataModel.getDescriptor(0);
                                if (firstColDescriptor.isPrimaryKey()) {
                                    // first column is primary key for another level. So return column index for first
                                    // descriptor
                                    foreignKeyIndex = descriptor.getColumnIdx();
                                }
                                foreignTable.getColumnDescriptor(foreignKeyIndex)
                                    .getUniqueIndex(foreignTable, foreignKeyIndex, bindingContext);

                            }

                            SyntaxNodeException[] errors = bindingContext.getErrors();
                            for (SyntaxNodeException error : errors) {
                                String sourceLocation = error.getSourceLocation();
                                if (sourceLocation != null && foreignTable.getTableSyntaxNode()
                                    .getUriParser()
                                    .intersects(new XlsUrlParser(sourceLocation))) {
                                    String message = MessageUtils
                                        .getForeignTableCompilationErrorsMessage(foreignKeyTableName);
                                    ex = SyntaxNodeExceptionUtils.createError(message, null, foreignKeyTable);
                                }
                            }
                        }
                    }
                }
            }
            if (ex != null) {
                bindingContext.addError(ex);
                hasError = true;
            }
        }
        return hasError;
    }

    @Override
    public void preLoad(OpenlToolAdaptor openlAdapter) throws Exception {
        int rows = logicalTable.getHeight();
        int startRow = getStartRowForData();

        if (tableSyntaxNode.getNodeType() == XlsNodeTypes.XLS_DATA && isSupportMultirow()) {
            // process not merged rows as merged if they have the same value in first column
            List<Object> resultContainer = new ArrayList<>();
            List<DatatypeArrayMultiRowElementContext> dataContexts = new ArrayList<>();

            processMultirowDataTable(resultContainer, openlAdapter, dataContexts, startRow, rows);

            dataArray = Array.newInstance(dataModel.getInstanceClass(), resultContainer.size());
            for (int i = 0; i < resultContainer.size(); i++) {
                Array.set(dataArray, i, resultContainer.get(i));
            }
            this.dataContextCache = Collections.unmodifiableList(dataContexts);
        } else {
            dataArray = Array.newInstance(dataModel.getInstanceClass(), rows - startRow);
            for (int rowNum = startRow; rowNum < rows; rowNum++) {
                processRow(openlAdapter, startRow, rowNum);
            }
        }
    }

    private boolean isSupportMultirow() {
        if (dataModel.getDescriptors().length > 0) {
            for (ColumnDescriptor descriptor : dataModel.getDescriptors()) {
                if (descriptor.isSupportMultirows()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void processMultirowDataTable(List<Object> resultContainer,
            OpenlToolAdaptor openlAdapter,
            List<DatatypeArrayMultiRowElementContext> dataContexts,
            int startRow,
            int rows) throws OpenLCompilationException {

        // group descriptors by KEY
        Map<ColumnDescriptor.ColumnGroupKey, List<ColumnDescriptor>> descriptorGroups = new TreeMap<>();
        for (ColumnDescriptor descriptor : dataModel.getDescriptors()) {
            ColumnDescriptor.ColumnGroupKey key = descriptor.getGroupKey();
            List<ColumnDescriptor> descriptorsByKey = descriptorGroups.computeIfAbsent(key, k -> new ArrayList<>());
            if (descriptor.getField() != null && !descriptor.isReference()) {
                descriptorsByKey.add(descriptor);
            }
        }

        try {
            parseRowsAndPopulateRootLiteral(resultContainer,
                dataContexts,
                new ArrayList<>(descriptorGroups.values()),
                openlAdapter,
                startRow,
                rows);
        } catch (SyntaxNodeException e) {
            openlAdapter.getBindingContext().addError(e);
        }
    }

    private void parseRowsAndPopulateRootLiteral(List<Object> resultContainer,
            List<DatatypeArrayMultiRowElementContext> dataContexts,
            List<List<ColumnDescriptor>> allDescriptors,
            OpenlToolAdaptor openlAdapter,
            int startRow,
            int rows) throws OpenLCompilationException {

        List<ColumnDescriptor> descriptors = allDescriptors.get(0);

        Object[][] rowValues = new Object[rows - startRow][descriptors.size()];
        for (int rowNum = startRow; rowNum < rows; rowNum++) {
            for (int colNum = 0; colNum < descriptors.size(); colNum++) {
                ColumnDescriptor descriptor = descriptors.get(colNum);
                ILogicalTable valuesTable = LogicalTableHelper
                    .make1ColumnTable(logicalTable.getSubtable(descriptor.getColumnIdx(), rowNum, 1, 1));
                Object prevRes = ColumnDescriptor.PREV_RES_EMPTY;
                int width = valuesTable.getSource().getWidth();
                for (int i = 0; i < valuesTable.getSource().getHeight(); i++) {
                    ILogicalTable logicalTable = LogicalTableHelper.make1ColumnTable(
                        LogicalTableHelper.logicalTable(valuesTable.getSource().getSubtable(0, i, width, i + 1))
                            .getSubtable(0, 0, width, 1));
                    Object res = descriptor.parseCellValue(logicalTable, openlAdapter);
                    if (!descriptor.isSameValue(res, prevRes)) {
                        rowValues[rowNum - startRow][colNum] = res;
                        prevRes = res;
                    }
                }
            }
        }

        IRuntimeEnv env = openlAdapter.getOpenl().getVm().getRuntimeEnv();
        for (int rowNum = 0; rowNum < rowValues.length; rowNum++) {
            int height = 1;
            Object[] thisRow = rowValues[rowNum];
            if (thisRow == null) {
                continue;
            }
            Object literal = createLiteral();
            addToRowIndex(rowNum, literal);
            for (int j = rowNum + 1; j < rowValues.length; j++) {
                Object[] nextRow = rowValues[j];
                boolean isSameRow = true;
                for (int k = 0; k < thisRow.length; k++) {
                    isSameRow = descriptors.get(k).isSameValue(nextRow[k], thisRow[k]);
                    if (!isSameRow) {
                        break;
                    }
                }
                if (isSameRow) {
                    rowValues[j] = null;
                    addToRowIndex(j, literal);
                    height++;
                } else {
                    break;
                }
            }

            DatatypeArrayMultiRowElementContext context = new DatatypeArrayMultiRowElementContext();
            env.pushLocalFrame(new Object[] { context });
            env.pushThis(literal);
            try {
                for (List<ColumnDescriptor> allDescriptor : allDescriptors) {
                    parseRowsAndPopulateLiteral(literal, allDescriptor, openlAdapter, env, rowNum + startRow, height);
                }
                bindDataIndexWithTableRowNum(resultContainer.size(), rowNum + startRow);
                resultContainer.add(literal);
                dataContexts.add(context);
            } finally {
                env.popThis();
                env.popLocalFrame();
            }
        }
    }

    private void parseRowsAndPopulateLiteral(Object literal,
            List<ColumnDescriptor> descriptors,
            OpenlToolAdaptor openlAdapter,
            IRuntimeEnv env,
            int rowNum,
            int height) throws OpenLCompilationException {

        if (descriptors.isEmpty()) {
            return;
        }
        DatatypeArrayMultiRowElementContext context = (DatatypeArrayMultiRowElementContext) env.getLocalFrame()[0];

        Object[][] rowValues = null;
        for (int colNum = 0; colNum < descriptors.size(); colNum++) {
            ColumnDescriptor descriptor = descriptors.get(colNum);
            ILogicalTable valuesTable = LogicalTableHelper
                .make1ColumnTable(logicalTable.getSubtable(descriptor.getColumnIdx(), rowNum, 1, height));
            if (rowValues == null) {
                rowValues = new Object[valuesTable.getSource().getHeight()][descriptors.size()];
            }
            int width = valuesTable.getSource().getWidth();
            for (int i = 0; i < valuesTable.getSource().getHeight(); i++) {
                ILogicalTable logicalTable = LogicalTableHelper.make1ColumnTable(
                    LogicalTableHelper.logicalTable(valuesTable.getSource().getSubtable(0, i, width, i + 1))
                        .getSubtable(0, 0, width, 1));
                rowValues[i][colNum] = descriptor.parseCellValue(logicalTable, openlAdapter);
            }
        }

        ColumnDescriptor pkDescriptor = descriptors.get(0);

        Object[] prevRow = null;
        boolean shouldSkipMergingSameValues = !pkDescriptor.isPrimaryKey() && !pkDescriptor
            .isDeclaredClassSupportMultirow();

        for (int i = 0; i < rowValues.length; i++) {
            boolean isSameRow;
            Object[] thisRow = rowValues[i];
            context.setRow(i);
            if (prevRow == null || shouldSkipMergingSameValues) {
                isSameRow = false;
            } else {
                if (pkDescriptor.isPrimaryKey()) {
                    isSameRow = pkDescriptor.isSameValue(thisRow[0], prevRow[0]);
                } else {
                    isSameRow = true;
                    for (int k = 0; k < thisRow.length; k++) {
                        isSameRow = descriptors.get(k).isSameValue(thisRow[k], prevRow[k]);
                        if (!isSameRow) {
                            break;
                        }
                    }
                }
            }
            context.setRowValueIsTheSameAsPrevious(isSameRow);
            for (int k = 0; k < thisRow.length; k++) {
                ColumnDescriptor descriptor = descriptors.get(k);
                Object thisValue = thisRow[k];
                if (descriptor.isValuesAnArray()) {
                    Object currentValue = descriptor.getFieldValue(literal, env);
                    int thisLen = Array.getLength(thisValue);
                    if (currentValue == null || Array.getLength(currentValue) == 0) {
                        descriptor.setFieldValue(literal, thisLen == 0 ? null : thisValue, env);
                    } else if (thisLen != 0) {
                        int currentLen = Array.getLength(currentValue);
                        Object newArray = Array.newInstance(thisValue.getClass().getComponentType(),
                            currentLen + thisLen);
                        System.arraycopy(currentValue, 0, newArray, 0, currentLen);
                        System.arraycopy(thisValue, 0, newArray, currentLen, thisLen);
                        descriptor.setFieldValue(literal, newArray, env);
                    }
                } else {
                    descriptor.setFieldValue(literal, thisValue, env);
                }
            }

            prevRow = thisRow;
        }

    }

    private Object createLiteral() throws OpenLCompilationException {
        if (dataModel.getInstanceClass().isArray()) {
            int dim = 0;
            Class<?> type = dataModel.getInstanceClass();
            while (type.isArray()) {
                type = type.getComponentType();
                dim++;
            }
            return Array.newInstance(type, new int[dim]);
        } else {
            Object literal = dataModel.newInstance();
            if (literal == null) {
                throw new OpenLCompilationException(
                    String.format("Cannot create an instance of '%s'.", dataModel.getName()));
            }
            return literal;
        }
    }

    private void processRow(OpenlToolAdaptor openlAdapter, int startRow, int rowNum) throws OpenLCompilationException {

        boolean constructor = isConstructor();
        Object literal = null;

        int rowIndex = rowNum - startRow;

        if (!constructor) {
            literal = createLiteral();
            addToRowIndex(rowIndex, literal);
        }

        IRuntimeEnv env = openlAdapter.getOpenl().getVm().getRuntimeEnv();
        env.pushLocalFrame(new Object[] { new DatatypeArrayMultiRowElementContext() });
        for (ColumnDescriptor columnDescriptor : dataModel.getDescriptors()) {
            literal = processColumn(columnDescriptor, openlAdapter, constructor, rowNum, literal, env);
        }
        env.popLocalFrame();
        if (literal == null) {
            literal = dataModel.getType().nullObject();
        }

        int idx = rowNum - startRow;
        bindDataIndexWithTableRowNum(idx, rowNum);
        Array.set(dataArray, idx, literal);
    }

    private void bindDataIndexWithTableRowNum(Integer idx, Integer rowNum) {
        if (dataIdxToTableRowNum == null) {
            dataIdxToTableRowNum = new HashMap<>();
        }
        dataIdxToTableRowNum.put(idx, rowNum);
    }

    private Object processColumn(ColumnDescriptor columnDescriptor,
            OpenlToolAdaptor openlAdapter,
            boolean constructor,
            int rowNum,
            Object literal,
            IRuntimeEnv env) throws SyntaxNodeException {

        if (columnDescriptor != null && !columnDescriptor.isReference()) {
            if (constructor) {
                literal = columnDescriptor.getLiteral(dataModel.getType(),
                    logicalTable.getSubtable(columnDescriptor.getColumnIdx(), rowNum, 1, 1),
                    openlAdapter);
            } else {
                try {
                    ILogicalTable lTable = logicalTable.getSubtable(columnDescriptor.getColumnIdx(), rowNum, 1, 1);
                    if (!(lTable.getHeight() == 1 && lTable.getWidth() == 1) || lTable.getCell(0, 0)
                        .getStringValue() != null) { // EPBDS-6104. For empty values should be used data type default
                        // value.
                        return columnDescriptor.populateLiteral(literal, lTable, openlAdapter, env);
                    }
                } catch (SyntaxNodeException ex) {
                    openlAdapter.getBindingContext().addError(ex);
                }
            }
        }

        return literal;
    }

    @Override
    public synchronized void setPrimaryIndexKey(int row, String value) {
        if (primaryIndexMap == null) {
            primaryIndexMap = new BiMap<>();
        }
        Integer oldRow = primaryIndexMap.getKey(value);
        if (oldRow != null && row != oldRow) {
            throw new OpenLRuntimeException(String.format("Duplicated key: %s in rows %s and %s.", value, oldRow, row));
        }
        primaryIndexMap.put(row, value);
    }

    @Override
    public Object findObject(int columnIndex, String skey, IBindingContext cxt) {
        ColumnDescriptor descriptor = dataModel.getDescriptor(columnIndex);

        Map<String, Integer> index = descriptor.getUniqueIndex(this, columnIndex, cxt);

        Integer found = index.get(skey);

        if (found == null) {
            return null;
        }

        return Array.get(dataArray, found);
    }

    private void addToRowIndex(int rowIndex, Object target) {
        if (rowIndexMap == null) {
            rowIndexMap = new BiMap<>();
        }
        rowIndexMap.put(rowIndex, target);
    }

    /**
     * @return Start row for data rows from Data_With_Titles rows. It depends on if table has or no column title row.
     */
    private int getStartRowForData() {
        if (dataModel.hasColumnTitleRow()) {
            return 1;
        }

        return 0;
    }

    private boolean isConstructor() {
        for (ColumnDescriptor columnDescriptor : dataModel.getDescriptors()) {
            if (columnDescriptor.isConstructor()) {
                return true;
            }
        }
        return false;
    }

    private DatatypeArrayMultiRowElementContext getCachedContext(int i) {
        if (dataContextCache == null || dataContextCache.isEmpty()) {
            return null;
        }
        return dataContextCache.get(i);
    }
}
