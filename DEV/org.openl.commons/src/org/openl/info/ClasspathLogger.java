package org.openl.info;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ClasspathLogger extends OpenLLogger {

    private static final Logger LOG = LoggerFactory.getLogger(ClasspathLogger.class);

    @Override
    protected String getName() {
        return "cp";
    }

    @Override
    protected void discover() {
        log("Libs in the classpath:");
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = ClasspathLogger.class.getClassLoader();
        }
        while (classLoader != null) {
            log(getClassLoaderName(classLoader));
            if (classLoader instanceof URLClassLoader) {
                URL[] urls = ((URLClassLoader) classLoader).getURLs();
                for (URL url : urls) {
                    log("  {}", url);
                }
            }
            classLoader = classLoader.getParent();
        }
    }

    private static String getClassLoaderName(ClassLoader classLoader) {
        Class<?> clazz = classLoader.getClass();
        String name = clazz.getName();
        try {
            Class<?> cls = clazz.getMethod("toString").getDeclaringClass();
            if (cls != Object.class) {
                name = classLoader.toString() + "  Class: " + name + " #" + System.identityHashCode(classLoader);
            }
        } catch (NoSuchMethodException e) {
            // Ignore
        }

        try {
            Method getName = clazz.getMethod("getName");
            Object getNameStr = getName.invoke(classLoader);
            if (getNameStr != null) {
                name += "  Name: " + getNameStr.toString();
            }
        } catch (Exception e) {
            LOG.debug("Ignored error: ", e);
            // Ignore
        }
        return name;
    }
}
