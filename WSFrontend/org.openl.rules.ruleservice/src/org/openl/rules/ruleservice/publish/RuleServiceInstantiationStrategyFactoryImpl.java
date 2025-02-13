package org.openl.rules.ruleservice.publish;

import java.util.Collection;
import java.util.Objects;

import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.SimpleMultiModuleInstantiationStrategy;
import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.core.RuleServiceDependencyManager;
import org.openl.rules.ruleservice.core.ServiceDescription;
import org.openl.rules.ruleservice.publish.lazy.LazyInstantiationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation for RuleServiceInstantiationStrategyFactory. Delegates decision to
 * RulesInstantiationStrategyFactory if one module in service. Returns LazyMultiModuleInstantiationStrategy strategy if
 * more than one module in service.
 *
 *
 * @author Marat Kamalov
 *
 */
public class RuleServiceInstantiationStrategyFactoryImpl implements RuleServiceInstantiationStrategyFactory {

    private final Logger log = LoggerFactory.getLogger(RuleServiceInstantiationStrategyFactoryImpl.class);

    private boolean lazyCompilation = false;

    public void setLazyCompilation(boolean lazyCompilation) {
        this.lazyCompilation = lazyCompilation;
    }

    public boolean isLazyCompilation() {
        return lazyCompilation;
    }

    /** {@inheritDoc} */
    @Override
    public RulesInstantiationStrategy getStrategy(ServiceDescription serviceDescription,
            IDependencyManager dependencyManager) {
        Objects.requireNonNull(serviceDescription, "serviceDescription cannot be null");
        Objects.requireNonNull(dependencyManager, "dependencyManager cannot be null");
        Collection<Module> modules = serviceDescription.getModules();
        int moduleSize = modules.size();
        if (moduleSize == 0) {
            throw new IllegalStateException("There are no modules to instantiate.");
        }

        if (isLazyCompilation()) {
            if (dependencyManager instanceof RuleServiceDependencyManager) {
                log.debug("Lazy loading strategy is used for service '{}'.", serviceDescription.getDeployPath());
                return new LazyInstantiationStrategy(serviceDescription.getDeployment(),
                    modules,
                    (RuleServiceDependencyManager) dependencyManager);
            } else {
                log.error("Failed to use lazy loading strategy with dependency manager '{}'.",
                    dependencyManager.getClass());
            }
        }
        log.debug("Multi module loading strategy has been used for service '{}'.", serviceDescription.getDeployPath());
        return new SimpleMultiModuleInstantiationStrategy(modules, dependencyManager, true);
    }
}
