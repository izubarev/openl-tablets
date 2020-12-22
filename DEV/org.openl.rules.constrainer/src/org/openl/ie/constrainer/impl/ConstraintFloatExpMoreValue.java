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
//: ConstraintFloatExpMoreValue.java
//
/**
 * An implementation of the constraint: <code>FloatExp >= value</code>.
 */
public final class ConstraintFloatExpMoreValue extends ConstraintImpl {
    // PRIVATE MEMBERS
    private final FloatExp _exp;
    private final double _value;
    private final Constraint _opposite;

    public ConstraintFloatExpMoreValue(FloatExp exp, double value) {
        super(exp.constrainer(), "");// exp.name()+">="+value);
        _name = "";
        if (constrainer().showInternalNames()) {
            _name = exp.name() + ">=" + value;
        }
        _exp = exp;
        _value = value;
        _opposite = null;
    }

    @Override
    public Goal execute() throws Failure {
        class ObserverFloatMoreValue extends Observer {
            @Override
            public Object master() {
                return ConstraintFloatExpMoreValue.this;
            }

            @Override
            public int subscriberMask() {
                return EventOfInterest.VALUE | EventOfInterest.MAX;
            }

            @Override
            public String toString() {
                return "ObserverFloatMoreValue";
            }

            @Override
            public void update(Subject exp, EventOfInterest interest) throws Failure {
                // Debug.on();Debug.print("ObserverFloatMoreValue:
                // "+interest);Debug.off();
                FloatEvent event = (FloatEvent) interest;
                if (FloatCalc.gt(_value, event.max())) {
                    // System.out.println("???ObserverFloatMoreValue: max <
                    // value: "+event.max()+"<"+_value);
                    exp.constrainer().fail("from ObserverFloatMoreValue");
                }

                _exp.setMin(_value);
            }

        } // ~ ObserverFloatMoreValue

        _exp.setMin(_value); // may fail
        _exp.attachObserver(new ObserverFloatMoreValue());
        return null;
    }

    @Override
    public String toString() {
        return _exp + ">=" + _value;
    }

} // eof ConstraintFloatExpMoreValue
