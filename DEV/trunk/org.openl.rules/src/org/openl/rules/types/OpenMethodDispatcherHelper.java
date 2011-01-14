package org.openl.rules.types;

import java.util.ArrayList;
import java.util.List;

import org.openl.types.IOpenMethod;

public class OpenMethodDispatcherHelper {
    
    private OpenMethodDispatcherHelper(){}
    
    /**
     * Some of the list values from income parameters may be wrapped by {@link OpenMethodDispatcher},
     * extract all methods and return new list of methods without {@link OpenMethodDispatcher}.
     * 
     * @param methods list of {@link IOpenMethod}
     * @return list of {@link IOpenMethod}, unwrapped from {@link OpenMethodDispatcher}.
     */
    public static List<IOpenMethod> extractMethods(List<IOpenMethod> methods) {
        List<IOpenMethod> result = new ArrayList<IOpenMethod>();
        
        for (IOpenMethod method : methods) {
            if (method instanceof OpenMethodDispatcher) {
                OpenMethodDispatcher dispatcher = (OpenMethodDispatcher) method;
                List<IOpenMethod> candidates = extractMethods(dispatcher.getCandidates());
                
                for (IOpenMethod candidate : candidates) {
//                    if (!(candidate instanceof OpenMethodDispatcher)) {
                        result.add(candidate);
//                    }
                }
            } else {
                result.add(method);
            }
        }        
        return result;
    }
    
}
