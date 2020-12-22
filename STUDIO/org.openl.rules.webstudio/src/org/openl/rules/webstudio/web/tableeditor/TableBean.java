package org.openl.rules.webstudio.web.tableeditor;

import static org.openl.rules.security.AccessManager.isGranted;
import static org.openl.rules.security.Privileges.BENCHMARK;
import static org.openl.rules.security.Privileges.CREATE_TABLES;
import static org.openl.rules.security.Privileges.EDIT_TABLES;
import static org.openl.rules.security.Privileges.REMOVE_TABLES;
import static org.openl.rules.security.Privileges.RUN;
import static org.openl.rules.security.Privileges.TRACE;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.Severity;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.TableSyntaxNodeUtils;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.service.TableServiceImpl;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.tableeditor.model.TableEditorModel;
import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.rules.testmethod.ProjectHelper;
import org.openl.rules.testmethod.TestDescription;
import org.openl.rules.testmethod.TestMethodBoundNode;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.testmethod.TestUtils;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.RecentlyVisitedTables;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.validation.properties.dimentional.DispatcherTablesBuilder;
import org.openl.rules.webstudio.util.XSSFOptimizer;
import org.openl.rules.webstudio.web.test.Utils;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenMethod;
import org.openl.util.CollectionUtils;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.PropertyResolver;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Request scope managed bean for Table page.
 */
@Service
@RequestScope
public class TableBean {
    private static final String REQUEST_ID_FORMAT = "request-id:%s;project-name:%s";
    private static final Pattern REQUEST_ID_PATTERN = Pattern.compile("request-id:(.+);project-name:(.+)");
    private static final int MAX_PROBLEMS = 100;
    private final Logger log = LoggerFactory.getLogger(TableBean.class);

    private IOpenMethod method;

    // Test in current table (only for test tables)
    private TestDescription[] runnableTestMethods = {}; // test units
    // All checks and tests for current table (including tests with no cases, run methods).
    private IOpenMethod[] allTests = {};
    private IOpenMethod[] tests = {};

    private List<TableDescription> targetTables;

    private String uri;
    private String id;
    private IOpenLTable table;
    private boolean editable;
    private boolean canBeOpenInExcel;
    private boolean copyable;

    // Errors + Warnings
    private List<OpenLMessage> problems;

    private final PropertyResolver propertyResolver;

    private boolean hasErrors;

    public TableBean(PropertyResolver propertyResolver) {
        this.propertyResolver = propertyResolver;

        id = WebStudioUtils.getRequestParameter(Constants.REQUEST_PARAM_ID);

        WebStudio studio = WebStudioUtils.getWebStudio();
        final ProjectModel model = studio.getModel();

        table = model.getTableById(id);

        // TODO: There is should be a method to get the table by the ID without using URI which is used to generate the
        // ID.
        if (table == null) {
            table = model.getTable(studio.getTableUri());
        }

        if (table != null) {
            id = table.getId();
            uri = table.getUri();
            // Save URI because some actions don't provide table ID
            studio.setTableUri(uri);

            method = model.getMethod(uri);

            editable = model.isEditableTable(uri) && !isDispatcherValidationNode();
            canBeOpenInExcel = model.isEditable() && !isDispatcherValidationNode();
            copyable = editable && table.isCanContainProperties() && !XlsNodeTypes.XLS_DATATYPE.toString()
                .equals(table.getType()) && isGranted(CREATE_TABLES);

            initTargetTables();

            initProblems();
            initTests(model);

            // Save last visited table
            model.getRecentlyVisitedTables().setLastVisitedTable(table);
            // Check the save table parameter
            String saveTable1 = WebStudioUtils.getRequestParameter("saveTable");
            boolean saveTable = saveTable1 == null || Boolean.parseBoolean(saveTable1);
            if (saveTable) {
                storeTable();
            }
        }
    }

    private void storeTable() {
        ProjectModel model = WebStudioUtils.getProjectModel();
        RecentlyVisitedTables recentlyVisitedTables = model.getRecentlyVisitedTables();
        recentlyVisitedTables.add(table);
    }

