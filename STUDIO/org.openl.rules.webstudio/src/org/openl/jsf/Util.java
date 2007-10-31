package org.openl.jsf;

import org.openl.rules.ui.WebStudio;

/**
 * Contains utility methods, which can be used from any class.
 *
 * @author Aliaksandr Antonik
 */
public abstract class Util {
    public static WebStudio getWebStudio() {
        return (WebStudio)(FacesUtils.getSessionMap().get("studio"));
    }
}
