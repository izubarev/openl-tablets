package org.openl.classloader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.openl.util.ClassUtils;
import org.openl.util.IOUtils;

import groovy.lang.GroovyClassLoader;

/**
 * ClassLoader that have bundle classLoaders. When loading any class, at first tries to find it in bundle classLoaders
 * if can`t tries to find it in his parent.
 */
public class OpenLClassLoader extends GroovyClassLoader {

    private final Set<ClassLoader> bundleClassLoaders = new LinkedHashSet<>();

    private final Map<String, byte[]> generatedClasses = new ConcurrentHashMap<>();

    private final Set<GroovyClassLoader> groovyClassLoaders = new HashSet<>();

    public OpenLClassLoader(ClassLoader parent) {
        this(new URL[0], parent);
    }

    public OpenLClassLoader(URL[] urls, ClassLoader parent) {
        super(parent);
        if (urls != null && urls.length > 0) {
            for (URL url : urls) {
                addURL(url);
            }
            setResourceLoader(new GroovyResourceLoader(getResourceLoader()));
        } else {
            // Performance improvement, but groovy classes are loaded only from project classpath
            setResourceLoader(filename -> null);
        }
    }

    private static ClassLoader applyGroovySupport(ClassLoader classLoader, URL[] urls) {
        if (classLoader instanceof GroovyClassLoader) {
            return classLoader;
        } else {
            GroovyClassLoader groovyClassLoader = new GroovyClassLoader(classLoader);
            groovyClassLoader.setResourceLoader(new GroovyResourceLoader(groovyClassLoader.getResourceLoader()));
            if (urls != null) {
                for (URL url : urls) {
                    groovyClassLoader.addURL(url);
                }
            }
            return groovyClassLoader;
        }
    }

    public void addClassLoader(ClassLoader classLoader) {
        Objects.requireNonNull(classLoader, "Bundle class loader cannot be null");

        if (classLoader == this) {
            throw new IllegalArgumentException("Bundle class loader cannot register himself");
        }

        if (classLoader instanceof OpenLClassLoader && ((OpenLClassLoader) classLoader).containsClassLoader(this)) {
            throw new IllegalArgumentException("Bundle class loader cannot register class loader containing himself");
        }
        ClassLoader classLoader1 = applyGroovySupport(classLoader, new URL[0]);
        if (classLoader1 instanceof GroovyClassLoader) {
            groovyClassLoaders.add((GroovyClassLoader) classLoader1);
        }
        bundleClassLoaders.add(classLoader1);
    }

    public void addGeneratedClass(String name, byte[] byteCode) {
        if (generatedClasses.putIfAbsent(name, byteCode) != null) {
            throw new OpenLGeneratedClassAlreadyDefinedException(
                String.format("Byte code for class '%s' is already defined.", name));
        }

    }

