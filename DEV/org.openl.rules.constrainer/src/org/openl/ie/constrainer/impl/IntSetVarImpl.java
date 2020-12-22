package org.openl.ie.constrainer.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.openl.ie.constrainer.*;
import org.openl.ie.tools.Reusable;
import org.openl.ie.tools.ReusableFactory;

public class IntSetVarImpl extends SubjectImpl implements IntSetVar {

    class ElementsObserver extends Observer {
        private final int _val;

        public ElementsObserver(int val) {
            _val = val;
        }

        @Override
        public Object master() {
            return IntSetVarImpl.this;
        }

        @Override
        public int subscriberMask() {
            return IntEvent.Constants.ALL;
        }

        @Override
        public void update(Subject exp, EventOfInterest event) throws Failure {
            constrainer().addUndo(UndoPossibleSetReduction.getUndo(IntSetVarImpl.this));
            _unboundsCounter--;
            IntEvent e = (IntEvent) event;
            int valueMask = 0;
            if (_unboundsCounter == 0) {
                valueMask = IntSetEvent.IntSetEventConstants.VALUE;
            }
            if (e.max() == 1) {
                // TRUE
                notifyObservers(IntSetEvent
                    .getEvent(IntSetVarImpl.this, _val, IntSetEvent.IntSetEventConstants.REQUIRE | valueMask));
            } else {
                // FALSE
                notifyObservers(IntSetEvent
                    .getEvent(IntSetVarImpl.this, _val, IntSetEvent.IntSetEventConstants.REMOVE | valueMask));
            }
        } // ~update()
    }

    static public class UndoPossibleSetReduction extends UndoImpl {

        static final ReusableFactory _factory = new ReusableFactory() {
            @Override
            protected Reusable createNewElement() {
                return new UndoPossibleSetReduction();
            }
        };

        static UndoPossibleSetReduction getUndo(IntSetVarImpl var) {
            UndoPossibleSetReduction undo = (UndoPossibleSetReduction) _factory.getElement();
            undo.undoable(var);
            return undo;
        }

        @Override
        public String toString() {
            return "UndoPossibleSetReduction " + undoable();
        }

        @Override
        public void undo() {
            IntSetVarImpl var = (IntSetVarImpl) undoable();
            var._unboundsCounter++;
            super.undo();
        }
    }

    private IntExpArray _set;

    private final HashMap _values2index = new HashMap();

    private int _unboundsCounter;

    private IntSetVarImpl(Constrainer C) {
        super(C);
    }

    public IntSetVarImpl(Constrainer C, int[] array) {
        this(C, array, "");
    }

    public IntSetVarImpl(Constrainer C, int[] array, String name) {
        super(C, name);
        int size = array.length;
        _set = new IntExpArray(C, size);
        for (int i = 0; i < size; i++) {
            _set.set(C.addIntBoolVarInternal(name() + "[" + array[i] + "]"), i);
            _set.get(i).attachObserver(new ElementsObserver(array[i]));
            _values2index.put(array[i], i);
        }
        _unboundsCounter = size;
    }

    @Override
    public boolean bound() {
        for (int i = 0; i < _set.size(); i++) {
            if (!_set.get(i).bound()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean contains(Set anotherSet) {
        if (!_values2index.keySet().containsAll(anotherSet)) {
            return false;
        }
        for (Object o : anotherSet) {
            int val = ((Integer) o);
            if (!possible(val)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Goal generate() {
        return new GoalGenerate(_set);
    }

    private IntBoolVar hasElem(int i) {
        int idx = ((Integer) _values2index.get(i));
        return (IntBoolVar) _set.get(idx);
    }

    @Override
    public IntSetVar intersectionWith(IntSetVar anotherSet) {
        if (anotherSet instanceof IntSetVarImpl) {
            return intersectionWith((IntSetVarImpl) anotherSet);
        } else {
            return anotherSet.intersectionWith(this);
        }
    }

    public IntSetVar intersectionWith(IntSetVarImpl anotherSet) {

        Set values1 = anotherSet._values2index.keySet(), values2 = _values2index.keySet();

        int[] tmp = new int[values1.size()];

        Iterator iter = values1.iterator();
        int counter = 0;
        while (iter.hasNext()) {
            Integer curValue = (Integer) iter.next();
            if (values2.contains(curValue)) {
                tmp[counter++] = curValue;
            }
        }
        /** @todo add emptiness check */

        int[] intersection = new int[counter];
        System.arraycopy(tmp, 0, intersection, 0, counter);
        IntSetVarImpl result = (IntSetVarImpl) constrainer().addIntSetVar(intersection);
        for (int val : intersection) {
            try {
                result.hasElem(val).equals(hasElem(val).and(anotherSet.hasElem(val))).execute();
            } catch (Failure f) {/* it would be never thrown */
            }
        }
        return result;
    }

    @Override
    public boolean possible(int value) {
        return hasElem(value).max() == 1;
    }

    @Override
    public void propagate() throws Failure {
    }

    @Override
    public void remove(int val) throws Failure {
        hasElem(val).setFalse();
    }

    @Override
    public void require(int val) throws Failure {
        hasElem(val).setTrue();
    }

    @Override
    public boolean required(int value) {
        return hasElem(value).min() == 1;
    }

    @Override
    public Set requiredSet() {
        java.util.HashSet values = new java.util.HashSet();
        for (Object o : _values2index.keySet()) {
            Integer curValue = (Integer) o;
            if (hasElem(curValue).min() == 1) {
                values.add(curValue);
            }
        }
        /** @todo add emptiness chrecking */
        return values;
    }

    @Override
    public IntSetVar unionWith(IntSetVar anotherSet) {
        if (anotherSet instanceof IntSetVarImpl) {
            return unionWith((IntSetVarImpl) anotherSet);
        } else {
            return anotherSet.unionWith(this);
        }
    }

    public IntSetVar unionWith(IntSetVarImpl anotherSet) {
        Set values1 = _values2index.keySet(), values2 = anotherSet._values2index.keySet();

        int[] tmp = new int[values1.size() + values2.size()];
        int counter = 0;
        Iterator iter = values1.iterator();
        while (iter.hasNext()) {
            tmp[counter++] = ((Integer) iter.next());
        }
        iter = values2.iterator();
        while (iter.hasNext()) {
            Integer curValue = (Integer) iter.next();
            if (!values1.contains(curValue)) {
                tmp[counter++] = curValue;
            }
        }

        int[] union = new int[counter];
        System.arraycopy(tmp, 0, union, 0, counter);

        IntSetVarImpl result = (IntSetVarImpl) constrainer().addIntSetVar(union);

        for (int val : union) {
            if (values1.contains(val)) {
                if (values2.contains(val)) {
                    try {
                        result.hasElem(val).equals(hasElem(val).or(anotherSet.hasElem(val))).execute();
                    } catch (Failure f) {/* it would be never thrown */
                    }
                } else {
                    try {
                        result.hasElem(val).equals(hasElem(val)).execute();
                    } catch (Failure f) {/* it would be never thrown */
                    }
                }
            } else {
                try {
                    result.hasElem(val).equals(anotherSet.hasElem(val)).execute();
                } catch (Failure f) {/* it would be never thrown */
                }
            }
        }
        return result;
    }

    @Override
    public Set value() throws Failure {
        if (!bound()) {
            constrainer().fail("Attempt to get value of the unbound variable " + this);
        }
        return requiredSet();
    }

}