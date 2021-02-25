package org.openl.rules.property;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Map;

import org.junit.Test;
import org.openl.CompiledOpenClass;
import org.openl.message.Severity;
import org.openl.rules.enumeration.RegionsEnum;
import org.openl.rules.enumeration.UsRegionsEnum;
import org.openl.rules.enumeration.ValidateDTEnum;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.PropertiesHelper;
import org.openl.rules.table.properties.inherit.InheritanceLevel;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * Test for properties recognition in execution mode.
 *
 * @author PUdalau
 */
public class PropertiesTableInExecutionModeTest {

    private static final String SRC = "test/rules/PropertyTableTest.xls";

    @Test
    public void testPropertyTableLoading() {
        RulesEngineFactory<?> engineFactory = new RulesEngineFactory<>(SRC);
        engineFactory.setExecutionMode(true);
        CompiledOpenClass compiledOpenClass = engineFactory.getCompiledOpenClass();
        assertEquals(1, compiledOpenClass.getAllMessages().stream().filter(msg -> Severity.ERROR == msg.getSeverity()).count());//FIXME validation issue for Ranges.
        IOpenMethod method = compiledOpenClass.getOpenClassWithErrors()
            .getMethod("hello1", new IOpenClass[] { JavaOpenClass.INT });
        if (method != null) {
            ITableProperties tableProperties = PropertiesHelper.getTableProperties(method);
            assertNotNull(tableProperties);

            Map<String, Object> moduleProperties = tableProperties.getModuleProperties();
            assertEquals(3, moduleProperties.size());
            assertEquals(InheritanceLevel.MODULE.getDisplayName(), moduleProperties.get("scope"));
            assertEquals("Any phase", moduleProperties.get("buildPhase"));
            assertEquals(ValidateDTEnum.ON, moduleProperties.get("validateDT"));

            Map<String, Object> categoryProperties = tableProperties.getCategoryProperties();
            assertEquals(4, categoryProperties.size());
            assertEquals(InheritanceLevel.CATEGORY.getDisplayName(), categoryProperties.get("scope"));
            assertEquals("newLob", ((String[]) categoryProperties.get("lob"))[0]);
            assertEquals(UsRegionsEnum.SE.name(), ((UsRegionsEnum[]) categoryProperties.get("usregion"))[0].name());
            assertEquals(RegionsEnum.NCSA.name(), ((RegionsEnum[]) categoryProperties.get("region"))[0].name());

            Map<String, Object> defaultProperties = tableProperties.getDefaultProperties();
            // assertTrue(defaultProperties.size() == 5);
            // assertEquals("US",(String) defaultProperties.get("country"));

            assertTrue((Boolean) defaultProperties.get("active"));
            assertFalse((Boolean) defaultProperties.get("failOnMiss"));
        } else {
            fail();
        }
    }

    @Test
    public void testFieldsInOpenClass() {
        RulesEngineFactory<?> engineFactory = new RulesEngineFactory<>(SRC);
        engineFactory.setExecutionMode(true);
        CompiledOpenClass compiledOpenClass = engineFactory.getCompiledOpenClass();
        assertEquals(1, compiledOpenClass.getAllMessages().stream().filter(msg -> Severity.ERROR == msg.getSeverity()).count());//FIXME validation issue for Ranges.
        Collection<IOpenField> fields = compiledOpenClass.getOpenClassWithErrors().getFields();
        // properties table with name will be represented as field
        assertTrue(fields.stream().anyMatch(e -> "categoryProp".equals(e.getName())));
        // properties table without name will not be represented as field
        IRuntimeEnv env = engineFactory.getOpenL().getVm().getRuntimeEnv();
        for (IOpenField field : fields) {
            if (field instanceof PropertiesOpenField) {
                ITableProperties properties = (ITableProperties) field
                    .get(compiledOpenClass.getOpenClassWithErrors().newInstance(env), env);
                String scope = properties.getScope();
                assertFalse(InheritanceLevel.MODULE.getDisplayName().equalsIgnoreCase(scope));
            }
        }
    }
}
