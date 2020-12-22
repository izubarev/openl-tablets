package org.openl.binding.impl.cast;

final class StringToEnumCast implements IOpenCast {
    @SuppressWarnings("rawtypes")
    private final Class enumType;

    StringToEnumCast(Class<?> enumType) {
        this.enumType = enumType;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object convert(Object from) {
        if (from == null) {
            return null;
        }
        return Enum.valueOf(enumType, (String) from);
    }

    @Override
    public int getDistance() {
        return CastFactory.STRING_ENUM_TO_CAST_DISTANCE;
    }

    @Override
    public boolean isImplicit() {
        return false;
    }
}
