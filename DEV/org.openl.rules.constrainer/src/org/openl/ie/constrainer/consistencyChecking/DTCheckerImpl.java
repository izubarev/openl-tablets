package org.openl.ie.constrainer.consistencyChecking;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */
import java.util.ArrayList;
import java.util.List;

import org.openl.ie.constrainer.*;

public class DTCheckerImpl implements DTChecker {
    static public class CDecisionTableImpl implements CDecisionTable {
        private final IntBoolExp[] _rules;
        private final IntExpArray _vars;
        boolean overrideAscending;

        public CDecisionTableImpl(IntBoolExp[][] data, IntExpArray vars, boolean overrideAscending) {
            if (data == null) {
                throw new IllegalArgumentException(
                    "DecisionTableImpl(IntBoolExp[][] _data, IntExpArray vars) : " +
                        "cannot be created based on null data array.");
            }
            _vars = vars;
            this.overrideAscending = overrideAscending;
            int nbRules = data.length;
            _rules = new IntBoolExp[nbRules];
            java.util.Arrays.fill(_rules, new IntBoolExpConst(_vars.constrainer(), true));
            for (int i = 0; i < data.length; i++) {
                int nbVars = data[i].length;
                for (int j = 0; j < nbVars; j++) {
                    _rules[i] = _rules[i].and(data[i][j]);
                }
            }
        }

        @Override
        public IntBoolExp getRule(int i) {
            return _rules[i];
        }

        @Override
        public IntBoolExp[] getRules() {
            return _rules;
        }

        @Override
        public IntVar getVar(int i) {
            return (IntVar) _vars.get(i);
        }

        @Override
        public IntExpArray getVars() {
            return _vars;
        }

        @Override
        public boolean isOverrideAscending() {
            return overrideAscending;
        }
    }

    private class CompletenessCheckerImpl implements CompletenessChecker {
        private class GoalSaveSolutions extends GoalImpl {
            private static final long serialVersionUID = -4747909482843265994L;

            public GoalSaveSolutions(Constrainer c) {
                super(c);
            }

            @Override
            public Goal execute() throws Failure {
                _uncoveredRegions.add(new Uncovered(_dt.getVars()));
                return null;
            }
        }

        @Override
        public List<Uncovered> check() {
            IntBoolExp[] rules = _dt.getRules();
            Constrainer c = rules[0].constrainer();
            int stackSize = c.getStackSize();
            IntExpArray ruleArray = new IntExpArray(c, rules.length);
            for (int i = 0; i < rules.length; i++) {
                ruleArray.set(rules[i], i);
            }
            Constraint incompleteness = ruleArray.sum().equals(0);
            Goal save = new GoalSaveSolutions(c);
            Goal generate = new GoalGenerate(_dt.getVars());
            Goal target = new GoalAnd(new GoalAnd(incompleteness, generate), save);
            c.execute(target, true);
            c.backtrackStack(stackSize);
            return _uncoveredRegions;
        }
    }

    private CDecisionTable _dt;
    private final CompletenessChecker _cpChecker = new CompletenessCheckerImpl();

    private final OverlappingChecker _opChecker; // = new OverlappingCheckerImpl();

    private final List<Uncovered> _uncoveredRegions = new ArrayList<>();

    public DTCheckerImpl(CDecisionTable dtable) {
        _dt = dtable;
        _opChecker = new OverlappingCheckerImpl2(_dt);
    }

    @Override
    public List<Uncovered> checkCompleteness() {
        return _cpChecker.check();
    }

    @Override
    public List<Overlapping> checkOverlappings() {
        return _opChecker.check();
    }

    @Override
    public CDecisionTable getDT() {
        return _dt;
    }

    @Override
    public void setDT(CDecisionTable dtable) {
        _dt = dtable;
    }

}
