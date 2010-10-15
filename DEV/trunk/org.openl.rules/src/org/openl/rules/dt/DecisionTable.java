/*
 * Created on Sep 10, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
package org.openl.rules.dt;

import java.util.Map;

import org.openl.OpenL;
import org.openl.binding.BindingDependencies;
import org.openl.binding.IBindingContextDelegator;

import org.openl.binding.impl.module.ModuleBindingContext;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.annotations.Executable;
import org.openl.rules.dt.algorithm.DecisionTableOptimizedAlgorithm;
import org.openl.rules.dt.algorithm.evaluator.IConditionEvaluator;
import org.openl.rules.dt.data.DecisionTableDataType;
import org.openl.rules.dt.element.FunctionalRow;
import org.openl.rules.dt.element.IAction;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.element.RuleRow;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.types.IMemberMetaInfo;
import org.openl.rules.table.ILogicalTable;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.AMethod;
import org.openl.types.impl.CompositeMethod;
import org.openl.types.impl.Invoker;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 * 
 */
@Executable
public class DecisionTable extends AMethod implements IMemberMetaInfo {

    private ICondition[] conditionRows;
    private IAction[] actionRows;
    /**
     * Optional non-functional row with rule indexes.
     */
    private RuleRow ruleRow;

    private int columns;

    private TableSyntaxNode tableSyntaxNode;
    private DecisionTableOptimizedAlgorithm algorithm;
    private Map<String, Object> properties;
    
    /**
     * Object to invoke current method.
     */
    private Invoker invoker;

    public DecisionTable(IOpenMethodHeader header) {
        super(header);
    }

    public IAction[] getActionRows() {
        return actionRows;
    }

    public DecisionTableOptimizedAlgorithm getAlgorithm() {
        return algorithm;
    }

    public int getColumns() {
        return columns;
    }

    public ICondition[] getConditionRows() {
        return conditionRows;
    }

    public String getDisplayName(int mode) {
        return getHeader().getInfo().getDisplayName(mode);
    }

    public ILogicalTable getDisplayTable() {
        ILogicalTable table = tableSyntaxNode.getSubTables().get(IXlsTableNames.VIEW_BUSINESS);

        return table.getColumn(0);
    }

    public IMemberMetaInfo getInfo() {
        return this;
    }

    public IOpenMethod getMethod() {
        return this;
    }

    public int getNumberOfRules() {

        if (actionRows.length > 0) {
            return actionRows[0].getParamValues().length;
        }

        return 0;
    }

    public String getRuleName(int col) {
        return ruleRow == null ? "R" + (col + 1) : ruleRow.getRuleName(col);
    }

    public RuleRow getRuleRow() {
        return ruleRow;
    }

    public ILogicalTable getRuleTable(int col) {
        ILogicalTable table = tableSyntaxNode.getSubTables().get(IXlsTableNames.VIEW_BUSINESS);

        return table.getColumn(col + 1);
    }
    
    /**
     * Returns logical table that contains rule column. The column will contain
     * all return, action and condition cells for rule specified by index.
     * 
     * @param ruleNumber Index of rule.
     * @return ILogicalTable that contains rule column.
     */
    public ILogicalTable getRuleByIndex(int ruleNumber) {
        ILogicalTable dt = actionRows[0].getDecisionTable();
        int starColumn = dt.getWidth() - columns;
        
        return dt.getColumn(starColumn + ruleNumber);
    }

    public String getSourceUrl() {
        return tableSyntaxNode.getUri();
    }

    public TableSyntaxNode getSyntaxNode() {
        return tableSyntaxNode;
    }

    public void setActionRows(IAction[] actionRows) {
        this.actionRows = actionRows;
    }

    public void setColumns(int columns) {
        this.columns = columns;
    }

    public void setConditionRows(ICondition[] conditionRows) {
        this.conditionRows = conditionRows;
    }

    public void setRuleRow(RuleRow ruleRow) {
        this.ruleRow = ruleRow;
    }

    public void setTableSyntaxNode(TableSyntaxNode tsn) {
        tableSyntaxNode = tsn;
    }