    private void initTargetTables() {
        List<TableDescription> targetTables = new ArrayList<>();
        String tableType = table.getType();
        if (tableType.equals(XlsNodeTypes.XLS_TEST_METHOD.toString()) || tableType
            .equals(XlsNodeTypes.XLS_RUN_METHOD.toString())) {

            if (method instanceof TestSuiteMethod) {
                List<IOpenMethod> targetMethods = new ArrayList<>();
                IOpenMethod testedMethod = ((TestSuiteMethod) method).getTestedMethod();

                // Overloaded methods
                if (testedMethod instanceof OpenMethodDispatcher) {
                    List<IOpenMethod> overloadedMethods = ((OpenMethodDispatcher) testedMethod).getCandidates();
                    targetMethods.addAll(overloadedMethods);
                } else {
                    targetMethods.add(testedMethod);
                }

                for (IOpenMethod targetMethod : targetMethods) {
                    IMemberMetaInfo methodInfo = targetMethod.getInfo();
                    if (methodInfo != null) {
                        TableSyntaxNode tsn = (TableSyntaxNode) methodInfo.getSyntaxNode();
                        IOpenLTable targetTable = new TableSyntaxNodeAdapter(tsn);
                        targetTables.add((new TableDescription(targetTable.getUri(),
                            targetTable.getId(),
                            getTableName(targetTable))));
                    }
                }
            }

        }
        this.targetTables = targetTables;
    }

    private void initTests(final ProjectModel model) {
        initRunnableTestMethods();

        allTests = model.getTestAndRunMethods(uri);
        tests = model.getTestMethods(uri);
    }

    private void initRunnableTestMethods() {
        if (method instanceof TestSuiteMethod) {
            try {
                runnableTestMethods = ((TestSuiteMethod) method).getTests();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                runnableTestMethods = new TestDescription[0];
            }
        }
    }

    private void initProblems() {

        ProjectModel model = WebStudioUtils.getProjectModel();
        String tableUri = table.getUri();
        List<OpenLMessage> errors = new ArrayList<>(model.getErrorsByUri(tableUri));
        List<OpenLMessage> warnings = new ArrayList<>(model.getWarnsByUri(tableUri));

        if (warnings.size() >= MAX_PROBLEMS) {
            warnings = warnings.subList(0, MAX_PROBLEMS);
            warnings.add(OpenLMessagesUtils.newErrorMessage(
                    "Only first " + MAX_PROBLEMS + " warnings are shown. Fix them first."));
        }
        if (errors.size() >= MAX_PROBLEMS) {
            errors = errors.subList(0, MAX_PROBLEMS);
            errors.add(OpenLMessagesUtils.newErrorMessage(
                    "Only first " + MAX_PROBLEMS + " errors are shown. Fix them first."));
        }

        hasErrors = !errors.isEmpty();
        // if the current table is a test then check tested target tables on errors.
        for (TableDescription targetTable : targetTables) {
            if (!model.getErrorsByUri(targetTable.getUri()).isEmpty()) {
                warnings.add(new OpenLMessage("Tested rules have errors", Severity.WARN));
                hasErrors = true;
                break;
            }
        }

        problems = new ArrayList<>();
        problems.addAll(errors);
        problems.addAll(warnings);
    }

    public String getTableName(IOpenLTable table) {
        String[] dimensionProps = TablePropertyDefinitionUtils.getDimensionalTablePropertiesNames();
        ITableProperties tableProps = table.getProperties();
        StringBuilder dimensionBuilder = new StringBuilder();
        String tableName = table.getDisplayName();
        if (tableProps != null) {
            for (String dimensionProp : dimensionProps) {
                String propValue = tableProps.getPropertyValueAsString(dimensionProp);

                if (propValue != null && !propValue.isEmpty()) {
                    dimensionBuilder.append(dimensionBuilder.length() == 0 ? "" : ", ")
                        .append(dimensionProp)
                        .append(" = ")
                        .append(propValue);
                }
            }
        }
        if (dimensionBuilder.length() > 0) {
            return tableName + " [" + dimensionBuilder.toString() + "]";
        } else {
            return tableName;
        }
    }

    public boolean isDispatcherValidationNode() {
        return table != null && table.getName().startsWith(DispatcherTablesBuilder.DEFAULT_DISPATCHER_TABLE_NAME);
    }

