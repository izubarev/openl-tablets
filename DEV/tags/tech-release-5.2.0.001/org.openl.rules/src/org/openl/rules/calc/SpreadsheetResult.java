package org.openl.rules.calc;

import java.util.Map;

import org.openl.types.IDynamicObject;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.impl.DynamicObject;
import org.openl.util.print.NicePrinter;
import org.openl.vm.IRuntimeEnv;

public class SpreadsheetResult implements IDynamicObject 
{
	
	boolean cacheResult = true;

	Spreadsheet spreadsheet;
	
	IDynamicObject targetModule; // OpenL module
	Object[] params;  // copy of the spreadsheet call params
	
	IRuntimeEnv env; //copy of the call environment
	
	
	Object[][] results;
	
	
	
	public SpreadsheetResult(Spreadsheet spreadsheet, IDynamicObject targetModule,
			Object[] params, IRuntimeEnv env) {
		super();
		this.spreadsheet = spreadsheet;
		
		this.targetModule = targetModule;
		this.params = params;
		this.env = env;
		results = new Object[spreadsheet.height()][spreadsheet.width()];
	}

	public Object getFieldValue(String name) 
	{
		
		IOpenField f = spreadsheet.getSpreadsheetType().getField(name);
		
		if (f == null)
			return targetModule.getFieldValue(name);

		SCellField sfield = (SCellField)f;
		
		int row = sfield.getCell().getRow();
		int column = sfield.getCell().getColumn();
		
		return getValue(row, column);
	}

	public IOpenClass getType() {
		return spreadsheet.getSpreadsheetType();
	}

	public void setFieldValue(String name, Object value) {
		// TODO Auto-generated method stub
		
	}

	public Object getColumn(int column, IRuntimeEnv env2) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object getRow(int row, IRuntimeEnv env) {
		// TODO Auto-generated method stub
		return null;
	}

	  public String toString()
	  {
	    NicePrinter printer = new NicePrinter();
	    printer.print(this, DynamicObject.getNicePrinterAdaptor());
	    return printer.getBuffer().toString();
	  }

	public Map<String, Object> getFieldValues() 
	{
		throw new UnsupportedOperationException("Should not be called, this is only used in NicePrinter");
	}

	public Spreadsheet getSpreadsheet() {
		return spreadsheet;
	}

	public final int width() {
		return spreadsheet.width();
	}
	public final int height() {
		return spreadsheet.height();
	}

	public Object getValue(int row, int column) {
		Object result = null;
		if (cacheResult)
		{
			
			result = results[row][column];
			if (result != null)
				return result;
		}	

		
		
		result = spreadsheet.cells[row][column].calculate(this, targetModule, params, env);
		
		results[row][column] = result;
		
		return result;
	}

	
}