    public boolean containsClassLoader(ClassLoader classLoader) {
        if (bundleClassLoaders.contains(classLoader)) {
            return true;
        }

        for (ClassLoader bundleClassLoader : bundleClassLoaders) {
            if (bundleClassLoader instanceof OpenLClassLoader && ((OpenLClassLoader) bundleClassLoader)
                .containsClassLoader(classLoader)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        Set<ClassLoader> c = Collections.newSetFromMap(new IdentityHashMap<>());
        c.add(this);
        return loadClass(name, c);
    }

    protected Class<?> loadClass(String name, Set<ClassLoader> c) throws ClassNotFoundException {
        Class<?> clazz = findClassInBundles(name, c);

        if (clazz != null) {
            return clazz;
        }
        try {
            return super.loadClass(name);
        } catch (ClassNotFoundException e) {
            byte[] byteCode = generatedClasses.get(name);
            if (byteCode != null) {
                try {
                    return ClassUtils.defineClass(name, byteCode, this);
                } catch (Exception e1) {
                    throw e;
                }
            }
            throw e;
        }
    }

    private Class<?> findClassInBundles(String name, Set<ClassLoader> c) {
        for (ClassLoader bundleClassLoader : bundleClassLoaders) {
            if (c.contains(bundleClassLoader)) {
                continue;
            }
            c.add(bundleClassLoader);
            try {
                // if current class loader contains appropriate class - it will
                // be returned as a result
                //
                Class<?> clazz;
                if (bundleClassLoader instanceof OpenLClassLoader && bundleClassLoader.getParent() == this) {
                    OpenLClassLoader sbc = (OpenLClassLoader) bundleClassLoader;
                    clazz = sbc.findLoadedClass(name);
                    if (clazz == null) {
                        clazz = sbc.findClassInBundles(name, c);
                    }
                } else {
                    if (bundleClassLoader instanceof OpenLClassLoader) {
                        clazz = ((OpenLClassLoader) bundleClassLoader).loadClass(name, c);
                    } else {
                        clazz = bundleClassLoader.loadClass(name);
                    }
                }
                if (clazz != null) {
                    return clazz;
                }
            } catch (ClassNotFoundException ignored) {
            }
        }

        return null;
    }

    private URL findResourceInBundleClassLoader(String name) {
        for (ClassLoader bundleClassLoader : bundleClassLoaders) {
            URL url;
            if (bundleClassLoader instanceof OpenLClassLoader && bundleClassLoader.getParent() == this) {
                OpenLClassLoader sbcl = (OpenLClassLoader) bundleClassLoader;
                url = sbcl.findResourceInBundleClassLoader(name);
            } else {
                url = bundleClassLoader.getResource(name);
            }
            if (url != null) {
                return url;
            }
        }
        return null;
    }

    private InputStream findResourceAsStreamInBundleClassLoader(String name) {
        for (ClassLoader bundleClassLoader : bundleClassLoaders) {
            InputStream inputStream;
            if (bundleClassLoader instanceof OpenLClassLoader && bundleClassLoader.getParent() == this) {
                OpenLClassLoader sbcl = (OpenLClassLoader) bundleClassLoader;
                inputStream = sbcl.findResourceAsStreamInBundleClassLoader(name);
            } else {
                inputStream = bundleClassLoader.getResourceAsStream(name);
            }
            if (inputStream != null) {
                return inputStream;
            }
        }
        return null;
    }

    private Enumeration<URL> findResourcesInBundleClassLoader(String name) throws IOException {
        for (ClassLoader bundleClassLoader : bundleClassLoaders) {
            Enumeration<URL> resources;
            if (bundleClassLoader instanceof OpenLClassLoader && bundleClassLoader.getParent() == this) {
                OpenLClassLoader sbcl = (OpenLClassLoader) bundleClassLoader;
                resources = sbcl.findResourcesInBundleClassLoader(name);
            } else {
                resources = bundleClassLoader.getResources(name);
            }
            if (resources != null) {
                return resources;
            }
        }
        return null;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        Enumeration<URL> resources = findResourcesInBundleClassLoader(name);
        if (resources != null) {
            return resources;
        }
        return super.getResources(name);
    }

    @Override
    public URL getResource(String name) {
        URL url = findResourceInBundleClassLoader(name);
        if (url != null) {
            return url;
        }
        return super.getResource(name);
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        InputStream inputStream = findResourceAsStreamInBundleClassLoader(name);
        if (inputStream != null) {
            return inputStream;
        }
        return super.getResourceAsStream(name);
    }

    private void addURLToList(List<URL> urls, URL url) {
        for (URL existingURL : urls) {
            if (existingURL.sameFile(url)) {
                return;
            }
        }
        urls.add(url);
    }

    @Override
    public URL[] getURLs() {
        List<URL> urls = new ArrayList<>();
        for (URL url : super.getURLs()) {
            addURLToList(urls, url);
        }
        for (ClassLoader bundleClassLoader : bundleClassLoaders) {
            if (bundleClassLoader instanceof URLClassLoader) {
                for (URL url : ((URLClassLoader) bundleClassLoader).getURLs()) {
                    addURLToList(urls, url);
                }
            }
        }
        return urls.toArray(new URL[0]);
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            groovyClassLoaders.forEach(IOUtils::closeQuietly);
        }
    }
}