    public String getMode() {
        return getCanEdit() ? WebStudioUtils.getRequestParameter("mode") : null;
    }

    public IOpenLTable getTable() {
        return table;
    }

    public List<OpenLMessage> getProblems() {
        return problems;
    }

    /**
     * Return test cases for current table.
     *
     * @return array of tests for current table.
     */
    public TestDescription[] getTests() {
        return runnableTestMethods;
    }

    public ParameterWithValueDeclaration[] getTestCaseParams(TestDescription testCase) {
        ParameterWithValueDeclaration[] params;
        if (testCase != null) {
            ParameterWithValueDeclaration[] contextParams = TestUtils
                .getContextParams(new TestSuite((TestSuiteMethod) method), testCase);
            Utils.getDb(WebStudioUtils.getProjectModel());
            ParameterWithValueDeclaration[] inputParams = testCase.getExecutionParams();

            params = new ParameterWithValueDeclaration[contextParams.length + inputParams.length];
            int n = 0;
            for (ParameterWithValueDeclaration contextParam : contextParams) {
                params[n++] = contextParam;
            }
            for (ParameterWithValueDeclaration inputParam : inputParams) {
                params[n++] = inputParam;
            }
        } else {
            params = ParameterWithValueDeclaration.EMPTY_ARRAY;
        }
        return params;
    }

    public String getUri() {
        return uri;
    }

    public String getId() {
        return id;
    }

    public List<TableDescription> getTargetTables() {
        return targetTables;
    }

    /**
     * @return true if it is possible to create tests for current table.
     */
    public boolean isCanCreateTest() {
        return table != null && table.isExecutable() && isEditable() && isGranted(CREATE_TABLES);
    }

    public boolean isEditable() {
        return editable;
    }

    public boolean isCopyable() {
        return copyable;
    }

    public boolean isTablePart() {
        return WebStudioUtils.getProjectModel().isTablePart(uri);
    }

    public boolean isHasErrors() {
        return hasErrors;
    }

    public boolean isHasProblems() {
        return CollectionUtils.isNotEmpty(problems);
    }

    /**
     * Checks if there are runnable tests for current table.
     *
     * @return true if there are runnable tests for current table.
     */
    public boolean isTestable() {
        return runnableTestMethods.length > 0;
    }

    /**
     * Checks if there are tests, including tests with test cases, runs with filled runs, tests without cases(empty),
     * runs without any parameters and tests without cases and runs.
     */
    public boolean isHasAnyTests() {
        return CollectionUtils.isNotEmpty(allTests);
    }

    public boolean isHasTests() {
        return CollectionUtils.isNotEmpty(tests);
    }

    /**
     * Gets all tests for current table.
     */
    public TableDescription[] getAllTests() {
        if (allTests == null) {
            return null;
        }
        List<TableDescription> tableDescriptions = new ArrayList<>(allTests.length);
        for (IOpenMethod test : allTests) {
            TableSyntaxNode syntaxNode = (TableSyntaxNode) test.getInfo().getSyntaxNode();
            tableDescriptions.add(new TableDescription(syntaxNode.getUri(), syntaxNode.getId(), getTestName(test)));
        }
        tableDescriptions.sort(Comparator.comparing(TableDescription::getName));
        return tableDescriptions.toArray(new TableDescription[0]);
    }

    public String getTestName(Object testMethod) {
        IOpenMethod method = (IOpenMethod) testMethod;
        String name = TableSyntaxNodeUtils.getTestName(method);
        String info = ProjectHelper.getTestInfo(method);
        return String.format("%s (%s)", name, info);
    }

    public String removeTable() throws Throwable {
        try {
            final WebStudio studio = WebStudioUtils.getWebStudio();
            IGridTable gridTable = table.getGridTable(IXlsTableNames.VIEW_DEVELOPER);

            gridTable.edit();
            new TableServiceImpl().removeTable(gridTable);
            XlsSheetGridModel sheetModel = (XlsSheetGridModel) gridTable.getGrid();
            sheetModel.getSheetSource().getWorkbookSource().save();
            gridTable.stopEditing();
            WebStudioUtils.getExternalContext()
                .getSessionMap()
                .remove(org.openl.rules.tableeditor.util.Constants.TABLE_EDITOR_MODEL_NAME);

            studio.compile();
            RecentlyVisitedTables visitedTables = studio.getModel().getRecentlyVisitedTables();
            visitedTables.remove(table);
        } catch (Exception e) {
            throw e.getCause();
        }
        return null;
    }

