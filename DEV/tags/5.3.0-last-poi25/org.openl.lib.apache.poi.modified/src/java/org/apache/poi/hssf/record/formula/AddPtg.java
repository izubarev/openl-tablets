/* ====================================================================
 Copyright 2003-2004   Apache Software Foundation

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 ==================================================================== */

/*
 * AddPtg.java
 *
 * Created on October 29, 2001, 7:48 PM
 */
package org.apache.poi.hssf.record.formula;

import org.apache.poi.hssf.model.Workbook;

/**
 * Addition operator PTG the "+" binomial operator. If you need more explanation
 * than that then well...We really can't help you here.
 *
 * @author Andrew C. Oliver (acoliver@apache.org)
 * @author Jason Height (jheight at chariot dot net dot au)
 */

public class AddPtg extends OperationPtg {
    public final static int SIZE = 1;
    public final static byte sid = 0x03;

    private final static String ADD = "+";

    /** Creates new AddPtg */

    public AddPtg() {
    }

    public AddPtg(byte[] data, int offset) {

        // doesn't need anything
    }

    @Override
    public Object clone() {
        return new AddPtg();
    }

    @Override
    public byte getDefaultOperandClass() {
        return Ptg.CLASS_VALUE;
    }

    @Override
    public int getNumberOfOperands() {
        return 2;
    }

    @Override
    public int getSize() {
        return SIZE;
    }

    @Override
    public int getType() {
        return TYPE_BINARY;
    }

    /** implementation of method from OperationsPtg */
    @Override
    public String toFormulaString(String[] operands) {
        StringBuffer buffer = new StringBuffer();

        buffer.append(operands[0]);
        buffer.append(ADD);
        buffer.append(operands[1]);
        return buffer.toString();
    }

    /** Implementation of method from Ptg */
    @Override
    public String toFormulaString(Workbook book) {
        return "+";
    }

    @Override
    public void writeBytes(byte[] array, int offset) {
        array[offset + 0] = sid;
    }

}
