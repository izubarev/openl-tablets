package org.openl.rules.lang.xls.binding.wrapper;

import java.util.LinkedHashMap;
import java.util.Map;

import org.openl.binding.ICastFactory;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.context.DefaultRulesRuntimeContext;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.vm.SimpleRulesRuntimeEnv;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ContextPropertiesInjector {
    private static final Logger LOG = LoggerFactory.getLogger(ContextPropertiesInjector.class);
    private static final ContextPropertyInjection[] PROPERTY_INJECTIONS = new ContextPropertyInjection[0];
    private final ContextPropertyInjection[] contextPropertyInjections;

    public ContextPropertiesInjector(IOpenClass[] paramTypes, ICastFactory castFactory) {
        int i = 0;
        Map<String, ContextPropertyInjection> contextInjections = new LinkedHashMap<>();
        for (IOpenClass paramType : paramTypes) {
            try {
                int paramIndex = i;
                paramType.getFields()
                    .stream()
                    .filter(IOpenField::isContextProperty)
                    .forEach(field -> contextInjections.put(field.getContextProperty(),
                        createContextInjection(paramIndex, field, castFactory)));
            } catch (Exception | LinkageError e) {
                LOG.debug("Ignored error: ", e);
            }
            i++;
        }
        this.contextPropertyInjections = !contextInjections.isEmpty() ? contextInjections.values()
            .toArray(PROPERTY_INJECTIONS) : null;
    }

    private static ContextPropertyInjection createContextInjection(int paramIndex,
            IOpenField field,
            ICastFactory castFactory) {
        Class<?> contextType = DefaultRulesRuntimeContext.CONTEXT_PROPERTIES.get(field.getContextProperty());
        if (contextType == null) {
            throw new IllegalStateException(
                String.format("Context property '%s' is not found.", field.getContextProperty()));
        }
        IOpenClass contextTypeOpenClass = JavaOpenClass.getOpenClass(contextType);
        IOpenCast openCast = castFactory.getCast(field.getType(), contextTypeOpenClass);
        if ((openCast != null && (openCast.isImplicit())
                || (contextTypeOpenClass.getInstanceClass() != null
                    && contextTypeOpenClass.getInstanceClass().isEnum()))) {
            return new ContextPropertyInjection(paramIndex, field, openCast);
        } else {
            throw new ClassCastException(String.format(
                "Type mismatch for context property '%s' for field '%s' in class '%s'. " +
                    "Cannot convert from '%s' to '%s'.",
                field.getContextProperty(),
                field.getName(),
                field.getDeclaringClass().getName(),
                field.getType().getName(),
                contextTypeOpenClass.getName()));
        }
    }

    public boolean push(Object[] params, IRuntimeEnv env, SimpleRulesRuntimeEnv simpleRulesRuntimeEnv) {
        if (contextPropertyInjections != null) {
            IRulesRuntimeContext rulesRuntimeContext = null;
            for (ContextPropertyInjection contextPropertiesInjector : contextPropertyInjections) {
                rulesRuntimeContext = contextPropertiesInjector
                    .inject(params, env, simpleRulesRuntimeEnv, rulesRuntimeContext);
            }
            if (rulesRuntimeContext != null) {
                env.pushContext(rulesRuntimeContext);
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    public void pop(SimpleRulesRuntimeEnv env) {
        env.popContext();
    }

    private static class ContextPropertyInjection {
        private final int paramIndex;
        private final IOpenField field;
        private final IOpenCast openCast;

        public ContextPropertyInjection(int paramIndex, IOpenField field, IOpenCast openCast) {
            super();
            this.paramIndex = paramIndex;
            this.field = field;
            this.openCast = openCast;
        }

        public IRulesRuntimeContext inject(Object[] params,
                IRuntimeEnv env,
                SimpleRulesRuntimeEnv simpleRulesRuntimeEnv,
                IRulesRuntimeContext rulesRuntimeContext) {
            if (params[paramIndex] != null) {
                Object value = field.get(params[paramIndex], env);
                value = openCast.convert(value);
                if (rulesRuntimeContext == null) {
                    IRulesRuntimeContext currentRuntimeContext = (IRulesRuntimeContext) simpleRulesRuntimeEnv
                        .getContext();
                    try {
                        rulesRuntimeContext = currentRuntimeContext.clone();
                        rulesRuntimeContext.setValue(field.getContextProperty(), value);
                    } catch (CloneNotSupportedException e) {
                        throw new OpenlNotCheckedException(e);
                    }
                } else {
                    rulesRuntimeContext.setValue(field.getContextProperty(), value);
                }
            }
            return rulesRuntimeContext;
        }
    }
}
