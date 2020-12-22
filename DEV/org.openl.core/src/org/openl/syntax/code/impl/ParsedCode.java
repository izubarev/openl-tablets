/*
 * Created on May 9, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.syntax.code.impl;

import java.util.*;

import org.openl.dependency.CompiledDependency;
import org.openl.message.OpenLMessage;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.code.IDependency;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.exception.SyntaxNodeException;

/**
 * @author snshor
 *
 */
public class ParsedCode implements IParsedCode {

    private final ISyntaxNode topNode;
    private final SyntaxNodeException[] syntaxErrors;
    private final Collection<OpenLMessage> messages;
    private final IOpenSourceCodeModule source;

    private Map<String, Object> params;
    private final IDependency[] dependencies;

    private Set<CompiledDependency> compiledDependencies = new HashSet<>();

    public ParsedCode(ISyntaxNode topnode,
            IOpenSourceCodeModule source,
            SyntaxNodeException[] syntaxErrors,
            Collection<OpenLMessage> messages) {
        this(topnode, source, syntaxErrors, messages, new IDependency[0]);
    }

    public ParsedCode(ISyntaxNode topNode,
            IOpenSourceCodeModule source,
            SyntaxNodeException[] syntaxErrors,
            Collection<OpenLMessage> messages,
            IDependency[] dependencies) {
        this.topNode = topNode;
        this.syntaxErrors = syntaxErrors;
        this.source = source;
        if (messages == null) {
            this.messages = Collections.emptyList();
        } else {
            this.messages = new LinkedHashSet<>(messages);
        }
        this.dependencies = dependencies;
    }

    @Override
    public SyntaxNodeException[] getErrors() {
        return syntaxErrors;
    }

    public Collection<OpenLMessage> getMessages() {
        return Collections.unmodifiableCollection(messages);
    }

    @Override
    public IOpenSourceCodeModule getSource() {
        return source;
    }

    @Override
    public ISyntaxNode getTopNode() {
        return topNode;
    }

    @Override
    public Map<String, Object> getExternalParams() {
        return params;
    }

    @Override
    public void setExternalParams(Map<String, Object> params) {
        this.params = params;
    }

    @Override
    public Set<CompiledDependency> getCompiledDependencies() {
        return Collections.unmodifiableSet(compiledDependencies);
    }

    @Override
    public void setCompiledDependencies(Set<CompiledDependency> compiledDependencies) {
        this.compiledDependencies = new LinkedHashSet<>(compiledDependencies);
    }

    @Override
    public IDependency[] getDependencies() {
        return dependencies;
    }
}
