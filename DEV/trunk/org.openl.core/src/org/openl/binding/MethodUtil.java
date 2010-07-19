/*
 * Created on Jun 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding;

import java.lang.reflect.Method;

import org.apache.commons.lang.ClassUtils;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;
import org.openl.util.print.Formatter;

/**
 * @author snshor
 *
 */
public class MethodUtil {

    public static StringBuffer printMethod(IOpenMethod method, StringBuffer buf) {
        return printMethod(method.getName(), method.getSignature(), buf);
    }

    public static String printMethod(IOpenMethodHeader methodHeader, int mode, boolean printType) {
        StringBuffer buf = new StringBuffer(100);

        if (printType) {
            buf.append(methodHeader.getType().getDisplayName(mode)).append(' ');
        }

        startPrintingMethodName(methodHeader.getName(), buf);

        IMethodSignature signature = methodHeader.getSignature();

        for (int i = 0; i < signature.getNumberOfParameters(); i++) {
            printParameterInfo(signature.getParameterType(i).getDisplayName(mode), signature.getParameterName(i), i == 0, buf);
        }
        
        endPrintingMethodName(buf);

        return buf.toString();
    }

    public static String printMethod(String name, Class<?>[] params) {
        return printMethod(name, params, new StringBuffer()).toString();
    }

    public static StringBuffer printMethod(String name, Class<?>[] params, StringBuffer buf) {
        startPrintingMethodName(name, buf);
        
        for (int i = 0; i < params.length; i++) {
            printParameterInfo(params[i].getName(), null, i == 0, buf);
        }
        
        endPrintingMethodName(buf);
        return buf;
    }

    public static StringBuffer printMethod(String name, IMethodSignature signature, StringBuffer buf) {
        startPrintingMethodName(name, buf);
        
        for (int i = 0; i < signature.getNumberOfParameters(); i++) {
            printParameterInfo(signature.getParameterType(i).getName(), signature.getParameterName(i), i == 0, buf);
        }
        
        endPrintingMethodName(buf);
        return buf;
    }

    public static String printMethod(String name, IOpenClass[] params) {
        return printMethod(name, params, new StringBuffer()).toString();
    }

    public static StringBuffer printMethod(String name, IOpenClass[] params, StringBuffer buf) {
        startPrintingMethodName(name, buf);
        
        for (int i = 0; i < params.length; i++) {
            printParameterInfo(params[i].getName(), null, i == 0, buf);
        }
        endPrintingMethodName(buf);
        return buf;
    }

    public static StringBuffer printMethodWithParameterValues(IOpenMethod method, Object[] params, int mode, StringBuffer buf) {
        startPrintingMethodName(method.getName(), buf);

        IMethodSignature signature = method.getSignature();
        for (int i = 0; i < params.length; i++) {
            printParameterInfo(null, signature.getParameterName(i), params[i], i == 0, mode, buf);
        }

        endPrintingMethodName(buf);

        return buf;
    }
    
    public static String printMethodWithParameterValues(IOpenMethod method, Object[] params, int mode) {
        return printMethodWithParameterValues(method, params, mode, new StringBuffer()).toString();
    }
    
    private static void startPrintingMethodName(String name, StringBuffer buf) {
        buf.append(name).append('(');
    }
    
    private static void endPrintingMethodName(StringBuffer buf) {
        buf.append(')');
    }
    private static void printParameterInfo(String type, String name, boolean isFirst, StringBuffer buf){
        printParameterInfo(type, name, null, isFirst, 0, buf);
    }
    
    private static void printParameterInfo(String type, String name, Object value, boolean isFirst, int displayMode, StringBuffer buf){
        if (!isFirst){
            buf.append(", ");
        }
        
        if (type != null){
            buf.append(type);
        }
        
        if (type != null && name != null){
            buf.append(' ');
        }
        
        if (name != null){
            buf.append(name);
        }
        
        if (value != null){
            buf.append(" = ");
            Formatter.format(value, displayMode, buf);
        }
    }
    
    public static Method getMatchingAccessibleMethod(Class<?> methodOwner, String methodName, Class<?>[] paramTypes, boolean autoboxing) {
        Method resultMethod = null;
        Method[] methods = methodOwner.getMethods();
        for (Method method : methods) {
            Class<?>[] signatureParams = method.getParameterTypes();
            if (methodName.equals(method.getName()) && signatureParams.length == paramTypes.length) {
                if (ClassUtils.isAssignable(paramTypes, signatureParams, autoboxing)) {
                    if (resultMethod != null) {
                        if (isMoreConcreteMethod(resultMethod.getParameterTypes(), signatureParams)) {
                            resultMethod = method;
                        }
                    } else {
                        resultMethod = method;
                    }
                }
            }
        }
        return resultMethod;
    }

    /**
     * Determines which signature of two methods is more concrete. For example
     * method "append(String)" of StringBuilder is more concrete than method
     * "append(Object)"
     * 
     * @param firstMethodparams
     * @param secondMethodParams
     * @return <true> if the the signature of the second method is more concrete
     */
    private static boolean isMoreConcreteMethod(Class<?>[] firstMethodparams, Class<?>[] secondMethodParams) {
        if (ClassUtils.isAssignable(secondMethodParams, firstMethodparams)) {
            if (!ClassUtils.isAssignable(firstMethodparams, secondMethodParams)) {
                return true;
            }
        }
        return false;
    }
}
