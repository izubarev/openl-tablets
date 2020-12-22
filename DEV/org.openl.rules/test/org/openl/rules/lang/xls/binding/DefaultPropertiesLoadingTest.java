package org.openl.rules.lang.xls.binding;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

public class DefaultPropertiesLoadingTest extends BaseOpenlBuilderHelper {

    private static final String SRC = "test/rules/DefaultPropertiesLoadingTest.xls";

    public DefaultPropertiesLoadingTest() {
        super(SRC);
    }

    @Test
    public void testLoadingDefaultValuesForPreviouslyEmptyProp() {
        String tableName = "Rules void hello1(int hour)";
        TableSyntaxNode resultTsn = findTable(tableName);
        if (resultTsn != null) {

            assertEquals("Check that number of properties defined in table is 0",
                resultTsn.getTableProperties().getTableProperties().size(),
                0);

            assertFalse("Tsn does not have properties defined in appropriate table in excel",
                    resultTsn.hasPropertiesDefinedInTable());
        } else {
            fail();
        }
    }
}
