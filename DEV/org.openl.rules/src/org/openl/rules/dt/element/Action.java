package org.openl.rules.dt.element;

import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.rules.calc.AnySpreadsheetResultOpenClass;
import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.dt.DTScale;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.data.RuleExecutionObject;
import org.openl.rules.dt.storage.IStorage;
import org.openl.rules.enumeration.DTEmptyResultProcessingEnum;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ILogicalTable;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IDynamicObject;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.IParameterDeclaration;
import org.openl.types.NullParameterDeclaration;
import org.openl.types.impl.CompositeMethod;
import org.openl.types.impl.ParameterDeclaration;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.ClassUtils;
import org.openl.util.StringUtils;
import org.openl.vm.IRuntimeEnv;

public class Action extends FunctionalRow implements IAction {

    private static final String EXTRA_RET = "e$x$t$r$a$R$e$t";
    private boolean isSingleReturnParam;
    private IOpenClass returnType;
    private IOpenClass singleActionReturnType;
    private final ActionType actionType;
    private final boolean skipEmptyResult;

    public Action(String name,
            int row,
            ILogicalTable decisionTable,
            ActionType actionType,
            DTScale.RowScale scale,
            DecisionTable decisionTableInvocableMethod) {
        super(name, row, decisionTable, scale);
        this.actionType = actionType;
        this.skipEmptyResult = decisionTableInvocableMethod
            .getMethodProperties() != null && DTEmptyResultProcessingEnum.SKIP
                .equals(decisionTableInvocableMethod.getMethodProperties().getEmptyResultProcessing());
    }

    @Override
    public boolean isAction() {
        return true;
    }

    @Override
    public boolean isCondition() {
        return false;
    }

    @Override
    public boolean isReturnAction() {
        return ActionType.RETURN == actionType;
    }

    @Override
    public boolean isCollectReturnKeyAction() {
        return ActionType.COLLECT_RETURN_KEY == actionType;
    }

    @Override
    public boolean isCollectReturnAction() {
        return ActionType.COLLECT_RETURN == actionType;
    }

    @Override
    public Object executeAction(int ruleN, Object target, Object[] params, IRuntimeEnv env) {

        if (target instanceof IDynamicObject) {
            target = new RuleExecutionObject(ruleExecutionType, (IDynamicObject) target, ruleN);
        }

        if (isSingleReturnParam) {
            if (skipEmptyResult && isEmpty(ruleN)) {
                return null;
            }

            Object[] dest = new Object[getNumberOfParams()];
            loadValues(dest, 0, ruleN, target, params, env);

            Object returnValue = dest[0];
            IOpenMethod method = getMethod();
            IOpenClass methodType = method.getType();

            // Check that returnValue object has the same type as a return type
            // of method. If they are same return returnValue as result of
            // execution.
            //
            if (returnValue == null || ClassUtils.isAssignable(returnValue.getClass(), methodType.getInstanceClass())) {
                return ifSpreadsheetResultUseDTSprType(returnValue);
            }

            // At this point of action execution we have the result value, but it
            // has different type than return type of method. We should skip
            // optimization for this step and invoke method.
            //
            return ifSpreadsheetResultUseDTSprType(executeActionInternal(ruleN, target, params, env));
        }

        return ifSpreadsheetResultUseDTSprType(executeActionInternal(ruleN, target, params, env));
    }

    private Object ifSpreadsheetResultUseDTSprType(Object value) {
        if (singleActionReturnType instanceof CustomSpreadsheetResultOpenClass) {
            SpreadsheetResult spreadsheetResult = (SpreadsheetResult) value;
            SpreadsheetResult newSpreadsheetResult = new SpreadsheetResult(spreadsheetResult);
            newSpreadsheetResult
                .setCustomSpreadsheetResultOpenClass((CustomSpreadsheetResultOpenClass) singleActionReturnType);
            return newSpreadsheetResult;
        }
        return value;
    }

    private Object executeActionInternal(int ruleN, Object target, Object[] params, IRuntimeEnv env) {
        if (skipEmptyResult && isEmpty(ruleN)) {
            return null;
        }

        return getMethod().invoke(target, mergeParams(target, params, env, ruleN), env);
    }

