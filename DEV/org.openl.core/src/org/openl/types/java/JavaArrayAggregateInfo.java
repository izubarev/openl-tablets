/*
 * Created on Jul 10, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.java;

import java.util.Iterator;

import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenIndex;
import org.openl.types.impl.AAggregateInfo;
import org.openl.types.impl.ArrayIndex;
import org.openl.util.AIndexedIterator;

/**
 * @author snshor
 *
 */
public class JavaArrayAggregateInfo extends AAggregateInfo {

    public static final IAggregateInfo ARRAY_AGGREGATE = new JavaArrayAggregateInfo();

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IAggregateInfo#getComponentType(org.openl.types.IOpenClass)
     */
    @Override
    public IOpenClass getComponentType(IOpenClass aggregateType) {
        Class<?> c = aggregateType.getInstanceClass().getComponentType();
        return c != null ? JavaOpenClass.getOpenClass(c) : null;
    }

    @Override
    public IOpenIndex getIndex(IOpenClass aggregateType, IOpenClass indexType) {
        if (indexType != JavaOpenClass.INT && indexType.getInstanceClass() != Integer.class) {
            return null;
        }

        if (!isAggregate(aggregateType)) {
            return null;
        }

        return new ArrayIndex(JavaOpenClass.getOpenClass(aggregateType.getInstanceClass().getComponentType()));
    }

    @Override
    public Iterator<Object> getIterator(Object aggregate) {
        return AIndexedIterator.fromArrayObj(aggregate);
    }

    @Override
    public boolean isAggregate(IOpenClass type) {
        return type.getInstanceClass().isArray();
    }

}
