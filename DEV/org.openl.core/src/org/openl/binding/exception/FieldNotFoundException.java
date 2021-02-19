/*
 * Created on Jun 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.exception;

import org.openl.exception.OpenLCompilationException;
import org.openl.types.IOpenClass;

/**
 * @author snshor
 *
 */
public class FieldNotFoundException extends OpenLCompilationException {

    private static final long serialVersionUID = 1L;

    private final String fieldName;

    private final IOpenClass type;

    public FieldNotFoundException(String msg, String fieldName, IOpenClass type) {
        super(msg);
        this.fieldName = fieldName;
        this.type = type;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder();
        if (super.getMessage() != null) {
            sb.append(super.getMessage());
        }

        sb.append(type != null && type.isStatic() ? "Static Field '" : "Field '");
        sb.append(fieldName).append("' is not found");
        if (type != null) {
            sb.append(" in type '").append(type.getName());
        }
        sb.append("'.");
        return sb.toString();
    }
}
