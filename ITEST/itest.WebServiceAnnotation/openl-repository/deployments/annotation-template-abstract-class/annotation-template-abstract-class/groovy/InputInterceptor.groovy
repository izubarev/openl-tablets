import java.lang.reflect.Method

import org.openl.rules.ruleservice.core.interceptors.ServiceMethodBeforeAdvice;

class InputInterceptor implements ServiceMethodBeforeAdvice {

    void before(Method interfaceMethod, Object proxy, Object... args) throws Throwable {
        if (args == null || args.length == 0 || "throwBeforeCall".equals(args[0])) {
            throw new IllegalArgumentException("Service method should have at least one argument");
        }
    }

}