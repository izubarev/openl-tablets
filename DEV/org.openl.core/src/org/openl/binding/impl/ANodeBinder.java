package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.INodeBinder;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
import org.openl.types.NullOpenClass;
import org.openl.util.MessageUtils;

/**
 * A base node binder with a bunch of utility methods.
 *
 * @author Yury Molchan
 */
public abstract class ANodeBinder implements INodeBinder {

    public static IBoundNode bindChildNode(ISyntaxNode node, IBindingContext bindingContext) {

        INodeBinder binder = findBinder(node, bindingContext);

        try {
            return binder.bind(node, bindingContext);
        } catch (Exception | LinkageError e) {
            return makeErrorNode(e, node, bindingContext);
        }
    }

    protected static boolean hasErrorBoundNode(IBoundNode[] boundNodes) {
        for (IBoundNode boundNode : boundNodes) {
            if (boundNode instanceof ErrorBoundNode) {
                return true;
            }
        }
        return false;
    }

    public static IBoundNode bindTargetNode(ISyntaxNode node, IBindingContext bindingContext, IBoundNode targetNode) {

        if (targetNode instanceof ErrorBoundNode) {
            return new ErrorBoundNode(node);
        }

        INodeBinder binder = findBinder(node, bindingContext);

        try {
            return binder.bindTarget(node, bindingContext, targetNode);
        } catch (Exception | LinkageError e) {
            return makeErrorNode(e, node, bindingContext);
        }
    }

    public static IBoundNode bindTypeNode(ISyntaxNode node, IBindingContext bindingContext, IOpenClass type) {

        INodeBinder binder = findBinder(node, bindingContext);

        try {
            return binder.bindType(node, bindingContext, type);
        } catch (Exception | LinkageError e) {
            return makeErrorNode(e, node, bindingContext);
        }
    }

    public static IBoundNode[] bindChildren(ISyntaxNode parentNode,
            IBindingContext bindingContext) {

        return bindChildren(parentNode, bindingContext, 0, parentNode.getNumberOfChildren());
    }

    public static IBoundNode[] bindChildren(ISyntaxNode parentNode,
            IBindingContext bindingContext,
            int from,
            int to) {

        int n = to - from;

        if (n == 0) {
            return IBoundNode.EMPTY;
        }

        IBoundNode[] children = new IBoundNode[n];

        int boundNodesCount = 0;

        for (int i = 0; i < n; i++) {

            ISyntaxNode childNode = parentNode.getChild(from + i);

            if (childNode == null) {
                boundNodesCount += 1;
                continue;
            }

            children[i] = bindChildNode(childNode, bindingContext);
            boundNodesCount += 1;
        }

        if (boundNodesCount != n) {
            return new IBoundNode[] { makeErrorNode("Cannot bind a node.", parentNode, bindingContext) };
        }

        return children;
    }

    public static IBoundNode[] bindTypeChildren(ISyntaxNode parentNode,
            IBindingContext bindingContext,
            IOpenClass type) {
        return bindTypeChildren(parentNode, bindingContext, type, 0, parentNode.getNumberOfChildren());
    }

    public static IBoundNode[] bindTypeChildren(ISyntaxNode parentNode,
            IBindingContext bindingContext,
            IOpenClass type,
            int from,
            int to) {

        int n = to - from;

        if (n == 0) {
            return IBoundNode.EMPTY;
        }

        IBoundNode[] children = new IBoundNode[n];

        for (int i = 0; i < n; i++) {

            ISyntaxNode childNode = parentNode.getChild(from + i);

            if (childNode == null) {
                continue;
            }

            children[i] = bindTypeNode(childNode, bindingContext, type);
        }

        return children;
    }

    public static IOpenClass[] getTypes(IBoundNode[] nodes) {

        IOpenClass[] types = new IOpenClass[nodes.length];

        for (int i = 0; i < types.length; i++) {
            types[i] = nodes[i].getType();
        }

        return types;
    }

