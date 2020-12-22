package org.openl.binding.impl;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.message.OpenLMessagesUtils;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.ArrayLengthOpenField;
import org.openl.types.impl.CastingMethodCaller;
import org.openl.types.java.JavaOpenClass;
import org.openl.types.java.JavaOpenConstructor;
import org.openl.types.java.JavaOpenField;
import org.openl.types.java.JavaOpenMethod;

public final class BindHelper {

    private BindHelper() {
    }

    public static void processError(Throwable error, IBindingContext bindingContext) {
        SyntaxNodeException syntaxNodeException = SyntaxNodeExceptionUtils.createError(error, null);
        bindingContext.addError(syntaxNodeException);
    }

    public static void processError(String message, ISyntaxNode syntaxNode, IBindingContext bindingContext) {
        SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, syntaxNode);
        bindingContext.addError(error);
    }

    public static void processError(String message,
            Throwable ex,
            ISyntaxNode syntaxNode,
            IBindingContext bindingContext) {
        SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, ex, syntaxNode);
        bindingContext.addError(error);
    }

    public static void processError(String message, IOpenSourceCodeModule source, IBindingContext bindingContext) {
        SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, source);
        bindingContext.addError(error);
    }

    public static void processError(String message,
            Throwable ex,
            IOpenSourceCodeModule source,
            IBindingContext bindingContext) {
        SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, ex, null, source);
        bindingContext.addError(error);
    }

    public static void processError(Throwable ex, IOpenSourceCodeModule source, IBindingContext bindingContext) {
        SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(ex, null, source);
        bindingContext.addError(error);
    }

    public static void processError(Throwable e, ISyntaxNode node, IBindingContext bindingContext) {
        SyntaxNodeException syntaxNodeException = SyntaxNodeExceptionUtils.createError(e, node);
        bindingContext.addError(syntaxNodeException);
    }

    private static final Collection<String> EQUAL_OPERATORS = Collections
        .unmodifiableCollection(Arrays.asList("op.binary.eq",
            "op.binary.strict_eq",
            "op.binary.le",
            "op.binary.strict_le",
            "op.binary.ge",
            "op.binary.strict_ge"));
    private static final Collection<String> NOT_EQUAL_OPERATORS = Collections
        .unmodifiableCollection(Arrays.asList("op.binary.ne",
            "op.binary.strict_ne",
            "op.binary.lt",
            "op.binary.strict_lt",
            "op.binary.gt",
            "op.binary.strict_gt"));

    /**
     * Checks the condition expression.
     *
     * @param conditionNode Bound node that represents condition expression.
     * @param bindingContext Binding context.
     * @return <code>conditionNode</code> in case it is correct, and {@link ErrorBoundNode} in case condition is wrong.
     */
    public static IBoundNode checkConditionBoundNode(IBoundNode conditionNode, IBindingContext bindingContext) {
        if (conditionNode != null && !isBooleanType(conditionNode.getType())) {
            if (conditionNode.getType() != NullOpenClass.the) {
                BindHelper.processError("Expected boolean type for condition.",
                    conditionNode.getSyntaxNode(),
                    bindingContext);
            }
            return new ErrorBoundNode(conditionNode.getSyntaxNode());
        } else {
            if (conditionNode != null) {
                checkForSameLeftAndRightExpression(conditionNode, bindingContext);
            }

            return conditionNode;
        }
    }

    public static void checkOnDeprecation(ISyntaxNode node, IBindingContext context, IMethodCaller caller) {
        if (caller instanceof JavaOpenMethod) {
            Method javaMethod = ((JavaOpenMethod) caller).getJavaMethod();
            if (isDeprecated(javaMethod)) {
                String msg = String.format("DEPRECATED '%s' function will be removed in the next version.",
                    javaMethod.getName());
                processWarn(msg, node, context);
            }
        } else if (caller instanceof JavaOpenConstructor) {
            Constructor<?> constr = ((JavaOpenConstructor) caller).getJavaConstructor();
            if (isDeprecated(constr)) {
                String msg = String.format("DEPRECATED '%s' constructor will be removed in the next version.",
                    constr.getName());
                processWarn(msg, node, context);
            }
        } else if (caller instanceof CastingMethodCaller) {
            checkOnDeprecation(node, context, caller.getMethod());
        }
    }

    public static void checkOnDeprecation(ISyntaxNode node, IBindingContext context, IOpenClass aClass) {
        if (aClass instanceof JavaOpenClass) {
            Class<?> javaClass = aClass.getInstanceClass();
            if (javaClass.isAnnotationPresent(Deprecated.class)) {
                String msg = String.format("DEPRECATED '%s' class will be removed in the next version.",
                    javaClass.getTypeName());
                processWarn(msg, node, context);
            }
        } else if (aClass != null && aClass.isArray()) {
            checkOnDeprecation(node, context, aClass.getComponentClass());
        }
    }

    public static void checkOnDeprecation(ISyntaxNode node, IBindingContext context, IOpenField field) {
        if (field instanceof JavaOpenField) {
            Field javaField = ((JavaOpenField) field).getJavaField();
            if (isDeprecated(javaField)) {
                String msg = String.format("DEPRECATED '%s' field will be removed in the next version.",
                    javaField.getName());
                processWarn(msg, node, context);
            }
        } else if (field instanceof ArrayLengthOpenField) {
            processWarn(
                "DEPRECATED 'length' field for arrays will be removed in the next version. " +
                    "Use length() function instead.",
                node,
                context);
        }
    }

    private static <T extends Member & AnnotatedElement> boolean isDeprecated(T javaField) {
        return javaField.isAnnotationPresent(Deprecated.class) || javaField.getDeclaringClass()
            .isAnnotationPresent(Deprecated.class);
    }

    private static void checkForSameLeftAndRightExpression(IBoundNode conditionNode, IBindingContext bindingContext) {
        IBoundNode[] children = conditionNode.getChildren();
        if (children != null && children.length == 2) {
            IBoundNode left = children[0];
            IBoundNode right = children[1];
            if (isSame(left, right)) {
                String type = conditionNode.getSyntaxNode().getType();

                if (EQUAL_OPERATORS.contains(type)) {
                    BindHelper.processWarn("Condition is always true.", conditionNode.getSyntaxNode(), bindingContext);
                } else if (NOT_EQUAL_OPERATORS.contains(type)) {
                    BindHelper.processWarn("Condition is always false.", conditionNode.getSyntaxNode(), bindingContext);
                }
            }
        }
    }

    private static boolean isSame(IBoundNode left, IBoundNode right) {
        if (left instanceof FieldBoundNode && right instanceof FieldBoundNode) {
            if (((FieldBoundNode) left).getBoundField() == ((FieldBoundNode) right).getBoundField()) {
                if (left.getTargetNode() == right.getTargetNode()) {
                    return true;
                }
                if (isSame(left.getTargetNode(), right.getTargetNode())) {
                    return true;
                }
            }
        } else if (left instanceof LiteralBoundNode && right instanceof LiteralBoundNode) {
            Object leftValue = ((LiteralBoundNode) left).getValue();
            Object rightValue = ((LiteralBoundNode) right).getValue();
            if (leftValue == rightValue || (leftValue != null && leftValue.equals(rightValue))) {
                return true;
            }
        }

        return false;
    }

    public static void processWarn(String message, ISyntaxNode source, IBindingContext bindingContext) {
        if (bindingContext.isExecutionMode()) {
            bindingContext.addMessage(OpenLMessagesUtils.newWarnMessage(message));
        } else {
            bindingContext.addMessage(OpenLMessagesUtils.newWarnMessage(message, source));
        }
    }

    /**
     * Checks given type that it is boolean type.
     *
     * @param type {@link IOpenClass} instance
     * @return <code>true</code> if given type equals {@link JavaOpenClass#BOOLEAN} or
     *         JavaOpenClass.getOpenClass(Boolean.class); otherwise - <code>false</code>
     */
    private static boolean isBooleanType(IOpenClass type) {
        return type == null || JavaOpenClass.BOOLEAN == type || JavaOpenClass.getOpenClass(Boolean.class) == type;

    }

}
