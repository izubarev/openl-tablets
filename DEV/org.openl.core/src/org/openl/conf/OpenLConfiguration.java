package org.openl.conf;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.openl.binding.ICastFactory;
import org.openl.binding.INodeBinder;
import org.openl.binding.exception.AmbiguousMethodException;
import org.openl.binding.exception.AmbiguousTypeException;
import org.openl.binding.exception.AmbiguousVarException;
import org.openl.binding.impl.NotExistNodeBinder;
import org.openl.binding.impl.cast.CastFactory;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.binding.impl.method.MethodSearch;
import org.openl.conf.TypeCastFactory.JavaCastComponent;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.grammar.IGrammar;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.MethodKey;

/**
 * @author snshor
 *
 */
public class OpenLConfiguration implements IOpenLConfiguration {

    private String uri;

    private IOpenLConfiguration parent;

    private IConfigurableResourceContext configurationContext;

    private ClassFactory grammarFactory;

    private NodeBinderFactoryConfiguration binderFactory;

    private LibraryFactoryConfiguration methodFactory;

    private TypeCastFactory typeCastFactory;

    private TypeFactoryConfiguration typeFactory;

    private Map<String, IOpenFactoryConfiguration> openFactories = null;

    @Override
    public synchronized void addOpenFactory(IOpenFactoryConfiguration opfc) {
        if (openFactories == null) {
            openFactories = new HashMap<>();
        }

        if (opfc.getName() == null) {
            throw new OpenLConfigurationException("The factory must have a name", opfc.getUri(), null);
        }
        if (openFactories.containsKey(opfc.getName())) {
            throw new OpenLConfigurationException("Duplicated name: " + opfc.getName(), opfc.getUri(), null);
        }

        openFactories.put(opfc.getName(), opfc);
    }

    public NodeBinderFactoryConfiguration getBinderFactory() {
        return binderFactory;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.ICastFactory#getCast(java.lang.String, org.openl.types.IOpenClass,
     * org.openl.types.IOpenClass)
     */
    @Override
    public IOpenCast getCast(IOpenClass from, IOpenClass to) {
        IOpenCast cast = typeCastFactory == null ? null : typeCastFactory.getCast(from, to, configurationContext);
        if (cast != null) {
            return cast;
        }
        return parent == null ? null : parent.getCast(from, to);
    }

    protected Collection<JavaCastComponent> getAllJavaCastComponents() {
        Collection<JavaCastComponent> javaCastComponents = new ArrayList<>();
        if (typeCastFactory != null) {
            javaCastComponents.addAll(typeCastFactory.getJavaCastComponents());
        }
        if (parent instanceof OpenLConfiguration) {
            javaCastComponents.addAll(((OpenLConfiguration) parent).getAllJavaCastComponents());
        }
        return javaCastComponents;
    }

    private final Map<Key, IOpenClass> closestClassCache = new HashMap<>();
    private final ReadWriteLock closestClassCacheLock = new ReentrantReadWriteLock();

    @Override
    public IOpenClass findParentClass(IOpenClass openClass1, IOpenClass openClass2) {
        return CastFactory.findParentClass1(openClass1, openClass2);
    }

    @Override
    public IOpenClass findClosestClass(IOpenClass openClass1, IOpenClass openClass2) {
        Key key = new Key(openClass1, openClass2);
        IOpenClass closestClass;
        Lock readLock = closestClassCacheLock.readLock();
        try {
            readLock.lock();
            closestClass = closestClassCache.get(key);
        } finally {
            readLock.unlock();
        }
        if (closestClass == null) {
            Collection<JavaCastComponent> components = getAllJavaCastComponents();
            Collection<IOpenMethod> allMethods = new ArrayList<>();
            for (JavaCastComponent component : components) {
                CastFactory castFactory = component.getCastFactory(configurationContext);
                Iterable<IOpenMethod> methods = castFactory.getMethodFactory()
                    .methods(CastFactory.AUTO_CAST_METHOD_NAME);
                for (IOpenMethod method : methods) {
                    allMethods.add(method);
                }
            }
            closestClass = CastFactory.findClosestClass(openClass1, openClass2, new ICastFactory() {
                @Override
                public IOpenClass findClosestClass(IOpenClass openClass1, IOpenClass openClass2) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public IOpenCast getCast(IOpenClass from, IOpenClass to) {
                    return OpenLConfiguration.this.getCast(from, to);
                }

                @Override
                public IOpenClass findParentClass(IOpenClass openClass1, IOpenClass openClass2) {
                    return CastFactory.findParentClass1(openClass1, openClass2);
                }
            }, allMethods);
            Lock writeLock = closestClassCacheLock.readLock();
            try {
                writeLock.lock();
                closestClassCache.put(key, closestClass);
            } finally {
                writeLock.unlock();
            }
        }
        return closestClass;
    }

    @Override
    public IConfigurableResourceContext getConfigurationContext() {
        return configurationContext;
    }

    @Override
    public synchronized IGrammar getGrammar() {
        if (grammarFactory == null) {
            return parent.getGrammar();
        } else {
            return (IGrammar) grammarFactory.getResource(configurationContext);
        }
    }

    public ClassFactory getGrammarFactory() {
        return grammarFactory;
    }

    @Override
    public IMethodCaller getMethodCaller(String namespace,
            String name,
            IOpenClass[] params,
            ICastFactory casts) throws AmbiguousMethodException {

        IOpenMethod[] mcs = getMethods(namespace, name);

        return MethodSearch.findMethod(name, params, casts, Arrays.asList(mcs));
    }

