package org.openl.rules.ruleservice.databinding;

import java.util.Objects;

import org.openl.rules.ruleservice.core.ServiceDescription;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.util.Assert;

public class ServiceConfigurationBooleanFactoryBean extends AbstractFactoryBean<Boolean> {

    private ServiceDescription serviceDescription;

    private Boolean defaultValue;
    private String propertyName;

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = Objects.requireNonNull(propertyName, "propertyName cannot be null");
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    @Override
    protected Boolean createInstance() throws Exception {
        Boolean ret = getDefaultValue();
        Object value = getValue(getPropertyName().trim());
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            if ("true".equalsIgnoreCase(((String) value).trim())) {
                return Boolean.TRUE;
            }
            if ("false".equalsIgnoreCase(((String) value).trim())) {
                return Boolean.FALSE;
            }
            throw new ServiceConfigurationException(
                String.format("Expected true/false value for '%s' in the deployment configuration for service '%s'.",
                    getPropertyName().trim(),
                    getServiceDescription().getDeployPath()));
        } else {
            if (value != null) {
                throw new ServiceConfigurationException(String.format(
                    "Expected true/false value for '%s' in the deployment configuration for service '%s'.",
                    getPropertyName().trim(),
                    getServiceDescription().getDeployPath()));
            }
        }
        return ret;
    }

    @Override
    public Class<?> getObjectType() {
        return Boolean.class;
    }

    @Override
    public void afterPropertiesSet() {
        Assert.notNull(this.propertyName, "propertyName cannot be null");
    }

    public ServiceDescription getServiceDescription() {
        return serviceDescription;
    }

    public void setServiceDescription(ServiceDescription serviceDescription) {
        this.serviceDescription = serviceDescription;
    }

    public Boolean getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Boolean defaultValue) {
        this.defaultValue = defaultValue;
    }

    protected Object getValue(String property) {
        return getServiceDescription().getConfiguration() == null ? null
                                                                  : getServiceDescription().getConfiguration()
                                                                      .get(property);
    }
}
