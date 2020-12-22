package org.openl.rules.webstudio.filter;

import java.util.Collection;
import java.util.HashSet;

import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.util.StringUtils;

/**
 * Filter for <code>ProjectResource</code>s based on their file extension.
 */
public class RepositoryFileExtensionFilter implements IFilter<AProjectArtefact> {
    /**
     * Arrays of accepted exceptions.
     */
    private final String[] extensions;

    /**
     * Constructs new instance of the class. Parses a list of extentions from <code>extensionList</code>.
     *
     * @param extensionList <i>;</i> separated list of accepted file extensions.
     */
    public RepositoryFileExtensionFilter(String extensionList) {
        // set of parsed extensions
        Collection<String> extSet = new HashSet<>();
        for (String ext : extensionList.split(";")) {
            if (StringUtils.isNotBlank(ext)) {
                extSet.add(ext.trim());
            }
        }

        extensions = extSet.toArray(new String[0]);
        // for each extension prepend period if it is not already there
        for (int i = 0; i < extensions.length; i++) {
            if (!extensions[i].startsWith(".")) {
                extensions[i] = "." + extensions[i];
            }
        }
    }

    @Override
    public boolean select(AProjectArtefact artifact) {
        for (String ext : extensions) {
            if (artifact.getName().endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean supports(Class<?> aClass) {
        return aClass != null && AProjectResource.class.isAssignableFrom(aClass);
    }
}
