/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */

package org.apache.poi.hssf.record.formula.functions;

import org.apache.poi.hssf.record.formula.eval.AreaEval;
import org.apache.poi.hssf.record.formula.eval.ErrorEval;
import org.apache.poi.hssf.record.formula.eval.NumberEval;
import org.apache.poi.hssf.record.formula.eval.RefEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
//ZS
import org.apache.poi.ss.formula.ArrayEval;
// end changes ZS
/**
 * Implementation for Excel ROWS function.
 *
 * @author Josh Micich
 * @author zsulkins(ZS)- array support
 */
// ZS
public final class Rows extends Fixed1ArgFunction implements FunctionWithArraySupport{
// end changes ZS
	public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0) {

		int result;
		// !!changed ZS
		if (arg0 instanceof ArrayEval){
			arg0 = ((ArrayEval)arg0).arrayAsArea();
		}
		// end change
		if (arg0 instanceof AreaEval) {
			result = ((AreaEval) arg0).getHeight();
		} else if (arg0 instanceof RefEval) {
			result = 1;
		} else { // anything else is not valid argument
			return ErrorEval.VALUE_INVALID;
		}
		return new NumberEval(result);
	}
	
	// ZS	
	/* (non-Javadoc)
	 * @see org.apache.poi.hssf.record.formula.functions.FunctionWithArraySupport#supportArray(int)
	 */
	public boolean supportArray(int paramIndex){
		return true;
	}
// end changes ZS	
}