    private static IOpenClass extractMethodTypeForCollectReturnKeyAction(TableSyntaxNode tableSyntaxNode,
            IBindingContext bindingContext) {
        IOpenClass cType = null;
        if (tableSyntaxNode.getHeader().getCollectParameters().length > 0) {
            cType = bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE,
                tableSyntaxNode.getHeader().getCollectParameters()[0]);
        }
        return cType != null ? cType : JavaOpenClass.OBJECT;
    }

    private static IOpenClass extractMethodTypeForCollectReturnAction(TableSyntaxNode tableSyntaxNode,
            IOpenClass type,
            IBindingContext bindingContext) {
        if (type.isArray()) {
            return type.getComponentClass();
        }
        if (ClassUtils.isAssignable(type.getInstanceClass(), Collection.class)) {
            IOpenClass cType = null;
            if (tableSyntaxNode.getHeader().getCollectParameters().length > 0) {
                cType = bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE,
                    tableSyntaxNode.getHeader().getCollectParameters()[0]);
            }
            return cType != null ? cType : JavaOpenClass.OBJECT;
        }
        if (ClassUtils.isAssignable(type.getInstanceClass(), Map.class)) {
            IOpenClass cType = null;
            if (tableSyntaxNode.getHeader().getCollectParameters().length > 1) {
                cType = bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE,
                    tableSyntaxNode.getHeader().getCollectParameters()[1]);
            }
            return cType != null ? cType : JavaOpenClass.OBJECT;
        }
        return type;
    }

    @Override
    public void prepareAction(DecisionTable decisionTable,
            IOpenMethodHeader header,
            IMethodSignature signature,
            OpenL openl,
            IBindingContext bindingContext,
            RuleRow ruleRow,
            IOpenClass ruleExecutionType,
            TableSyntaxNode tableSyntaxNode) throws Exception {

        IOpenClass methodType = JavaOpenClass.VOID;
        if (isReturnAction()) {
            methodType = header.getType();
        } else {
            if (isCollectReturnAction()) {
                methodType = extractMethodTypeForCollectReturnAction(tableSyntaxNode, header.getType(), bindingContext);
            } else {
                if (isCollectReturnKeyAction()) {
                    methodType = extractMethodTypeForCollectReturnKeyAction(tableSyntaxNode, bindingContext);
                }
            }
        }

        prepare(decisionTable,
            methodType,
            signature,
            openl,
            bindingContext,
            ruleRow,
            ruleExecutionType,
            tableSyntaxNode);

        IParameterDeclaration[] params = getParams();
        CompositeMethod method = getMethod();
        String code = Optional.ofNullable(method.getMethodBodyBoundNode())
            .map(IBoundNode::getSyntaxNode)
            .map(ISyntaxNode::getModule)
            .map(IOpenSourceCodeModule::getCode)
            .orElse(null);

        isSingleReturnParam = params.length == 1 && !NullParameterDeclaration.isAnyNull(params[0]) && params[0]
            .getName()
            .equals(code);

        if ((isReturnAction() || isCollectReturnAction()) && decisionTable.isTypeCustomSpreadsheetResult()) {
            if (method.getBodyType() instanceof AnySpreadsheetResultOpenClass) {
                decisionTable.setTypeCustomSpreadsheetResult(false);
                decisionTable.setCustomSpreadsheetResultType(null);
                decisionTable.setTypeAnySpreadsheetResult(true);
            } else {
                decisionTable.getDeferredChanges().add(() -> {
                    decisionTable.getCustomSpreadsheetResultType().updateWithType(method.getBodyType());
                });
            }
        }

        this.returnType = decisionTable.getType();
        if (isReturnAction()) {
            this.singleActionReturnType = decisionTable.getType();
        } else {
            if (isCollectReturnAction()) {
                this.singleActionReturnType = extractMethodTypeForCollectReturnAction(tableSyntaxNode,
                    decisionTable.getType(),
                    bindingContext);
            }
        }
    }

    @Override
    public IOpenClass getType() {
        return returnType;
    }

    @Override
    protected void prepareParams(IOpenClass declaringClass,
            IMethodSignature signature,
            IOpenClass methodType,
            IOpenSourceCodeModule methodSource,
            OpenL openl,
            IBindingContext bindingContext) throws Exception {

        if (EXTRA_RET.equals(
            methodSource.getCode()) && (isReturnAction() || isCollectReturnAction() || isCollectReturnKeyAction())) {
            ParameterDeclaration extraParam = new ParameterDeclaration(methodType, EXTRA_RET);
            params = new IParameterDeclaration[] { extraParam };
            paramInitialized = new BitSet(1);
            paramInitialized.set(0);
            paramsUniqueNames = new HashSet<>();
            paramsUniqueNames.add(extraParam.getName());
        } else {
            super.prepareParams(declaringClass, signature, methodType, methodSource, openl, bindingContext);
        }
    }

    @Override
    protected IOpenSourceCodeModule getExpressionSource(TableSyntaxNode tableSyntaxNode,
            IMethodSignature signature,
            IOpenClass methodType,
            IOpenClass declaringClass,
            OpenL openl,
            IBindingContext bindingContext) throws Exception {

        IOpenSourceCodeModule source = super.getExpressionSource(tableSyntaxNode,
            signature,
            methodType,
            declaringClass,
            openl,
            bindingContext);

        if ((isReturnAction() || isCollectReturnAction() || isCollectReturnKeyAction()) && StringUtils
            .isEmpty(source.getCode())) {
            if (hasDeclaredParams()) {
                // trigger parameter compilation & initialization
                super.prepareParams(declaringClass, signature, methodType, null, openl, bindingContext);
                // generate return statement to return parameter
                return new StringSourceCodeModule(params[0].getName() != null ? params[0].getName() : EXTRA_RET,
                    source.getUri());
            }
            return new StringSourceCodeModule(EXTRA_RET, source.getUri());
        }
        return source;
    }

    @Override
    public void removeDebugInformation() {
        super.removeDebugInformation();
        if (storage != null) {
            for (IStorage<?> st : storage) {
                int rules = st.size();
                for (int i = 0; i < rules; i++) {
                    Object paramValue = st.getValue(i);
                    if (paramValue instanceof CompositeMethod) {
                        ((CompositeMethod) paramValue).removeDebugInformation();
                    }
                }
            }
        }
    }

}
