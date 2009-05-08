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
 *
 * @author andy
 * @author Jason Height (jheight at chariot dot net dot au)
 */

public class UnknownPtg extends Ptg {
    private short size;

    /** Creates new UnknownPtg */

    public UnknownPtg() {
    }

    public UnknownPtg(byte[] data, int offset) {

        // doesn't need anything
    }

    @Override
    public Object clone() {
        return new UnknownPtg();
    }

    @Override
    public byte getDefaultOperandClass() {
        return Ptg.CLASS_VALUE;
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public String toFormulaString(Workbook book) {
        return "UNKNOWN";
    }

    @Override
    public void writeBytes(byte[] array, int offset) {
    }

}