    private static INodeBinder findBinder(ISyntaxNode node, IBindingContext bindingContext) {

        return bindingContext.findBinder(node);
    }

    private static IBoundNode convertType(IBoundNode node,
            IBindingContext bindingContext,
            IOpenClass type) throws TypeCastException {

        IOpenCast cast = getCast(node, type, bindingContext);

        if (cast == null) {
            return node;
        }

        return new CastNode(null, node, cast, type);
    }

    public static IOpenCast getCast(IBoundNode node,
            IOpenClass to,
            IBindingContext bindingContext) throws TypeCastException {
        return getCast(node, to, bindingContext, true);
    }

    public static IOpenCast getCast(IBoundNode node,
            IOpenClass to,
            IBindingContext bindingContext,
            boolean implicitOnly) throws TypeCastException {
        IOpenClass from = node.getType();

        if (from == null) {
            throw new TypeCastException(node.getSyntaxNode(), NullOpenClass.the, to);
        }

        if (from.equals(to)) {
            return null;
        }

        IOpenCast cast = bindingContext.getCast(from, to);

        if ((cast == null || (implicitOnly && !cast.isImplicit())) && !NullOpenClass.isAnyNull(from, to)) {
            throw new TypeCastException(node.getSyntaxNode(), from, to);
        }

        return cast;
    }

    public static String getIdentifier(ISyntaxNode node) {
        return ((IdentifierNode) node).getIdentifier();
    }

    protected static IOpenClass[] replace(int index, IOpenClass[] oldArray, IOpenClass newValue) {
        IOpenClass[] newArray = new IOpenClass[oldArray.length];
        System.arraycopy(oldArray, 0, newArray, 0, oldArray.length);
        newArray[index] = newValue;
        return newArray;
    }

    protected static IBoundNode makeErrorNode(String message, ISyntaxNode node, IBindingContext bindingContext) {
        BindHelper.processError(message, node, bindingContext);
        return new ErrorBoundNode(node);
    }

    private static IBoundNode makeErrorNode(Throwable e, ISyntaxNode node, IBindingContext bindingContext) {
        BindHelper.processError(e, node, bindingContext);
        return new ErrorBoundNode(node);
    }

    protected static IOpenClass getType(ISyntaxNode node,
            IBindingContext bindingContext) throws ClassNotFoundException {
        if ("type.name".equals(node.getType())) {
            String typeName = node.getText();
            IOpenClass varType = bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE, typeName);
            if (varType == null) {
                throw new ClassNotFoundException(MessageUtils.getTypeNotFoundMessage(typeName));
            }
            BindHelper.checkOnDeprecation(node, bindingContext, varType);
            return varType;
        }
        IOpenClass arrayType = getType(node.getChild(0), bindingContext);
        return arrayType != null ? arrayType.getAggregateInfo().getIndexedAggregateType(arrayType) : null;
    }

    protected static void assertCountOfChild(String message, ISyntaxNode node, int count) {
        if (node.getNumberOfChildren() != count) {
            throw new IllegalStateException(message);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.INodeBinder#bindTarget(org.openl.syntax.ISyntaxNode, org.openl.binding.IBindingContext,
     * org.openl.types.IOpenClass)
     */
    @Override
    public IBoundNode bindTarget(ISyntaxNode node,
            IBindingContext bindingContext,
            IBoundNode targetNode) throws Exception {
        return makeErrorNode("This node does not support target binding.", node, bindingContext);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.INodeBinder#bindType(org.openl.syntax.ISyntaxNode, org.openl.binding.IBindingContext,
     * org.openl.types.IOpenClass)
     */
    @Override
    public IBoundNode bindType(ISyntaxNode node, IBindingContext bindingContext, IOpenClass type) throws Exception {

        IBoundNode boundNode = bindChildNode(node, bindingContext);

        return convertType(boundNode, bindingContext, type);
    }
}
