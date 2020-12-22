package org.openl.types.impl;

import java.util.Objects;

import org.openl.OpenL;
import org.openl.binding.impl.BindingContext;
import org.openl.engine.OpenLManager;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.types.IMethodCaller;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * @author Marat Kamalov
 *
 */
public class SourceCodeMethodCaller implements IMethodCaller {
    IOpenMethod method;
    final String sourceCode;
    final IMethodSignature signature;
    final IOpenClass resultType;

    public SourceCodeMethodCaller(IMethodSignature signature, IOpenClass resultType, String sourceCode) {
        this.signature = Objects.requireNonNull(signature, "signature cannot be null");
        this.sourceCode = Objects.requireNonNull(sourceCode, "sourceCode cannot be null");
        this.resultType = resultType;
    }

    @Override
    public IOpenMethod getMethod() {
        if (method == null) {
            IOpenSourceCodeModule src = new StringSourceCodeModule(sourceCode, null);
            OpenL op = OpenL.getInstance(OpenL.OPENL_J_NAME);
            OpenMethodHeader methodHeader = new OpenMethodHeader("run",
                resultType == null ? JavaOpenClass.VOID : resultType,
                signature,
                null);
            BindingContext cxt = new BindingContext(op.getBinder(), null, op);
            method = OpenLManager.makeMethod(op, src, methodHeader, cxt);
        }
        return method;
    }

    @Override
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        return getMethod().invoke(null, params, env);
    }
}
