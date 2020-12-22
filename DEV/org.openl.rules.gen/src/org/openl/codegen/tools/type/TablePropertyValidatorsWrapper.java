package org.openl.codegen.tools.type;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.table.constraints.Constraint;
import org.openl.rules.table.constraints.Constraints;
import org.openl.rules.table.constraints.RegexpValueConstraint;
import org.openl.rules.table.constraints.UniqueActiveTableConstraint;
import org.openl.rules.table.constraints.UniqueInModuleConstraint;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.validation.ActivePropertyValidator;
import org.openl.rules.validation.RegexpPropertyValidator;
import org.openl.rules.validation.UniquePropertyValueValidator;
import org.openl.validation.IOpenLValidator;

public class TablePropertyValidatorsWrapper {

    private final List<Class<? extends IOpenLValidator>> validatorClasses = new ArrayList<>();
    private final String name;
    private String constraintsStr;
    private final Constraints constraints;

    TablePropertyValidatorsWrapper(TablePropertyDefinition tablePropertyDefinition) {
        name = tablePropertyDefinition.getName();
        constraints = tablePropertyDefinition.getConstraints();

        if (constraints != null) {
            constraintsStr = constraints.getConstraintsStr();

            List<Constraint> constraints = this.constraints.getAll();
            for (Constraint constraint : constraints) {
                if (constraint instanceof UniqueActiveTableConstraint) {
                    validatorClasses.add(ActivePropertyValidator.class);
                } else if (constraint instanceof UniqueInModuleConstraint) {
                    validatorClasses.add(UniquePropertyValueValidator.class);
                } else if (constraint instanceof RegexpValueConstraint) {
                    validatorClasses.add(RegexpPropertyValidator.class);
                }
            }
        }
    }

    public List<Class<? extends IOpenLValidator>> getValidatorClasses() {
        return validatorClasses;
    }

    public String getPropertyName() {
        return name;
    }

    public String getPropertyConstraints() {
        return constraintsStr;
    }

    public String getPropertyConstraints(Class<?> validatorClass) {
        return constraints.get(validatorClasses.indexOf(validatorClass)).getValue();
    }
}
