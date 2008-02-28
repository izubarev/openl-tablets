package org.openl.rules.security;

import org.acegisecurity.AccessDecisionManager;
import org.acegisecurity.AccessDeniedException;
import org.acegisecurity.ConfigAttributeDefinition;
import org.acegisecurity.SecurityConfig;
import org.acegisecurity.context.SecurityContextHolder;

/**
 * @author Aliaksandr Antonik.
 */
public final class SecurityUtil {
    private static AccessDecisionManager accessDecisionManager;

    /**
     * Converts <code>privilege</code> to <code>ConfigAttributeDefinition</code> object and calls
     * {@link #check(org.acegisecurity.ConfigAttributeDefinition)}.
     *
     * @param privilege privilege to check.
     * @throws AccessDeniedException if current authentication context does not hold a required authority
     */
    public static void check(String privilege) throws AccessDeniedException {
        ConfigAttributeDefinition cad = new ConfigAttributeDefinition();
        cad.addConfigAttribute(new SecurityConfig(privilege));

        check(cad);
    }

    /**
     * Checks that current security context authentication is authorized to access a secured object with
     * given security attributes.
     *
     * @param config the configuration attributes of a secured object.
     * @throws AccessDeniedException if current authentication context does not hold a required authority
     */
    public static void check(ConfigAttributeDefinition config) throws AccessDeniedException {
        accessDecisionManager.decide(SecurityContextHolder.getContext().getAuthentication(), null, config);
    }

    /**
     * Sets static <code>accessDecisionManager</code> property for further use in <code>check</code> methods. <br/>
     * The only difference of this method from
     * {@link #useAccessDecisionManager(org.acegisecurity.AccessDecisionManager)} that this one is not <i>static</i>.  
     *
     * @param accessDecisionManager <code>AccessDecisionManager</code> instance.
     */
    public void setStaticAccessDecisionManager(AccessDecisionManager accessDecisionManager) {
        useAccessDecisionManager(accessDecisionManager);
    }

    /**
     * Sets static <code>accessDecisionManager</code> property for further use in <code>check</code> methods.
     *
     * @param adm <code>AccessDecisionManager</code> instance.
     */
    public static void useAccessDecisionManager(AccessDecisionManager adm) {
        accessDecisionManager = adm;
    }
}