    public void bindTable(ICondition[] conditionRows,
            IAction[] actionRows,
            RuleRow ruleRow,
            OpenL openl,
            ModuleOpenClass module,
            IBindingContextDelegator cxtd,
            int columns) throws Exception {

        this.conditionRows = conditionRows;
        this.actionRows = actionRows;
        properties = tableSyntaxNode.getTableProperties().getAllProperties();
        if (!cxtd.isExecutionMode()) {
            this.ruleRow = ruleRow;
        }
        this.columns = columns;

        prepare(getHeader(), openl, module, cxtd);
    }

    public BindingDependencies getDependencies() {

        BindingDependencies bindingDependencies = new BindingDependencies();
        updateDependency(bindingDependencies);

        return bindingDependencies;
    }

    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        if (invoker == null) {
            invoker = new DecisionTableInvoker(this, target, params, env);
        } else {
            invoker.resetParams(target, params, env);
        }
        return invoker.invoke();        
    }

    /**
     * Check whether execution of decision table should be failed if no rule
     * fired.
     */
    public boolean shouldFailOnMiss() {
        return (Boolean)properties.get("failOnMiss");
    }

    protected void makeAlgorithm(IConditionEvaluator[] evs) throws Exception {

        algorithm = new DecisionTableOptimizedAlgorithm(evs, this);
        algorithm.buildIndex();
    }

    private void prepare(IOpenMethodHeader header,
            OpenL openl,
            ModuleOpenClass module,
            IBindingContextDelegator bindingContextDelegator) throws Exception {

        IMethodSignature signature = header.getSignature();
        
        IConditionEvaluator[] evaluators = prepareConditions(openl, module, bindingContextDelegator, signature);
        
        prepareActions(header, openl, module, bindingContextDelegator, signature);

        makeAlgorithm(evaluators);
    }

    private void prepareActions(IOpenMethodHeader header,
            OpenL openl,
            ModuleOpenClass module,
            IBindingContextDelegator bindingContextDelegator,
            IMethodSignature signature) throws Exception {
        
        IBindingContextDelegator actionBindingContextDelegator = new ModuleBindingContext(bindingContextDelegator, (ModuleOpenClass)getRuleExecutionType(openl));

        for (int i = 0; i < actionRows.length; i++) {
            IOpenClass methodType = actionRows[i].isReturnAction() ? header.getType() : JavaOpenClass.VOID;
            actionRows[i].prepareAction(methodType, signature, openl, module, actionBindingContextDelegator, ruleRow, getRuleExecutionType(openl));
        }
    }

    private IConditionEvaluator[] prepareConditions(OpenL openl,
            ModuleOpenClass module,
            IBindingContextDelegator bindingContextDelegator,
            IMethodSignature signature) throws Exception {
        IConditionEvaluator[] evaluators = new IConditionEvaluator[conditionRows.length];

        for (int i = 0; i < conditionRows.length; i++) {
            evaluators[i] = conditionRows[i].prepareCondition(signature,
                openl,
                module,
                bindingContextDelegator,
                ruleRow);
        }
        return evaluators;
    }

    @Override
    public String toString() {
        return getName();
    }

    public void updateDependency(BindingDependencies dependencies) {

        for (int i = 0; i < conditionRows.length; i++) {
            ((CompositeMethod) conditionRows[i].getMethod()).updateDependency(dependencies);
            updateValueDependency((FunctionalRow) conditionRows[i], dependencies);
        }

        for (int i = 0; i < actionRows.length; i++) {
            ((CompositeMethod) actionRows[i].getMethod()).updateDependency(dependencies);
            updateValueDependency((FunctionalRow) actionRows[i], dependencies);
        }
    }

    protected void updateValueDependency(FunctionalRow frow, BindingDependencies dependencies) {

        Object[][] values = frow.getParamValues();

        for (int i = 0; i < values.length; i++) {

            if (values[i] == null) {
                continue;
            }

            for (int j = 0; j < values[i].length; j++) {
                if (values[i][j] instanceof CompositeMethod) {
                    ((CompositeMethod) values[i][j]).updateDependency(dependencies);
                }
            }
        }
    }    
    
    IOpenClass ruleExecutionType;
    
    private synchronized IOpenClass getRuleExecutionType(OpenL openl)
    {
        if (ruleExecutionType == null)
        {
            ruleExecutionType = new DecisionTableDataType(this, null, getName()+ "Type", openl);
        }    
        return ruleExecutionType;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }
    

}