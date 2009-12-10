package org.openl.engine;

import org.openl.IOpenBinder;
import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.IBoundCode;
import org.openl.binding.IBoundMethodNode;
import org.openl.binding.impl.ANodeBinder;
import org.openl.binding.impl.BoundError;
import org.openl.binding.impl.MethodCastNode;
import org.openl.syntax.IParsedCode;
import org.openl.syntax.ISyntaxError;
import org.openl.syntax.SyntaxErrorException;
import org.openl.types.IOpenCast;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.NullOpenClass;
import org.openl.types.java.JavaOpenClass;

/**
 * Class that defines OpenL engine manager implementation for binding
 * operations.
 * 
 */
public class OpenlBindManager extends BaseOpenlManager {

    /**
     * Construct new instance of manager.
     * 
     * @param openl {@link OpenL} instance
     */
    public OpenlBindManager(OpenL openl) {
        super(openl);
    }

    /**
     * Binds parsed code.
     * 
     * @param bindingContextDelegator binding context
     * @param parsedCode parsed code
     * @return bound code
     */
    public IBoundCode bindCode(IBindingContextDelegator bindingContextDelegator, IParsedCode parsedCode) {

        IOpenBinder binder = getOpenL().getBinder();

        if (bindingContextDelegator == null) {
            return binder.bind(parsedCode);
        }

        return binder.bind(parsedCode, bindingContextDelegator);
    }

    /**
     * Binds method which defines by header descriptor.
     *  
     * @param boundCode bound code that contains method bound code
     * @param header method header descriptor
     * @param bindingContext binding context
     * @return node of bound code that contains information about method
     */
    public IBoundMethodNode bindMethod(IBoundCode boundCode, IOpenMethodHeader header, IBindingContext bindingContext) {

        IBoundMethodNode boundMethodNode = null;

        try {
            boundMethodNode = bindMethodType((IBoundMethodNode) boundCode.getTopNode(), bindingContext, header
                    .getType());
        } catch (Exception ex) {

            BoundError boundError = new BoundError(boundCode.getTopNode().getSyntaxNode(), "", ex);
            throw new SyntaxErrorException("", new ISyntaxError[] { boundError });
        }

        return boundMethodNode;
    }

    private IBoundMethodNode bindMethodType(IBoundMethodNode boundMethodNode, IBindingContext bindingContext,
            IOpenClass type) throws Exception {

        if (type == JavaOpenClass.VOID || type == NullOpenClass.the) {
            return boundMethodNode;
        }

        IOpenCast cast = ANodeBinder.getCast(boundMethodNode, type, bindingContext);

        if (cast == null) {
            return boundMethodNode;
        }

        return new MethodCastNode(boundMethodNode, cast, type);
    }

}
