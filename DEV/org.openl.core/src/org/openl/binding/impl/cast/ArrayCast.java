package org.openl.binding.impl.cast;

import java.lang.reflect.Array;

import org.openl.types.IOpenClass;

final class ArrayCast implements IOpenCast {

    private final IOpenClass toComponentType;
    private final IOpenCast openCast;
    private final int distance;

    ArrayCast(IOpenClass to, IOpenCast openCast) {
        this.toComponentType = to;
        this.openCast = openCast;
        this.distance = CastFactory.ARRAY_CAST_DISTANCE + openCast.getDistance();
    }

    @Override
    public Object convert(Object from) {
        if (from == null) {
            return null;
        }
        Class<?> fromClass = from.getClass();
        Class<?> toClass = toComponentType.getInstanceClass();
        if (!fromClass.isArray()) {
            throw new ClassCastException(
                String.format("Cannot cast '%s' to '%s'.", fromClass.getTypeName(), toClass.getTypeName()));
        }
        int length = Array.getLength(from);
        Object convertedArray = Array.newInstance(toClass, length);
        for (int i = 0; i < length; i++) {
            Object fromValue = Array.get(from, i);
            Object toValue = openCast.convert(fromValue);
            Array.set(convertedArray, i, toValue);
        }
        return convertedArray;
    }

    @Override
    public int getDistance() {
        return distance;
    }

    @Override
    public boolean isImplicit() {
        return openCast.isImplicit();
    }

}
