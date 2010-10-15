/*
 * Created on Oct 8, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.impl;

import org.openl.binding.BindingDependencies;
import org.openl.binding.IBoundMethodNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethodHeader;
import org.openl.vm.IRuntimeEnv;

public class CompositeMethod extends AMethod {
    
    private IBoundMethodNode methodBodyBoundNode;
    
    /**
     * Invoker for current method.
     */
    private Invoker invoker;

    public CompositeMethod(IOpenMethodHeader header, IBoundMethodNode methodBodyBoundNode) {
        super(header);
        this.methodBodyBoundNode = methodBodyBoundNode;
    }

    public IOpenClass getBodyType() {
        return methodBodyBoundNode.getType();
    }
   
    public IBoundMethodNode getMethodBodyBoundNode() {
        return methodBodyBoundNode;
    }

    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        if (invoker == null) {
            // create new instance of invoker.
            invoker = new CompositeMethodInvoker(methodBodyBoundNode, target, params, env);
        } else {
            // reset previously initialized parameters with new ones.
            invoker.resetParams(target, params, env);
        }
        return invoker.invoke();
    }

    public void setMethodBodyBoundNode(IBoundMethodNode node) {
        methodBodyBoundNode = node;
    }

    public void updateDependency(BindingDependencies dependencies) {
        dependencies.visit(getMethodBodyBoundNode());
    }
  
}