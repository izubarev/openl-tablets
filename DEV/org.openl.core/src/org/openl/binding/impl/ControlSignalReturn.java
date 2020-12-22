/*
 * Created on Jul 28, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

/**
 * @author snshor
 *
 */
public class ControlSignalReturn extends ControlSignal {

    final Object returnValue;

    public ControlSignalReturn(Object returnValue) {
        this.returnValue = returnValue;
    }

    /**
     * @return
     */
    public Object getReturnValue() {
        return returnValue;
    }

}
