package org.openl.rules.dt;

import java.util.ArrayList;
import java.util.List;

import org.openl.binding.MethodUtil;
import org.openl.domain.IIntIterator;
import org.openl.rules.dt.algorithm.FailOnMissException;
import org.openl.rules.dt.algorithm.IDecisionTableAlgorithm;
import org.openl.rules.enumeration.DTEmptyResultProcessingEnum;
import org.openl.rules.method.RulesMethodInvoker;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.Tracer;

/**
 * Invoker for {@link DecisionTable}.
 *
 * @author DLiauchuk
 */
public class DecisionTableInvoker extends RulesMethodInvoker<DecisionTable> {

    private final boolean returnEmptyResult;
    private final IOpenClass retType;

    DecisionTableInvoker(DecisionTable decisionTable) {
        super(decisionTable);
        // This expression should be calculated once to improve performance of DT calculations
        this.returnEmptyResult = decisionTable.getMethodProperties() != null && DTEmptyResultProcessingEnum.RETURN
            .equals(decisionTable.getMethodProperties().getEmptyResultProcessing());
        this.retType = decisionTable.getType();
    }

    @Override
    public boolean canInvoke() {
        return getInvokableMethod().getAlgorithm() != null;
    }

    @Override
    public Object invokeSimple(Object target, Object[] params, IRuntimeEnv env) {
        try {
            env.pushLocalFrame(new Object[] { new DecisionTableRuntimePool() });
            return doInvoke(target, params, env);
        } finally {
            env.popLocalFrame();
        }
    }

    private Object doInvoke(Object target, Object[] params, IRuntimeEnv env) {
        IDecisionTableAlgorithm algorithm = getInvokableMethod().getAlgorithm();
        IIntIterator rulesIntIterator = algorithm.checkedRules(target, params, env);

        boolean atLeastOneRuleFired = false;
        List<Integer> r = new ArrayList<>();
        while (rulesIntIterator.hasNext()) {
            atLeastOneRuleFired = true;
            r.add(rulesIntIterator.nextInt());
        }
        int[] rules = new int[r.size()];
        int i = 0;
        for (Integer v : r) {
            rules[i++] = v;
        }

        IBaseAction[] actions = getInvokableMethod().getActionRows();

        Object returnValue = Tracer
            .invoke(new ActionInvoker(rules, actions, returnEmptyResult), target, params, env, this);
        if (returnValue != null) {
            return returnValue;
        }

        if (!atLeastOneRuleFired && getInvokableMethod().shouldFailOnMiss()) {
            String method = MethodUtil.printMethodWithParameterValues(getInvokableMethod().getMethod(), params);
            String message = String.format("Table '%s' failed to match any rule condition.", method);

            throw new FailOnMissException(message, getInvokableMethod());
        }

        return retType.nullObject();
    }

}
