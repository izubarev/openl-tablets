package org.openl.rules.testmethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.openl.binding.BindingDependencies;
import org.openl.rules.binding.RulesBindingDependencies;
import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.SpreadsheetStructureBuilder;
import org.openl.rules.data.ColumnDescriptor;
import org.openl.rules.data.DataTableBindHelper;
import org.openl.rules.data.FieldChain;
import org.openl.rules.data.IDataBase;
import org.openl.rules.data.ITableModel;
import org.openl.rules.data.PrecisionFieldChain;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.CollectionElementField;
import org.openl.types.impl.CollectionType;
import org.openl.types.impl.DynamicObject;
import org.openl.types.impl.ThisField;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.ClassUtils;
import org.openl.util.StringUtils;
import org.openl.vm.IRuntimeEnv;

public class TestSuiteMethod extends ExecutableRulesMethod {

    private final static String PRECISION_PARAM = "precision";
    private static final Pattern DASH_SEPARATOR = Pattern.compile("\\s[-]\\s");
    private IOpenMethod testedMethod;
    private TestDescription[] tests;
    private Map<String, Integer> indexes;
    private final boolean runMethod;
    private DynamicObject[] testObjects;
    private final IDataBase db;
    private ITableModel dataModel;
    private TestSuiteMethod originalTestSuiteMethod;

    public TestSuiteMethod(IOpenMethod testedMethod, IOpenMethodHeader header, TestMethodBoundNode boundNode) {
        super(header, boundNode);

        this.db = boundNode.getDataBase();
        this.testedMethod = testedMethod;
        initProperties(getSyntaxNode().getTableProperties());
        runMethod = XlsNodeTypes.XLS_RUN_METHOD.toString().equals(getSyntaxNode().getType());
    }

    public TestSuiteMethod(IOpenMethod testedMethod, TestSuiteMethod target) {
        super(target.getHeader(), target.getBoundNode());
        this.db = target.db;
        this.testedMethod = testedMethod;
        initProperties(target.getMethodProperties());
        this.runMethod = target.isRunMethod();
        this.testObjects = target.getTestObjects();
        this.dataModel = target.getDataModel();
        this.originalTestSuiteMethod = target.originalTestSuiteMethod != null ? target.originalTestSuiteMethod : target;
    }

    public TestSuiteMethod getOriginalTestSuiteMethod() {
        return originalTestSuiteMethod != null ? originalTestSuiteMethod : this;
    }

    private TestDescription[] initTestsAndIndexes() {
        DynamicObject[] testObjects = getTestObjects();
        TestDescription[] tests = new TestDescription[testObjects.length];
        indexes = new HashMap<>(tests.length);
        Map<String, Object> properties = getProperties();
        Integer precision = null;
        if (properties != null && properties.containsKey(PRECISION_PARAM)) {
            precision = Integer.parseInt(properties.get(PRECISION_PARAM).toString());
        }
        IOpenMethod testedMethod = getTestedMethod();
        List<IOpenField> fields = createFieldsToTest(testedMethod, getDataModel(), precision);

        for (int i = 0; i < tests.length; i++) {
            tests[i] = new TestDescription(testedMethod, testObjects[i], fields, getDataModel(), db);
            tests[i].setIndex(i);
            indexes.put(tests[i].getId(), i);
        }
        return tests;
    }

    public synchronized int[] getIndices(String ids) {
        if (tests == null) {
            initTestsAndIndexes();
        }
        TreeSet<Integer> result = new TreeSet<>();

        String[] ranges = StringUtils.split(ids.trim(), ',');
        for (String range : ranges) {
            if (range.isEmpty() && indexes.containsKey(",")) {
                result.add(indexes.get(","));
                continue;
            }
            String v = range.trim();
            if (indexes.containsKey(v)) {
                result.add(indexes.get(v));
                continue;
            }
            String[] edges = StringUtils.split(v, '-');
            if (edges.length > 2 || edges[edges.length - 1].trim().isEmpty()) {
                edges = DASH_SEPARATOR.split(v);
            }
            if (edges.length == 0) {
                if (indexes.containsKey("-")) {
                    result.add(indexes.get("-"));
                }
            } else {
                String startIdValue = edges[0].trim();
                String endIdValue = edges[edges.length - 1].trim();

                int startIndex = indexes.get(startIdValue);
                int endIndex = indexes.get(endIdValue);

                for (int i = startIndex; i <= endIndex; i++) {
                    result.add(i);
                }
            }
        }
        Integer[] indices = new Integer[result.size()];
        return ArrayUtils.toPrimitive(result.toArray(indices));
    }