    public boolean beforeEditAction() {
        final WebStudio studio = WebStudioUtils.getWebStudio();
        RulesProject currentProject = studio.getCurrentProject();
        if (currentProject != null) {
            try {
                return currentProject.tryLock();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return false;
            }
        }

        return true;
    }

    public boolean beforeSaveAction() {
        String editorId = WebStudioUtils
            .getRequestParameter(org.openl.rules.tableeditor.util.Constants.REQUEST_PARAM_EDITOR_ID);

        Map<?, ?> editorModelMap = (Map<?, ?>) WebStudioUtils.getExternalContext()
            .getSessionMap()
            .get(org.openl.rules.tableeditor.util.Constants.TABLE_EDITOR_MODEL_NAME);

        TableEditorModel editorModel = (TableEditorModel) editorModelMap.get(editorId);

        Workbook workbook = editorModel.getSheetSource().getWorkbookSource().getWorkbook();
        if (workbook instanceof XSSFWorkbook) {
            XSSFOptimizer.removeUnusedStyles((XSSFWorkbook) workbook);
        }

        if (WebStudioUtils.getWebStudio().isUpdateSystemProperties()) {
            return EditHelper.updateSystemProperties(table, editorModel, propertyResolver.getProperty("user.mode"));
        }
        return true;
    }

    public void afterSaveAction(String newId) {
        final WebStudio studio = WebStudioUtils.getWebStudio();
        studio.compile();
    }

    public String getRequestId() {
        final WebStudio studio = WebStudioUtils.getWebStudio();
        RulesProject currentProject = studio.getCurrentProject();
        String requestId = currentProject == null ? "" : currentProject.getRepository().getId();
        String projectName = currentProject == null ? "" : currentProject.getName();
        return String.format(REQUEST_ID_FORMAT, requestId, projectName);
    }

    public static void tryUnlock(String requestId) {
        if (StringUtils.isBlank(requestId)) {
            return;
        }
        Matcher matcher = REQUEST_ID_PATTERN.matcher(requestId);
        if (!matcher.matches()) {
            return;
        }

        String repositoryId = matcher.group(1);
        String projectName = matcher.group(2);

        final WebStudio studio = WebStudioUtils.getWebStudio();
        RulesProject currentProject = studio.getProject(repositoryId, projectName);
        if (currentProject != null) {
            try {
                if (!currentProject.isModified()) {
                    currentProject.releaseMyLock();
                }
            } catch (Exception e) {
                Logger logger = LoggerFactory.getLogger(TableBean.class);
                logger.error(e.getMessage(), e);
            }
        }
    }

    public boolean getCanEdit() {
        return isEditable() && isGranted(EDIT_TABLES);
    }

    public boolean isCanOpenInExcel() {
        return canBeOpenInExcel;
    }

    public boolean getCanRemove() {
        return isEditable() && isGranted(REMOVE_TABLES);
    }

    public boolean getCanRun() {
        return isGranted(RUN) && !isHasErrors();
    }

    public boolean getCanTrace() {
        return isGranted(TRACE) && !isHasErrors();
    }

    public boolean getCanBenchmark() {
        return isGranted(BENCHMARK) && !isHasErrors();
    }

    public Integer getRowIndex() {
        if (runnableTestMethods.length > 0 && !runnableTestMethods[0].hasId()) {
            if (method instanceof TestSuiteMethod) {
                TestMethodBoundNode boundNode = ((TestSuiteMethod) method).getBoundNode();
                if (boundNode != null && !boundNode.getTable().getHeaderTable().isNormalOrientation()) {
                    // Currently row indexes aren't supported for transposed test tables
                    return null;
                }
            }

            return table.getGridTable().getHeight() - runnableTestMethods.length + 1;
        }
        return null;
    }

    public static class TableDescription {
        private final String uri;
        private final String id;
        private final String name;

        public TableDescription(String uri, String id, String name) {
            this.uri = uri;
            this.id = id;
            this.name = name;
        }

        public String getUri() {
            return uri;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }
}
