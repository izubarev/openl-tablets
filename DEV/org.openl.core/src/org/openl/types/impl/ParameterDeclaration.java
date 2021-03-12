package org.openl.types.impl;

import java.util.Objects;

import org.openl.types.IOpenClass;
import org.openl.types.IParameterDeclaration;
import org.openl.util.ClassUtils;

/**
 * @author snshor
 *
 */
public class ParameterDeclaration implements IParameterDeclaration {

    private final IOpenClass type;
    private final String name;
    private final boolean complete;

    public ParameterDeclaration(IOpenClass type, String name) {
        this(type, name, true);
    }

    public ParameterDeclaration(IOpenClass type, String name, boolean complete) {
        this.type = type;
        this.name = name;
        this.complete = complete;
    }

    @Override
    public boolean isComplete() {
        return complete;
    }

    @Override
    public String getDisplayName(int mode) {
        return name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public IOpenClass getType() {
        return type;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ParameterDeclaration)) {
            return false;
        }
        ParameterDeclaration paramDecl = (ParameterDeclaration) obj;

        return Objects.equals(name, paramDecl.name) && Objects.equals(type, paramDecl.getType());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }

    @Override
    public String toString() {
        return ClassUtils.getShortClassName(type.getInstanceClass()) + " " + name;
    }

}
