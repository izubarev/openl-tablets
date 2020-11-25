package org.openl.rules.dt.algorithm.evaluator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openl.domain.IDomain;
import org.openl.domain.IIntIterator;
import org.openl.domain.IIntSelector;
import org.openl.domain.IntRangeDomain;
import org.openl.rules.dt.DecisionTableRuleNodeBuilder;
import org.openl.rules.dt.IBaseCondition;
import org.openl.rules.dt.element.ConditionCasts;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.type.IRangeAdaptor;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.types.IParameterDeclaration;
import org.openl.vm.IRuntimeEnv;

public abstract class ARangeIndexEvaluator extends AConditionEvaluator implements IConditionEvaluator {

    final IRangeAdaptor<Object, ? extends Comparable<Object>> rangeAdaptor;
    final int nparams;

    ARangeIndexEvaluator(IRangeAdaptor<Object, ? extends Comparable<Object>> rangeAdaptor,
            int nparams,
            ConditionCasts conditionCasts) {
        super(conditionCasts);
        this.rangeAdaptor = rangeAdaptor;
        this.nparams = nparams;
    }

    @Override
    public IOpenSourceCodeModule getFormalSourceCode(IBaseCondition condition) {
        if (rangeAdaptor != null && rangeAdaptor.useOriginalSource()) {
            return condition.getSourceCodeModule();
        }

        IParameterDeclaration[] params = condition.getParams();
        IOpenSourceCodeModule conditionSource = condition.getSourceCodeModule();

        String code = params.length == 2 ? String.format("%1$s<=(%2$s) && (%2$s) < %3$s",
            params[0].getName(),
            conditionSource.getCode(),
            params[1].getName()) : String.format("%1$s.contains(%2$s)", params[0].getName(), conditionSource.getCode());
        return new StringSourceCodeModule(code, conditionSource.getUri());
    }

    @Override
    @SuppressWarnings("unchecked")
    public IIntSelector getSelector(ICondition condition, Object target, Object[] dtparams, IRuntimeEnv env) {
        Object value = conditionCasts.castToConditionType(condition.getEvaluator().invoke(target, dtparams, env));
        return new RangeSelector(condition, value, target, dtparams, rangeAdaptor, env);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected IDomain<?> indexedDomain(IBaseCondition condition) throws DomainCanNotBeDefined {
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;

        int nRules = condition.getNumberOfRules();
        for (int ruleN = 0; ruleN < nRules; ruleN++) {
            if (condition.isEmpty(ruleN)) {
                continue;
            }

            Comparable<?> vFrom;
            Comparable<?> vTo;
            if (nparams == 2) {
                if (rangeAdaptor == null) {
                    vFrom = (Comparable<Object>) condition.getParamValue(0, ruleN);
                    vTo = (Comparable<Object>) condition.getParamValue(1, ruleN);
                } else {
                    vFrom = rangeAdaptor.getMin(condition.getParamValue(0, ruleN));
                    vTo = rangeAdaptor.getMax(condition.getParamValue(1, ruleN));
                }
            } else {
                if (rangeAdaptor == null) {
                    vFrom = (Comparable<Object>) condition.getParamValue(0, ruleN);
                    vTo = (Comparable<Object>) condition.getParamValue(0, ruleN);
                } else {
                    Object range = condition.getParamValue(0, ruleN);
                    vFrom = rangeAdaptor.getMin(range);
                    vTo = rangeAdaptor.getMax(range);
                }
            }

            if (!(vFrom instanceof Long)) {
                throw new DomainCanNotBeDefined("Domain cannot be converted to Long", null);
            }

            min = Math.min(min, (Long) vFrom);
            max = Math.max(max, (Long) vTo - 1);
        }
        min = min < Integer.MIN_VALUE ? Integer.MIN_VALUE : min;
        min = min >= Integer.MAX_VALUE ? (Integer.MAX_VALUE - 1) : min;
        max = max < Integer.MIN_VALUE ? Integer.MIN_VALUE : max;
        max = max >= Integer.MAX_VALUE ? (Integer.MAX_VALUE - 1) : max;

        return new IntRangeDomain((int) min, (int) max);
    }

    List<IndexNode> mergeRulesByValue(List<IndexNode> nodes) {
        Collections.sort(nodes);
        final int length = nodes.size();
        DecisionTableRuleNodeBuilder builder = new DecisionTableRuleNodeBuilder();
        List<IndexNode> result = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            IndexNode node = nodes.get(i);
            builder.addRule(node.getRuleN());
            if (i == length - 1 || node.compareTo(nodes.get(i + 1)) != 0) {
                result.add(new IndexNode(node.getValue(), builder.makeRulesAry()));
                builder = new DecisionTableRuleNodeBuilder();
            }
        }
        return result;
    }

    @Override
    public boolean isIndexed() {
        return true;
    }

    @Override
    public int countUniqueKeys(ICondition condition, IIntIterator it) {
        return 0;
    }

    @Override
    public int getPriority() {
        return IConditionEvaluator.RANGE_CONDITION_PRIORITY;
    }

    protected static class RangeIndexNodeAdaptor implements IRangeAdaptor<IndexNode, Comparable<?>> {
        private final IRangeAdaptor<Object, ? extends Comparable<Object>> rangeAdaptor;

        RangeIndexNodeAdaptor(IRangeAdaptor<Object, ? extends Comparable<Object>> rangeAdaptor) {
            this.rangeAdaptor = rangeAdaptor;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Comparable<?> adaptValueType(Object value) {
            if (value == null) {
                throw new IllegalArgumentException("Null values is not supported.");
            }
            if (rangeAdaptor != null) {
                value = rangeAdaptor.adaptValueType(value);
            }
            return new IndexNode((Comparable<Object>) value);
        }

        @Override
        public Comparable<Object> getMax(IndexNode param) {
            throw new UnsupportedOperationException("Operation is not supported.");
        }

        @Override
        public Comparable<Object> getMin(IndexNode param) {
            throw new UnsupportedOperationException("Operation is not supported.");
        }

        @Override
        public boolean useOriginalSource() {
            throw new UnsupportedOperationException("Operation not supported.");
        }

    }

    public static class IndexNode implements Comparable<IndexNode> {
        private final Comparable<Object> value;
        private int[] rules;
        private int ruleN;

        IndexNode(Comparable<Object> value, int ruleN) {
            this.value = value;
            this.ruleN = ruleN;
        }

        IndexNode(Comparable<Object> value, int[] rules) {
            this.value = value;
            this.rules = rules;
        }

        IndexNode(Comparable<Object> value) {
            this.value = value;
        }

        public Comparable<Object> getValue() {
            return value;
        }

        int getRuleN() {
            return ruleN;
        }

        public int[] getRules() {
            return rules;
        }

        @Override
        public int compareTo(IndexNode o) {
            if (this.value == o.value) {
                return 0;
            } else if (this.value == null) {
                return -1;
            } else if (o.value == null) {
                return 1;
            }
            return this.value.compareTo(o.value);
        }
    }
}
