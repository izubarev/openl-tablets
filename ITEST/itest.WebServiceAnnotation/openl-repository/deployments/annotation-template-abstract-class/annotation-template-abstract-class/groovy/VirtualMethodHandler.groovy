import org.openl.rules.ruleservice.core.annotations.ServiceExtraMethodHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.lang.reflect.Method

class VirtualMethodHandler implements ServiceExtraMethodHandler<Double> {

    private static final Logger LOG = LoggerFactory.getLogger(VirtualMethodHandler.class);

    Double invoke(Method interfaceMethod, Object serviceBean, Object... args) throws Exception {
        LOG.debug("Invoking " + interfaceMethod.getName() + "( '" + args[0] + "' )");

        Method method = serviceBean.getClass().getMethod("parse", String.class);

        LOG.debug("Redirecting to parse( '" + args[0] + "' )");

        Object result = method.invoke(serviceBean, args[0]);

        LOG.debug("Result is " + result);

        Integer num = (Integer) result;
        Double value = num * 1.5;

        LOG.debug("Converted to " + value);

        return value;
    }

}
