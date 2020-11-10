package org.openl.rules.ruleservice.publish.lazy;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openl.CompiledOpenClass;
import org.openl.OpenL;
import org.openl.dependency.IDependencyManager;
import org.openl.engine.OpenLCompileManager;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.context.IRulesRuntimeContextProvider;
import org.openl.rules.lang.xls.prebind.IPrebindHandler;
import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.core.DeploymentDescription;
import org.openl.rules.ruleservice.core.RuleServiceDependencyManager;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallAfterInterceptor;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallAroundInterceptor;
import org.openl.rules.runtime.AOpenLRulesEngineFactory;
import org.openl.rules.runtime.InterfaceClassGenerator;
import org.openl.rules.runtime.InterfaceClassGeneratorImpl;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.source.impl.VirtualSourceCodeModule;
import org.openl.runtime.AOpenLEngineFactory;
import org.openl.runtime.IEngineWrapper;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.code.Dependency;
import org.openl.syntax.code.DependencyType;
import org.openl.syntax.code.IDependency;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMember;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Prebinds openclass and creates LazyMethod and LazyField that will compile neccessary modules on demand.
 *
 * @author PUdalau, Marat Kamalov
 */
class LazyEngineFactory<T> extends AOpenLRulesEngineFactory {

    private static final String RULES_XLS_OPENL_NAME = OpenL.OPENL_JAVA_RULE_NAME;

    static {
        OpenL.setConfig(new LazyOpenLConfigurator());
    }

    private static final Logger LOG = LoggerFactory.getLogger(LazyEngineFactory.class);

    private CompiledOpenClass compiledOpenClass;
    private Class<T> interfaceClass;
    private final Collection<Module> modules;
    private final RuleServiceDependencyManager dependencyManager;
    private final Map<String, Object> externalParameters;
    private final DeploymentDescription deployment;

    public DeploymentDescription getDeployment() {
        return deployment;
    }

    private InterfaceClassGenerator interfaceClassGenerator = new InterfaceClassGeneratorImpl();

    void setInterfaceClassGenerator(InterfaceClassGenerator interfaceClassGenerator) {
        this.interfaceClassGenerator = Objects.requireNonNull(interfaceClassGenerator,
            "interfaceClassGenerator cannot be null");
        if (interfaceClass != null) {
            LOG.warn("Rules engine factory already has interface class. Interface class generator has been ignored.");
        }
    }

    LazyEngineFactory(DeploymentDescription deployment,
            Collection<Module> modules,
            RuleServiceDependencyManager dependencyManager,
            Class<T> interfaceClass,
            Map<String, Object> externalParameters) {
        super(RULES_XLS_OPENL_NAME);
        this.deployment = Objects.requireNonNull(deployment, "deployment cannot be null");
        this.modules = modules;
        this.dependencyManager = dependencyManager;
        this.interfaceClass = interfaceClass;
        this.externalParameters = externalParameters;
    }

    @Override
    public CompiledOpenClass getCompiledOpenClass() {
        if (compiledOpenClass == null) {
            compiledOpenClass = initializeOpenClass();
        }
        return compiledOpenClass;
    }

    @SuppressWarnings({ "unchecked" })
    Class<T> getInterfaceClass() {
        if (interfaceClass == null) {
            CompiledOpenClass compiledOpenClass = getCompiledOpenClass();
            IOpenClass openClass = compiledOpenClass.getOpenClass();
            final String className = openClass.getName();
            try {
                interfaceClass = (Class<T>) interfaceClassGenerator
                    .generateInterface(className, openClass, getCompiledOpenClass().getClassLoader());
            } catch (Exception e) {
                String errorMessage = String.format("Failed to create interface '%s'.", className);
                throw new OpenlNotCheckedException(errorMessage, e);
            }
        }
        return interfaceClass;
    }

    void setInterfaceClass(Class<T> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    @Override
    protected Class<?>[] prepareInstanceInterfaces() {
        return new Class[] { getInterfaceClass(), IEngineWrapper.class, IRulesRuntimeContextProvider.class };
    }

    @Override
    protected Object prepareInstance(IRuntimeEnv runtimeEnv) {
        try {
            compiledOpenClass = getCompiledOpenClass();
            IOpenClass openClass = compiledOpenClass.getOpenClass();
            Object openClassInstance = openClass.newInstance(runtimeEnv);
            Map<Method, IOpenMember> methodMap = prepareMethodMap(getInterfaceClass(), openClass);

            return prepareProxyInstance(openClassInstance,
                methodMap,
                runtimeEnv,
                getCompiledOpenClass().getClassLoader());
        } catch (Exception ex) {
            String errorMessage = "Failed to instantiate engine instance.";
            throw new OpenlNotCheckedException(errorMessage, ex);
        }
    }

    private CompiledOpenClass initializeOpenClass() {
        // put prebinder to openl
        IPrebindHandler prebindHandler = LazyBinderMethodHandler.getPrebindHandler();
        try {
            LazyBinderMethodHandler.setPrebindHandler(new LazyPrebindHandler(modules, dependencyManager, null, deployment));

            IOpenSourceCodeModule mainModule = createMainModule();
            RulesEngineFactory<?> engineFactory = new RulesEngineFactory<>(mainModule,
                AOpenLEngineFactory.DEFAULT_USER_HOME,
                getOpenlName());// FIXME
            engineFactory.setDependencyManager(dependencyManager);
            engineFactory.setExecutionMode(true);
            return engineFactory.getCompiledOpenClass();
        } finally {
            LazyBinderMethodHandler.setPrebindHandler(prebindHandler);
        }
    }

    private IOpenSourceCodeModule createMainModule() {
        List<IDependency> dependencies = new ArrayList<>();

        for (Module module : modules) {
            IDependency dependency = createDependency(module);
            dependencies.add(dependency);
        }

        Map<String, Object> params = new HashMap<>();
        if (externalParameters != null) {
            params.putAll(externalParameters);
        }
        if (params.get(OpenLCompileManager.EXTERNAL_DEPENDENCIES_KEY) != null) {
            @SuppressWarnings("unchecked")
            List<IDependency> externalDependencies = (List<IDependency>) params
                .get(OpenLCompileManager.EXTERNAL_DEPENDENCIES_KEY);
            dependencies.addAll(externalDependencies);
        }
        params.put(OpenLCompileManager.EXTERNAL_DEPENDENCIES_KEY, dependencies);

        IOpenSourceCodeModule source = new VirtualSourceCodeModule();
        source.setParams(params);

        return source;
    }

    private IDependency createDependency(Module module) {
        return new Dependency(DependencyType.MODULE, new IdentifierNode(null, null, module.getName(), null));
    }

    public IDependencyManager getDependencyManager() {
        return dependencyManager;
    }

    @Override
    protected void validateReturnType(IOpenMethod openMethod, Method interfaceMethod) {
        if (!(interfaceMethod.isAnnotationPresent(ServiceCallAfterInterceptor.class) || interfaceMethod
            .isAnnotationPresent(ServiceCallAroundInterceptor.class))) {
            super.validateReturnType(openMethod, interfaceMethod);
        }
    }

}
