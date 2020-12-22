package org.openl.rules.lang.xls.syntax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.openl.CompiledOpenClass;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.types.IOpenMethod;

public class TestTableSyntaxNodeKey extends BaseOpenlBuilderHelper {
    private final static String SRC = "test/rules/OverloadedTables_Test.xls";
    private final List<TableSyntaxNode> driverAgeTypeTables = new ArrayList<>();
    private final List<TableSyntaxNode> driverEligibilityTables = new ArrayList<>();

    public TestTableSyntaxNodeKey() {
        super(SRC);
    }

    @Before
    public void getTables() {
        CompiledOpenClass compiledOpenClass = getCompiledOpenClass();
        XlsMetaInfo xmi = (XlsMetaInfo) compiledOpenClass.getOpenClassWithErrors().getMetaInfo();
        XlsModuleSyntaxNode xsn = xmi.getXlsModuleNode();
        TableSyntaxNode[] tsns = xsn.getXlsTableSyntaxNodes();
        for (TableSyntaxNode tsn : tsns) {
            if (tsn.getMember() instanceof IOpenMethod && tsn.getType().equals("xls.dt")) {
                if (tsn.getMember().getName().equals("driverAgeType")) {
                    driverAgeTypeTables.add(tsn);
                } else if (tsn.getMember().getName().equals("driverEligibility")) {
                    driverEligibilityTables.add(tsn);
                }
            }
        }
    }

    @Test
    public void testEquals() {
        assertEquals(new TableSyntaxNodeKey(driverAgeTypeTables.get(0)),
                new TableSyntaxNodeKey(driverAgeTypeTables.get(1)));
        assertFalse(new TableSyntaxNodeKey(driverEligibilityTables.get(0)).equals(new TableSyntaxNodeKey(
                driverEligibilityTables.get(1))));

    }

    @Test
    public void testHashCode() {
        // same hash codes for equal objects
        assertEquals(new TableSyntaxNodeKey(driverAgeTypeTables.get(0)).hashCode(),
            new TableSyntaxNodeKey(driverAgeTypeTables.get(1)).hashCode());
    }
}
