package org.openl.rules.convertor;

import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.openl.meta.BigDecimalValue;
import org.openl.meta.BigIntegerValue;
import org.openl.meta.ByteValue;
import org.openl.meta.DoubleValue;
import org.openl.meta.FloatValue;
import org.openl.meta.IntValue;
import org.openl.meta.LongValue;
import org.openl.meta.ShortValue;
import org.openl.meta.StringValue;
import org.openl.rules.helpers.IntRange;
import org.openl.util.RuntimeExceptionWrapper;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gives convertors from one class to another.
 *
 * @author PUdalau
 */
public class ObjectToDataConvertorFactory {
    public static class ClassCastPair {
        private final Class<?> from;
        private final Class<?> to;

        public ClassCastPair(Class<?> from, Class<?> to) {
            this.from = from;
            this.to = to;
        }

        public Class<?> getFrom() {
            return from;
        }

        public Class<?> getTo() {
            return to;
        }

        @Override
        public int hashCode() {
            return to.hashCode() + from.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ClassCastPair)) {
                return false;
            }
            ClassCastPair pair = (ClassCastPair) obj;
            return from == pair.from && to == pair.to;
        }
    }

    public static class MatchedConstructorConvertor implements IObjectToDataConvertor {
        private final Constructor<?> ctr;

        public MatchedConstructorConvertor(Constructor<?> ctr) {
            this.ctr = ctr;
        }

        @Override
        public Object convert(Object data) {
            try {
                return ctr.newInstance(data);
            } catch (Exception e) {
                throw RuntimeExceptionWrapper.wrap(e);
            }
        }
    }

    /**
     * Contains static method as a private field for constructing new objects of appropriate type.
     *
     * @author DLiauchuk
     */
    public static class StaticMethodConvertor implements IObjectToDataConvertor {
        private final Method staticMethod;

        public StaticMethodConvertor(Method staticMethod) {
            if (!Modifier.isStatic(staticMethod.getModifiers())) {
                throw new IllegalArgumentException("Income method should be static");
            }
            this.staticMethod = staticMethod;
        }

        @Override
        public Object convert(Object data) {
            try {
                // first argument is null as field staticMethod represents only
                // static method.
                //
                return staticMethod.invoke(null, data);
            } catch (Exception e) {
                throw RuntimeExceptionWrapper.wrap(e);
            }
        }
    }

    public static class CopyConvertor implements IObjectToDataConvertor {
        @Override
        public Object convert(Object data) {
            return data;
        }

        public static final CopyConvertor the = new CopyConvertor();
    }

    /**
     * Convertor that looks for the instance method 'getValue' for conversion to the appropriate type.
     *
     * @author DLiauchuk
     */
    public static class GetValueConvertor implements IObjectToDataConvertor {
        @Override
        public Object convert(Object data) {
            if (data != null) {
                Method getValueMethod;
                try {
                    getValueMethod = data.getClass().getMethod("getValue");
                } catch (Exception e) {
                    throw RuntimeExceptionWrapper.wrap(e);
                }
                Object value;
                try {
                    value = getValueMethod.invoke(data);
                } catch (Exception e) {
                    throw RuntimeExceptionWrapper.wrap(e);
                }
                return value;
            }
            return null;
        }
    }

    private static final Map<ClassCastPair, IObjectToDataConvertor> converters = new ConcurrentHashMap<>();

    static {
        try {
            converters.put(new ClassCastPair(Integer.class, IntRange.class), e -> new IntRange((Integer) e));

            converters.put(new ClassCastPair(int.class, IntRange.class), e -> new IntRange((Integer) e));

            converters.put(new ClassCastPair(Double.class, DoubleValue.class), e -> new DoubleValue((Double) e));
            converters.put(new ClassCastPair(Double.class, double.class), CopyConvertor.the);
            converters.put(new ClassCastPair(double.class, Double.class), CopyConvertor.the);
            converters.put(new ClassCastPair(int.class, Integer.class), CopyConvertor.the);
            converters.put(new ClassCastPair(Integer.class, int.class), CopyConvertor.the);

            converters.put(new ClassCastPair(double.class, DoubleValue.class), e -> new DoubleValue((Double) e));
            converters.put(new ClassCastPair(Date.class, Calendar.class), e -> {
                Calendar cal = Calendar.getInstance(LocaleDependConvertor.getLocale());
                cal.setTime((Date) e);
                return cal;
            });

            converters.put(new ClassCastPair(Date.class, LocalDate.class), e -> Instant.ofEpochMilli(((Date) e).getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDate());

            converters.put(new ClassCastPair(Date.class, ZonedDateTime.class), e -> ZonedDateTime.ofInstant(Instant.ofEpochMilli(((Date) e).getTime()),
                ZoneId.systemDefault()));

            converters.put(new ClassCastPair(Date.class, Instant.class), e -> ((Date) e).toInstant());

            converters.put(new ClassCastPair(Date.class, LocalTime.class), e -> Instant.ofEpochMilli(((Date) e).getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalTime());

            converters.put(new ClassCastPair(Date.class, LocalDateTime.class), e -> Instant.ofEpochMilli(((Date) e).getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime());

            /*
             * convertors from Openl types with meta info to common java types
             */
            converters.put(new ClassCastPair(ByteValue.class, Byte.class), new GetValueConvertor());
            converters.put(new ClassCastPair(ShortValue.class, Short.class), new GetValueConvertor());
            converters.put(new ClassCastPair(IntValue.class, Integer.class), new GetValueConvertor());
            converters.put(new ClassCastPair(LongValue.class, Long.class), new GetValueConvertor());
            converters.put(new ClassCastPair(FloatValue.class, Float.class), new GetValueConvertor());
            converters.put(new ClassCastPair(DoubleValue.class, Double.class), new GetValueConvertor());
            converters.put(new ClassCastPair(BigIntegerValue.class, BigInteger.class), new GetValueConvertor());
            converters.put(new ClassCastPair(BigDecimalValue.class, BigDecimal.class), new GetValueConvertor());
            converters.put(new ClassCastPair(StringValue.class, String.class), new GetValueConvertor());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static final IObjectToDataConvertor NO_Convertor = e -> new UnsupportedOperationException();

    /**
     * @return NO_Convertor if value is not convertable to expected type.
     */
    public static IObjectToDataConvertor getConvertor(Class<?> toClass, Class<?> fromClass) {
        if (toClass == fromClass) {
            return CopyConvertor.the;
        }
        ClassCastPair pair = new ClassCastPair(fromClass, toClass);

        IObjectToDataConvertor convertor = converters.get(pair);
        if (convertor != null) {
            return convertor;
        }

        Method method = getValueOfMethod(toClass, fromClass);
        if (method != null) {
            convertor = new StaticMethodConvertor(method);
        } else {
            // try to find appropriate constructor.
            //
            Constructor<?> ctr = ConstructorUtils.getMatchingAccessibleConstructor(toClass, fromClass);

            if (ctr != null) {
                convertor = new MatchedConstructorConvertor(ctr);
            } else {
                convertor = NO_Convertor;
            }
        }
        converters.put(pair, convertor);
        return convertor;
    }

    private static Method getValueOfMethod(Class<?> toClass, Class<?> fromClass) {
        if (fromClass == null) {
            return null;
        }
        Method method = MethodUtils.getAccessibleMethod(toClass, "valueOf", fromClass);
        return method == null ? MethodUtils.getAccessibleMethod(toClass, "valueOf", Object.class) : method;
    }

    public static IObjectToDataConvertor registerConvertor(Class<?> toClass,
            Class<?> fromClass,
            IObjectToDataConvertor convertor) {
        ClassCastPair pair = new ClassCastPair(fromClass, toClass);
        return converters.put(pair, convertor);
    }

}
