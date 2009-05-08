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

package org.apache.poi.hssf.record.formula;

import org.apache.poi.hssf.model.Workbook;

/**
 * This PTG implements the standard binomial divide "/"
 *
 * @author Andrew C. Oliver acoliver at apache dot org
 * @author Jason Height (jheight at chariot dot net dot au)
 */

public class DividePtg extends OperationPtg {
    public final static int SIZE = 1;
    public final static byte sid = 0x06;

    /** Creates new AddPtg */

    public DividePtg() {
    }

    public DividePtg(byte[] data, int offset) {

        // doesn't need anything
    }

    @Override
    public Object clone() {
        DividePtg ptg = new DividePtg();
        return ptg;
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

    @Override
    public String toFormulaString(String[] operands) {
        StringBuffer buffer = new StringBuffer();

        buffer.append(operands[0]);
        buffer.append(toFormulaString((Workbook) null));
        buffer.append(operands[1]);
        return buffer.toString();
    }

    @Override
    public String toFormulaString(Workbook book) {
        return "/";
    }

    @Override
    public void writeBytes(byte[] array, int offset) {
        array[offset + 0] = sid;
    }
}
