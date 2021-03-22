package org.openl.rules.table.properties;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

public final class PropertyPreprocessor<T> {

    private final BiPredicate<String, T> predicate;

    private final BiFunction<String, T, T> function;

    private final PropertyPreprocessor<T> child;

    private PropertyPreprocessor(BiPredicate<String, T> predicate,
                                 BiFunction<String, T, T> function,
                                 PropertyPreprocessor<T> child) {
        this.predicate = predicate;
        this.function = function;
        this.child = child;
    }

    public static <T> PropertyPreprocessor<T> of(BiPredicate<String, T> predicate, BiFunction<String, T, T> function) {
        return new PropertyPreprocessor<>(predicate, function, null);
    }

    public static <T> PropertyPreprocessor<T> of(BiPredicate<String, T> predicate,
                                                 BiFunction<String, T, T> function,
                                                 PropertyPreprocessor<T> child) {
        return new PropertyPreprocessor<>(predicate, function, child);
    }

    public boolean check(String propertyName, T property){
        return Optional.ofNullable(predicate).map(p -> p.test(propertyName, property)).orElse(true);
    }

    public T apply(String propertyName, T property) {
        T result = Optional.ofNullable(function).map(f -> f.apply(propertyName, property)).orElse(property);
        return Optional.ofNullable(child)
            .filter(childPreprocessor -> childPreprocessor.check(propertyName, result))
            .map(childPreprocessor -> childPreprocessor.apply(propertyName, result))
            .orElse(result);

    }
}
