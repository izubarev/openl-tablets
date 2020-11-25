package org.openl.rules.webstudio.web.admin;

import static org.openl.rules.webstudio.web.admin.AdministrationSettings.PRODUCTION_REPOSITORY_CONFIGS;

import java.util.List;

import javax.annotation.PostConstruct;

import org.openl.config.PropertiesHolder;
import org.openl.rules.repository.RepositoryMode;
import org.openl.rules.webstudio.web.repository.RepositoryFactoryProxy;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Pavel Tarasevich
 */
public abstract class AbstractProductionRepoController {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractProductionRepoController.class);

    private RepositoryConfiguration repositoryConfiguration;

    private boolean checked;
    private String errorMessage = "";

    @Autowired
    private SystemSettingsBean systemSettingsBean;

    @Autowired
    private RepositoryFactoryProxy productionRepositoryFactoryProxy;

    private PropertiesHolder properties;

    private List<RepositoryConfiguration> productionRepositoryConfigurations;

    @PostConstruct
    public void afterPropertiesSet() {
        setProductionRepositoryConfigurations(systemSettingsBean.getProductionRepositoryConfigurations());
        setProperties(systemSettingsBean.getProperties(), PRODUCTION_REPOSITORY_CONFIGS);
        repositoryConfiguration = createDummyRepositoryConfiguration();
        systemSettingsBean = null;
    }

    protected void addProductionRepoToMainConfig(RepositoryConfiguration repoConf) {
        getProductionRepositoryConfigurations().add(repoConf);
    }

    protected List<RepositoryConfiguration> getProductionRepositoryConfigurations() {
        return productionRepositoryConfigurations;
    }

    public void setProductionRepositoryConfigurations(
            List<RepositoryConfiguration> productionRepositoryConfigurations) {
        this.productionRepositoryConfigurations = productionRepositoryConfigurations;
    }

    public void setProperties(PropertiesHolder properties, String repoListConfig) {
        this.properties = RepositoryEditor.createPropertiesWrapper(properties, repoListConfig);
    }

    public RepositoryConfiguration getRepositoryConfiguration() {
        return repositoryConfiguration;
    }

    protected RepositoryConfiguration createRepositoryConfiguration() {
        String newConfigName = RepositoryEditor.getNewConfigName(getProductionRepositoryConfigurations(), RepositoryMode.PRODUCTION.toString());
        RepositoryConfiguration repoConfig = new RepositoryConfiguration(newConfigName, properties, repositoryConfiguration);
        repoConfig.commit();
        return repoConfig;
    }

    public void clearForm() {
        repositoryConfiguration = createDummyRepositoryConfiguration();
        errorMessage = "";
    }

    private RepositoryConfiguration createDummyRepositoryConfiguration() {
        RepositoryConfiguration rc = new RepositoryConfiguration(RepositoryMode.PRODUCTION.toString(), properties);
        rc.setType(RepositoryType.DB.name().toLowerCase());
        return rc;
    }

    public boolean isInputParamInvalid(RepositoryConfiguration prodConfig) {
        try {
            RepositoryValidators.validate(prodConfig, getProductionRepositoryConfigurations());

            RepositorySettings settings = repositoryConfiguration.getSettings();
            if (settings instanceof CommonRepositorySettings) {
                CommonRepositorySettings s = (CommonRepositorySettings) settings;
                if (s.isSecure() && (StringUtils.isEmpty(s.getLogin()) || StringUtils.isEmpty(s.getPassword()))) {
                    throw new RepositoryValidationException(
                        "Invalid login or password. Please, check login and password");
                }
            }

            return false;
        } catch (RepositoryValidationException e) {
            LOG.debug("Error occurred:", e);
            this.errorMessage = e.getMessage();
            return true;
        }
    }

    public abstract void save();

    public void setSystemSettingsBean(SystemSettingsBean systemSettingsBean) {
        this.systemSettingsBean = systemSettingsBean;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public RepositoryFactoryProxy getProductionRepositoryFactoryProxy() {
        return productionRepositoryFactoryProxy;
    }

    public void setProductionRepositoryFactoryProxy(RepositoryFactoryProxy productionRepositoryFactoryProxy) {
        this.productionRepositoryFactoryProxy = productionRepositoryFactoryProxy;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
