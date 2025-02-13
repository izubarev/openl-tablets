package org.openl.rules.ruleservice.core.interceptors;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.openl.types.IOpenMember;

/**
 * This annotation is designed to inject @{@link IOpenMember} related to invoked rule method to ruleservice
 * interceptors.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER, ElementType.METHOD })
public @interface InjectOpenMember {
}
