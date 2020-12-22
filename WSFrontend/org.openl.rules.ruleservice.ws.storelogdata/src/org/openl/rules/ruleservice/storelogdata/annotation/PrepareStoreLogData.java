package org.openl.rules.ruleservice.storelogdata.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.openl.rules.ruleservice.core.interceptors.ServiceMethodAdvice;
import org.openl.rules.ruleservice.storelogdata.advice.StoreLogDataAdvice;

@Repeatable(PrepareStoreLogDatas.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PrepareStoreLogData {
    Class<? extends StoreLogDataAdvice> value();

    Class<? extends ServiceMethodAdvice> bindToServiceMethodAdvice() default Default.class;

    boolean before() default false;

    class Default implements ServiceMethodAdvice {
    }

}
