package org.openl.types.java;

import java.util.Arrays;
import java.util.Iterator;

import org.openl.domain.IDomain;
import org.openl.domain.IType;

public class JavaEnumDomain implements IDomain<Object> {

    private final JavaOpenEnum enumClass;

    public JavaEnumDomain(JavaOpenEnum enumClass) {
        this.enumClass = enumClass;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Iterator iterator() {
        return Arrays.asList(enumClass.getInstanceClass().getEnumConstants()).iterator();
    }

    public int size() {
        return enumClass.getInstanceClass().getEnumConstants().length;
    }

    @Override
    public IType getElementType() {
        return enumClass;
    }

    @Override
    public boolean selectObject(Object obj) {
        return enumClass.getInstanceClass().isInstance(obj);
    }

    public Object getValue(int index) {
        return enumClass.getInstanceClass().getEnumConstants()[index];
    }

    public JavaOpenEnum getEnumClass() {
        return enumClass;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        @SuppressWarnings("rawtypes")
        Iterator itr = iterator();
        boolean f = false;
        while (itr.hasNext()) {
            Object v = itr.next();
            if (f) {
                sb.append(", ");
            } else {
                f = true;
            }
            sb.append(v.toString());
        }
        return "[" + sb.toString() + "]";
    }

}
