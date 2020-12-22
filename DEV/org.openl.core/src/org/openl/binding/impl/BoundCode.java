/*
 * Created on Jun 2, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

import org.openl.binding.IBoundCode;
import org.openl.binding.IBoundNode;
import org.openl.message.OpenLMessage;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.exception.SyntaxNodeException;

/**
 * @author snshor
 *
 */
public class BoundCode implements IBoundCode {

    private final IParsedCode parsedCode;
    private final IBoundNode topNode;
    private final SyntaxNodeException[] errors;
    private final Collection<OpenLMessage> messages;

    public BoundCode(IParsedCode parsedCode,
            IBoundNode topNode,
            SyntaxNodeException[] errors,
            Collection<OpenLMessage> messages) {
        this.parsedCode = parsedCode;
        this.topNode = topNode;
        this.errors = errors;
        if (messages == null) {
            this.messages = Collections.emptyList();
        } else {
            this.messages = new LinkedHashSet<>(messages);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBoundCode#getError()
     */
    @Override
    public SyntaxNodeException[] getErrors() {
        return errors;
    }

    @Override
    public Collection<OpenLMessage> getMessages() {
        return Collections.unmodifiableCollection(messages);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBoundCode#getParsedCode()
     */
    @Override
    public IParsedCode getParsedCode() {
        return parsedCode;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBoundCode#getTopNode()
     */
    @Override
    public IBoundNode getTopNode() {
        return topNode;
    }

}
