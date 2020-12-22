package org.openl.rules.webstudio.web.admin;

import org.openl.config.PropertiesHolder;
import org.openl.rules.project.abstraction.Comments;

public class FolderStructureSettings {
    private final String FLAT_FOLDER_STRUCTURE;
    private final RepositoryConfiguration configuration;

    public FolderStructureSettings(RepositoryConfiguration configuration) {
        String withPrefix = Comments.REPOSITORY_PREFIX + configuration.getConfigName();
        FLAT_FOLDER_STRUCTURE = withPrefix + ".folder-structure.flat";
        this.configuration = configuration;
    }

    public String getBasePath() {
        return configuration.getSettings().getBasePath();
    }

    public void setBasePath(String basePath) {
        String value = basePath.isEmpty() || basePath.endsWith("/") ? basePath : (basePath + "/");
        configuration.getSettings().setBasePath(value);
    }

    public boolean isFlatFolderStructure() {
        String property = getProperties().getProperty(FLAT_FOLDER_STRUCTURE);
        return property == null || Boolean.parseBoolean(property);
    }

    public void setFlatFolderStructure(boolean flatFolderStructure) {
        getProperties().setProperty(FLAT_FOLDER_STRUCTURE, flatFolderStructure);
    }

    public FolderStructureValidators getValidators() {
        return new FolderStructureValidators();
    }

    private PropertiesHolder getProperties() {
        return configuration.getProperties();
    }
}