    @Override
    public IOpenMethod[] getMethods(String namespace, String name) {
        IOpenMethod[] mcs = methodFactory == null ? IOpenMethod.EMPTY_ARRAY
                                                  : methodFactory.getMethods(namespace, name, configurationContext);
        IOpenMethod[] pmcs = parent == null ? IOpenMethod.EMPTY_ARRAY : parent.getMethods(namespace, name);

        // Shadowing
        Map<MethodKey, Collection<IOpenMethod>> methods = new HashMap<>();
        for (IOpenMethod method : pmcs) {
            MethodKey mk = new MethodKey(method);
            Collection<IOpenMethod> callers = methods.computeIfAbsent(mk, k -> new ArrayList<>());
            callers.add(method);
        }

        Set<MethodKey> usedKeys = new HashSet<>();
        for (IOpenMethod method : mcs) {
            MethodKey mk = new MethodKey(method);
            Collection<IOpenMethod> callers = methods.get(mk);
            if (callers == null) {
                usedKeys.add(mk);
                callers = new ArrayList<>();
                methods.put(mk, callers);
            }
            if (!usedKeys.contains(mk)) {
                usedKeys.add(mk);
                callers = new ArrayList<>();
                methods.put(mk, callers);
            }
            callers.add(method);
        }

        Collection<IOpenMethod> openMethods = new ArrayList<>();
        for (Collection<IOpenMethod> m : methods.values()) {
            openMethods.addAll(m);
        }
        return openMethods.toArray(IOpenMethod.EMPTY_ARRAY);
    }

    public LibraryFactoryConfiguration getMethodFactory() {
        return methodFactory;
    }

    @Override
    public INodeBinder getNodeBinder(ISyntaxNode node) {
        INodeBinder binder = binderFactory == null ? null : binderFactory.getNodeBinder(node, configurationContext);
        if (binder != null) {
            return binder;
        }
        return parent == null ? NotExistNodeBinder.the : parent.getNodeBinder(node);
    }

    final Map<String, Map<String, IOpenClass>> cache = new HashMap<>();

    @Override
    public IOpenClass getType(String namespace, String name) throws AmbiguousTypeException {
        Map<String, IOpenClass> namespaceCache = cache.computeIfAbsent(namespace, e -> new HashMap<>());
        if (namespaceCache.containsKey(name)) {
            return namespaceCache.get(name);
        }

        IOpenClass type = typeFactory == null ? null : typeFactory.getType(namespace, name, configurationContext);
        if (type != null) {
            namespaceCache.put(name, type);
            return type;
        }

        if (parent == null) {
            namespaceCache.put(name, null);
            return null;
        } else {
            type = parent.getType(namespace, name);
            namespaceCache.put(name, type);
            return type;
        }
    }

    public TypeCastFactory getTypeCastFactory() {
        return typeCastFactory;
    }

    public TypeCastFactory createTypeCastFactory() {
        this.typeCastFactory = new TypeCastFactory(this);
        return this.typeCastFactory;
    }

    public String getUri() {
        return uri;
    }

    @Override
    public IOpenField getVar(String namespace, String name, boolean strictMatch) throws AmbiguousVarException {
        IOpenField field = methodFactory == null ? null
                                                 : methodFactory
                                                     .getVar(namespace, name, configurationContext, strictMatch);
        if (field != null) {
            return field;
        }
        return parent == null ? null : parent.getVar(namespace, name, strictMatch);
    }

    public void setBinderFactory(NodeBinderFactoryConfiguration factory) {
        binderFactory = factory;
    }

    public void setConfigurationContext(IConfigurableResourceContext context) {
        configurationContext = context;
    }

    public void setGrammarFactory(ClassFactory factory) {
        grammarFactory = factory;
    }

    public void setMethodFactory(LibraryFactoryConfiguration factory) {
        methodFactory = factory;
    }

    public void setParent(IOpenLConfiguration configuration) {
        parent = configuration;
    }

    public void setTypeCastFactory(TypeCastFactory factory) {
        typeCastFactory = factory;
    }

    public void setTypeFactory(TypeFactoryConfiguration configuration) {
        typeFactory = configuration;
    }

    public void setUri(String string) {
        uri = string;
    }

    public synchronized void validate(IConfigurableResourceContext cxt) {
        if (grammarFactory != null) {
            grammarFactory.validate(cxt);
        } else if (parent == null) {
            throw new OpenLConfigurationException("Grammar class is not set", getUri(), null);
        }

        if (binderFactory != null) {
            binderFactory.validate(cxt);
        } else if (parent == null) {
            throw new OpenLConfigurationException("Bindings are not set", getUri(), null);
        }

        if (methodFactory != null) {
            methodFactory.validate(cxt);
        }

        if (typeCastFactory != null) {
            typeCastFactory.validate(cxt);
        }

        if (typeFactory != null) {
            typeFactory.validate(cxt);
        }

        if (openFactories != null) {
            for (IOpenFactoryConfiguration factory : openFactories.values()) {
                factory.validate(cxt);
            }
        }

    }

    private static class Key {
        final IOpenClass openClass1;
        final IOpenClass openClass2;

        public Key(IOpenClass openClass1, IOpenClass openClass2) {
            super();
            this.openClass1 = openClass1;
            this.openClass2 = openClass2;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (openClass1 == null ? 0 : openClass1.hashCode());
            result = prime * result + (openClass2 == null ? 0 : openClass2.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Key other = (Key) obj;
            if (openClass1 == null) {
                if (other.openClass1 != null) {
                    return false;
                }
            } else if (!openClass1.equals(other.openClass1)) {
                return false;
            }
            if (openClass2 == null) {
                return other.openClass2 == null;
            } else {
                return openClass2.equals(other.openClass2);
            }
        }
    }

}
