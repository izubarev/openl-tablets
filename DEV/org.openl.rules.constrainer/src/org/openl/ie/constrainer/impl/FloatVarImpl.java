package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.*;
import org.openl.ie.tools.Reusable;
import org.openl.ie.tools.ReusableFactory;

/**
 * A generic implementation of the FloatVar interface.
 */
public class FloatVarImpl extends FloatExpImpl implements FloatVar {
    /**
     * Undo for FloatVar.
     */
    static class UndoFloatVar extends SubjectImpl.UndoSubject {
        static final ReusableFactory _factory = new ReusableFactory() {
            @Override
            protected Reusable createNewElement() {
                return new UndoFloatVar();
            }

        };

        private int _history_index;

        static UndoFloatVar getFloatVarUndo() {
            return (UndoFloatVar) _factory.getElement();
        }

        /**
         * Returns a String representation of this object.
         *
         * @return a String representation of this object.
         */
        @Override
        public String toString() {
            return "UndoFloatVar " + undoable();// +": min="+_min+" max="+_max;
        }

        /**
         * Execute undo() operation for this UndoFloatVar object.
         */
        @Override
        public void undo() {
            FloatVarImpl floatvar = (FloatVarImpl) undoable();
            // System.out.println("++ Undo: " + floatvar + " index:" +
            // _history_index);
            floatvar.history().restore(_history_index);
            super.undo();
            // System.out.println("-- Undo: " + floatvar + " index:" +
            // _history_index);
        }

        @Override
        public void undoable(Undoable u) {
            super.undoable(u);
            FloatVarImpl floatvar = (FloatVarImpl) u;
            _history_index = floatvar.history().currentIndex();
            // System.out.println("++ SAVE: " + floatvar + " index:" +
            // _history_index);
        }

    } // ~UndoFloatVar

    private final FloatDomain _domain;

    private final FloatDomainHistory _history;

    public FloatVarImpl(Constrainer constrainer, double min, double max, String name) {
        super(constrainer, name);
        _domain = new FloatDomainImpl(this, min, max);
        _history = new FloatDomainHistory(this);
    }

    @Override
    public Undo createUndo() {
        _history.saveUndo();
        return UndoFloatVar.getFloatVarUndo();
    }

    @Override
    public void forceMax(double max) {
        _domain.forceMax(max);
    }

    @Override
    public void forceMin(double min) {
        _domain.forceMin(min);
    }

    public FloatDomainHistory history() {
        return _history;
    }

    @Override
    public Goal instantiate() {
        return new GoalFloatInstantiate(this);
    }

    @Override
    public boolean isLinear() {
        return true;
    }

    @Override
    public double max() {
        return _domain.max();
    }

    @Override
    public double min() {
        return _domain.min();
    }

    @Override
    public void propagate() throws Failure {
        _history.propagate();
    }

    @Override
    public void setMax(double max) throws Failure {
        // Debug.print(this+".setMax("+max+")");
        if (_domain.setMax(max)) {
            _history.setMax(max);
            addToPropagationQueue();
        }
    }

    @Override
    public void setMin(double min) throws Failure {
        // Debug.print(this+".setMin("+min+")");
        if (_domain.setMin(min)) {
            _history.setMin(min);
            addToPropagationQueue();
        }
    }

    @Override
    public void setValue(double value) throws Failure {
        // Debug.print(this+".setValue("+value+")");
        if (_domain.setValue(value)) {
            _history.setMin(value);
            _history.setMax(value);
            addToPropagationQueue();
        }
    }

    @Override
    public double size() {
        return _domain.size();
    }

    @Override
    public double value() throws Failure {
        if (!bound()) {
            constrainer().fail("Attempt to get value of the unbound float expresion " + this);
        }
        return (min() + max()) / 2;
    }
} // ~FloatVar
