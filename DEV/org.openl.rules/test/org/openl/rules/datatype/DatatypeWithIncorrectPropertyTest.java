package org.openl.rules.datatype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.openl.CompiledOpenClass;
import org.openl.rules.BaseOpenlBuilderHelper;

public class DatatypeWithIncorrectPropertyTest extends BaseOpenlBuilderHelper {
    public DatatypeWithIncorrectPropertyTest() {
        super("test/rules/datatype/DatatypeWithIncorrectProperty.xlsx");
    }

    @Test
    public void testHaveOnlyOneError() {
        CompiledOpenClass compiledOpenClass = getCompiledOpenClass();

        assertTrue("Expected an error in the project", compiledOpenClass.hasErrors());
        assertEquals("Datatype must have only one error", 1, compiledOpenClass.getAllMessages().size());
        assertEquals("Incorrect error message",
            "Property 'precision' cannot be defined in 'Datatype' table.",
            compiledOpenClass.getAllMessages().iterator().next().getSummary());
    }
}