    @Override
    public TestMethodBoundNode getBoundNode() {
        return (TestMethodBoundNode) super.getBoundNode();
    }

    @Override
    public BindingDependencies getDependencies() {
        BindingDependencies bindingDependencies = new RulesBindingDependencies();

        updateDependency(bindingDependencies);

        return bindingDependencies;
    }

    private void updateDependency(BindingDependencies bindingDependencies) {
        IOpenMethod testedMethod = getTestedMethod();
        if (testedMethod instanceof ExecutableRulesMethod || testedMethod instanceof OpenMethodDispatcher) {
            bindingDependencies.addMethodDependency(testedMethod, getBoundNode());
        }
    }

    public int getNumberOfTestsCases() {
        return getTestObjects().length;
    }

    @Override
    public String getSourceUrl() {
        return getSyntaxNode().getUri();
    }

    public DynamicObject[] getTestObjects() {
        initializeTestData();
        return testObjects;
    }

    public synchronized TestDescription[] getTests() {
        if (tests == null) {
            this.tests = initTestsAndIndexes();
        }
        return tests;
    }

    public TestDescription getTest(int numberOfTest) {
        return getTests()[numberOfTest];
    }

    public void setTestedMethod(IOpenMethod testedMethod) {
        this.testedMethod = testedMethod;
    }

    public String getColumnDisplayName(String columnTechnicalName) {
        int columnIndex = getColumnIndex(columnTechnicalName);
        return getColumnDisplayName(columnIndex);
    }

    public int getColumnIndex(String columnName) {
        ColumnDescriptor[] descriptors = getDataModel().getDescriptors();
        for (ColumnDescriptor descriptor : descriptors) {
            if (descriptor == null) {
                continue;
            }
            if (descriptor.getName().equals(columnName)) {
                return descriptor.getColumnIdx();
            }
        }

        return -1;
    }

    public String getColumnName(int index) {
        if (index >= 0) {
            ColumnDescriptor descriptor = getDataModel().getDescriptor(index);
            return descriptor == null ? null : descriptor.getName();
        } else {
            return null;
        }
    }

    public String getColumnDisplayName(int index) {
        if (index >= 0) {
            ColumnDescriptor descriptor = getDataModel().getDescriptor(index);
            return descriptor == null ? null : descriptor.getDisplayName();
        } else {
            return null;
        }
    }

    public int getColumnsCount() {
        return getDataModel().getColumnCount();
    }

    public IOpenMethod getTestedMethod() {
        return testedMethod;
    }

    @Override
    protected boolean isMethodCacheable() {
        return false;
    }

    @Override
    protected TestUnitsResults innerInvoke(Object target, Object[] params, IRuntimeEnv env) {
        return new TestSuite(this).invoke(target, env);
    }

    public boolean isRunMethod() {
        return runMethod;
    }

    /**
     * Indicates if test method has any row rules for testing target table. Finds it by field that contains
     * {@link TestMethodHelper#EXPECTED_RESULT_NAME} or {@link TestMethodHelper#EXPECTED_ERROR}
     *
     * @return true if method expects some return result or some error.
     *
     *         TODO: rename it. it is difficult to understand what is it doing
     */
    public boolean isRunmethodTestable() {
        for (int i = 0; i < getNumberOfTestsCases(); i++) {
            if (getTest(i).isExpectedResultDefined() || getTest(i)
                .isExpectedErrorDefined() || containsFieldsForSprCellTests(
                    getTest(i).getTestObject().getFieldValues().keySet()) || testedMethod instanceof Spreadsheet) {
                return true;
            }
        }

        return false;
    }

