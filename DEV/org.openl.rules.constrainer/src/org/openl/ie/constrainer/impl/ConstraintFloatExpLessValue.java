package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.*;

///////////////////////////////////////////////////////////////////////////////
/*
 * Copyright Exigen Group 1998, 1999, 2000
 * 320 Amboy Ave., Metuchen, NJ, 08840, USA, www.exigengroup.com
 *
 * The copyright to the computer program(s) herein
 * is the property of Exigen Group, USA. All rights reserved.
 * The program(s) may be used and/or copied only with
 * the written permission of Exigen Group
 * or in accordance with the terms and conditions
 * stipulated in the agreement/contract under which
 * the program(s) have been supplied.
 */
///////////////////////////////////////////////////////////////////////////////
//
//: ConstraintFloatExpLessValue.java
//
/**
 * An implementation of the constraint: <code>FloatExp <= value</code>.
 */
public final class ConstraintFloatExpLessValue extends ConstraintImpl {
    // PRIVATE MEMBERS
    private final FloatExp _exp;
    private double _value;
    private final Constraint _opposite;

    public ConstraintFloatExpLessValue(FloatExp exp, double value) {
        super(exp.constrainer(), "");// exp.name()+"<="+value);
        _name = "";
        if (constrainer().showInternalNames()) {
            _name = exp.name() + "<=" + value;
        }
        _exp = exp;
        _value = value;
        _opposite = null;
    }

    @Override
    public Goal execute() throws Failure {
        class ObserverFloatLessValue extends Observer {
            @Override
            public Object master() {
                return ConstraintFloatExpLessValue.this;
            }

            @Override
            public int subscriberMask() {
                return EventOfInterest.VALUE | EventOfInterest.MIN;
            }

            @Override
            public String toString() {
                return "ObserverFloatLessValue";
            }

            @Override
            public void update(Subject exp, EventOfInterest interest) throws Failure {
                // Debug.on();Debug.print("ObserverFloatLessValue:
                // "+interest);Debug.off();
                FloatEvent event = (FloatEvent) interest;
                if (FloatCalc.gt(event.min(), _value)) {
                    exp.constrainer().fail("from ObserverFloatLessValue");
                }

                _exp.setMax(_value);
            }

        } // ~ ObserverFloatLessValue

        _exp.setMax(_value); // may fail
        _exp.attachObserver(new ObserverFloatLessValue());
        return null;
    }

    /*
     * resetValue(double v) throws Failure { _value = v; _exp.setMax(_value); }
     */
    public void resetValue(double v) {
        _value = v;
    }

    @Override
    public String toString() {
        return _exp + "<=" + _value;
    }

} // ~ ConstraintFloatExpLessValue
