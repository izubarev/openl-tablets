/*
 * Created on May 19, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.BindingDependencies;
import org.openl.binding.IBoundNode;
import org.openl.exception.OpenLRuntimeException;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.types.NullOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public abstract class ABoundNode implements IBoundNode {

    protected ISyntaxNode syntaxNode;

    protected final IBoundNode[] children;

    protected ABoundNode(ISyntaxNode syntaxNode, IBoundNode... children) {
        this.syntaxNode = syntaxNode;
        this.children = children != null && (children.length == 0 || (children.length == 1 && children[0] == null))
                ? IBoundNode.EMPTY
                : children;
    }

    @Override
    public void assign(Object value, IRuntimeEnv env) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final Object evaluate(IRuntimeEnv env) {
        try {
            Object res = evaluateRuntime(env);
            return res != null ? res : getType().nullObject();
        } catch (OpenLRuntimeException | ControlSignal ore) {
            throw ore;
        } catch (Exception t) {
            throw new OpenLRuntimeException(t, this);
        }
    }

    protected abstract Object evaluateRuntime(IRuntimeEnv env) throws Exception;

    @Override
    public IOpenClass getType() {
        return NullOpenClass.the;
    }

    @Override
    public IBoundNode[] getChildren() {
        return children;
    }

    @Override
    public ISyntaxNode getSyntaxNode() {
        return syntaxNode;
    }

    public void setSyntaxNode(ISyntaxNode syntaxNode) {
        this.syntaxNode = syntaxNode;
    }

    @Override
    public IBoundNode getTargetNode() {
        return null;
    }

    @Override
    public boolean isLvalue() {
        return false;
    }

    @Override
    public void updateAssignFieldDependency(BindingDependencies dependencies) {
    }

    @Override
    public void updateDependency(BindingDependencies dependencies) {
    }

    @Override
    public boolean isStaticTarget() {
        return false;
    }

}