    private boolean containsFieldsForSprCellTests(Set<String> fieldNames) {
        for (String fieldName : fieldNames) {
            if (fieldName.startsWith(SpreadsheetStructureBuilder.DOLLAR_SIGN)) {
                return true;
            }
        }
        return false;
    }

    public ITableModel getDataModel() {
        initializeTestData();
        return dataModel;
    }

    private void initializeTestData() {
        if (dataModel == null) {
            testObjects = (DynamicObject[]) getBoundNode().getField().getData();
            dataModel = getBoundNode().getTable().getDataModel();
        }
    }

    private static List<IOpenField> createFieldsToTest(IOpenMethod testedMethod,
            ITableModel dataModel,
            Integer testTablePrecision) {
        IOpenClass resultType = testedMethod.getType();
        List<IOpenField> fieldsToTest = new ArrayList<>();
        for (int colNum = 0; colNum < dataModel.getColumnCount(); colNum++) {
            ColumnDescriptor columnDescriptor = dataModel.getDescriptor(colNum);
            if (columnDescriptor != null) {
                IdentifierNode[] nodes = columnDescriptor.getFieldChainTokens();
                if (nodes.length == 0 || !nodes[0].getIdentifier().startsWith(TestMethodHelper.EXPECTED_RESULT_NAME)) {
                    // skip empty or non-'_res_' columns
                    continue;
                }
                Integer fieldPrecision = testTablePrecision;
                if (nodes.length > 1 && StringUtils.matches(DataTableBindHelper.PRECISION_PATTERN,
                    nodes[nodes.length - 1].getIdentifier())) {
                    // set the precision of the field
                    fieldPrecision = DataTableBindHelper.getPrecisionValue(nodes[nodes.length - 1]);
                    nodes = ArrayUtils.remove(nodes, nodes.length - 1);
                }

                IOpenField[] fieldSequence;
                boolean resIsCollection = isCollectionType(nodes[0].getIdentifier());
                int startIndex = 0;
                IOpenClass currentType = resultType;

                if (resIsCollection) {
                    startIndex = 1;
                    fieldSequence = new IOpenField[nodes.length];
                    IOpenField arrayField = new ThisField(resultType);
                    CollectionType collectionType = getCollectionType(arrayField.getType());
                    IOpenField arrayAccessField;
                    if (collectionType == CollectionType.MAP) {
                        Object key = DataTableBindHelper.getCollectionKey(nodes[0]);
                        arrayAccessField = new CollectionElementField(arrayField,
                            key,
                            arrayField.getType().getComponentClass());
                    } else {
                        int index = DataTableBindHelper.getCollectionIndex(nodes[0]);
                        arrayAccessField = new CollectionElementField(arrayField,
                            index,
                            arrayField.getType().getComponentClass(),
                            collectionType);
                    }
                    if (arrayAccessField.getType().isArray()) {
                        currentType = arrayAccessField.getType().getComponentClass();
                    } else {
                        currentType = arrayAccessField.getType();
                    }
                    fieldSequence[0] = arrayAccessField;
                } else {
                    fieldSequence = new IOpenField[nodes.length - 1];
                }
                int i;
                for (i = startIndex; i < fieldSequence.length; i++) {
                    String identifier = nodes[i + 1 - startIndex].getIdentifier();
                    boolean isCollection = isCollectionType(identifier);
                    if (isCollection) {
                        IOpenField arrayField = currentType
                            .getField(DataTableBindHelper.getCollectionName(nodes[i + 1 - startIndex]));
                        // Try process field as SpreadsheetResult
                        if (arrayField == null && currentType.equals(JavaOpenClass.OBJECT) && StringUtils
                            .matches(DataTableBindHelper.SPREADSHEETRESULT_FIELD_PATTERN, identifier)) {
                            IOpenClass spreadsheetResultOpenClass = JavaOpenClass.getOpenClass(SpreadsheetResult.class);
                            arrayField = spreadsheetResultOpenClass
                                .getField(DataTableBindHelper.getCollectionName(nodes[i + 1 - startIndex]));
                        }
                        if (arrayField != null) {
                            IOpenClass type = arrayField.getType();
                            CollectionType collectionType = getCollectionType(type);
                            IOpenField arrayAccessField;
                            if (collectionType == CollectionType.MAP) {
                                Object key = DataTableBindHelper.getCollectionKey(nodes[i + 1 - startIndex]);
                                arrayAccessField = new CollectionElementField(arrayField,
                                    key,
                                    type.getComponentClass());
                            } else {
                                int arrayIndex = DataTableBindHelper.getCollectionIndex(nodes[i + 1 - startIndex]);
                                arrayAccessField = new CollectionElementField(arrayField,
                                    arrayIndex,
                                    type.getComponentClass(),
                                    collectionType);
                            }
                            fieldSequence[i] = arrayAccessField;
                        }
                    } else {
                        fieldSequence[i] = currentType.getField(identifier);
                        if (fieldSequence[i] == null && StringUtils
                            .matches(DataTableBindHelper.SPREADSHEETRESULT_FIELD_PATTERN, identifier)) {
                            // Try process field as SpreadsheetResult
                            IOpenClass spreadsheetResultOpenClass = JavaOpenClass.getOpenClass(SpreadsheetResult.class);
                            IOpenField openField = spreadsheetResultOpenClass.getField(identifier);
                            if (openField != null) {
                                fieldSequence[i] = openField;
                            }
                        }
                    }
                    if (fieldSequence[i] == null) {
                        break;
                    }
                    if (fieldSequence[i].getType().isArray() && isCollection) {
                        currentType = fieldSequence[i].getType().getComponentClass();
                    } else {
                        currentType = fieldSequence[i].getType();
                    }
                }
                if (i == 0 || i == fieldSequence.length) {
                    if (fieldSequence.length == 0) {
                        if (columnDescriptor.isReference()) {
                            if (resultType.isSimple() || resultType.isArray()) {
                                fieldsToTest.add(new ThisField(resultType));
                            } else {
                                fieldsToTest.addAll(resultType.getFields());
                            }
                            continue;
                        } else {
                            fieldSequence = new IOpenField[] { new ThisField(resultType) };
                        }
                    }
                    if (fieldPrecision != null) {
                        fieldsToTest.add(new PrecisionFieldChain(currentType, fieldSequence, fieldPrecision));
                    } else {
                        if (fieldSequence.length > 1) {
                            boolean hasNull = false;
                            for (IOpenField field : fieldSequence) {
                                if (field == null) {
                                    hasNull = true;
                                    break;
                                }
                            }
                            if (!hasNull) {
                                fieldsToTest.add(new FieldChain(currentType, fieldSequence));
                            }
                        } else {
                            IOpenField field = fieldSequence[0];
                            if (field != null) {
                                fieldsToTest.add(field);
                            }
                        }
                    }
                }
            }
        }
        return fieldsToTest;
    }

    private static boolean isCollectionType(String identifier) {
        return StringUtils.matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN, identifier) || StringUtils
            .matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN, identifier);
    }

    private static CollectionType getCollectionType(IOpenClass type) {
        Class<?> instanceClass = type.getInstanceClass();
        if (ClassUtils.isAssignable(instanceClass, List.class)) {
            return CollectionType.LIST;
        } else if (ClassUtils.isAssignable(instanceClass, Map.class)) {
            return CollectionType.MAP;
        } else {
            return CollectionType.ARRAY;
        }
    }

    @Override
    public void removeDebugInformation() {
        // Initialize data needed to run tests before removing debug info
        initializeTestData();
        super.removeDebugInformation();
    }
}
