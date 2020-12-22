package org.openl.codegen.tools.type;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.expressions.match.ContainsMatchingExpression;
import org.openl.rules.table.properties.expressions.match.EQMatchingExpression;

public class TablePropertyDefinitionWrappers {

    private final List<TablePropertyDefinitionWrapper> wrappers = new ArrayList<>();

    public TablePropertyDefinitionWrappers(TablePropertyDefinition[] definitions) {
        init(definitions);
    }

    private void init(TablePropertyDefinition[] definitions) {

        for (TablePropertyDefinition definition : definitions) {
            TablePropertyDefinitionWrapper wrapper = new TablePropertyDefinitionWrapper(definition);
            wrappers.add(wrapper);
        }
    }

    public List<TablePropertyDefinitionWrapper> asList() {
        return new ArrayList<>(wrappers);
    }

    public List<TablePropertyDefinitionWrapper> getDimensionalProperties(Selector selector) {

        List<TablePropertyDefinitionWrapper> dimensionalTablePropertyDefinitions = new ArrayList<>();

        for (TablePropertyDefinitionWrapper wrapper : wrappers) {

            if (wrapper.getDefinition().isDimensional() && selector != null && selector.suits(wrapper)) {
                dimensionalTablePropertyDefinitions.add(wrapper);
            }
        }

        return dimensionalTablePropertyDefinitions;
    }

    public List<TablePropertyDefinitionWrapper> getDimensionalPropertiesWithContextVar() {

        return getDimensionalProperties(wrapper -> {
            // The tablePropertyDefinition suits if the context variable is not empty
            //
            return wrapper.getContextVar() != null;
        });
    }

    public List<TablePropertyDefinitionWrapper> getDimensionalPropertiesWithMatchExpression() {
        return getDimensionalProperties(wrapper -> {
            // The tablePropertyDefinition suits if the match expression is not empty
            //
            return wrapper.getDefinition().getExpression() != null;
        });
    }

    public List<TablePropertyDefinitionWrapper> getGapOverlapDimensionalProperties() {

        List<TablePropertyDefinitionWrapper> dimensionalTablePropertyDefinitions = new ArrayList<>();

        for (TablePropertyDefinitionWrapper wrapper : wrappers) {

            if (wrapper.getDefinition().isDimensional()) {
                String operation = wrapper.getOperation();
                if (ContainsMatchingExpression.OPERATION_NAME
                    .equalsIgnoreCase(operation) || EQMatchingExpression.OPERATION_NAME.equalsIgnoreCase(operation)) {
                    dimensionalTablePropertyDefinitions.add(wrapper);
                }
            }
        }

        return dimensionalTablePropertyDefinitions;
    }

    public interface Selector {
        boolean suits(TablePropertyDefinitionWrapper wrapper);
    }
}
