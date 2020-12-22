/*
 * Created on Jun 11, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.openl.binding.impl.StaticClassLibrary;
import org.openl.binding.impl.cast.CastFactory;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;

/**
 * @author snshor
 *
 */
public class TypeCastFactory extends AConfigurationElement implements IConfigurationElement {

    TypeCastFactory(IOpenLConfiguration configuration) {
        this.configuration = configuration;
    }

    private final IOpenLConfiguration configuration;

    public class JavaCastComponent extends AConfigurationElement {
        private final String libraryClassName;
        private final String className;

        private volatile CastFactory factory = null;

        public JavaCastComponent(String libraryClassName, String className) {
            this.libraryClassName = Objects.requireNonNull(libraryClassName, "className cannot be null");
            this.className = Objects.requireNonNull(className, "className cannot be null");
        }

        CastFactory getCastFactory(IConfigurableResourceContext cxt) {
            if (factory == null) {
                synchronized (this) {
                    if (factory == null) {
                        ClassLoader classLoader = cxt.getClassLoader();
                        String uri = getUri();

                        Class<?> libClass = ClassFactory
                            .validateClassExistsAndPublic(libraryClassName, classLoader, uri);
                        Class<?> implClass = ClassFactory.validateClassExistsAndPublic(className, classLoader, uri);

                        // Strange reflection logic with implementation cast!
                        CastFactory castFactory = (CastFactory) ClassFactory.newInstance(implClass, uri);

                        castFactory.setMethodFactory(new StaticClassLibrary(JavaOpenClass.getOpenClass(libClass)));
                        castFactory.setGlobalCastFactory(configuration);

                        factory = castFactory;
                    }
                }
            }
            return factory;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.openl.newconf.IConfigurationElement#validate(org.openl.newconf. IConfigurationContext)
         */
        @Override
        public void validate(IConfigurableResourceContext cxt) {
            ClassFactory.validateClassExistsAndPublic(libraryClassName, cxt.getClassLoader(), getUri());
            Class<?> implClass = ClassFactory.validateClassExistsAndPublic(className, cxt.getClassLoader(), getUri());

            ClassFactory.validateSuper(implClass, CastFactory.class, getUri());

            ClassFactory.validateHaveNewInstance(implClass, getUri());
        }

    }

    private final List<JavaCastComponent> components = new ArrayList<>();

    public void addJavaCast(JavaCastComponent cmp) {
        components.add(cmp);
    }

    public Collection<JavaCastComponent> getJavaCastComponents() {
        return Collections.unmodifiableList(components);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.ICastFactory#getCast(org.openl.types.IOpenClass, org.openl.types.IOpenClass)
     */
    public IOpenCast getCast(IOpenClass from, IOpenClass to, IConfigurableResourceContext cxt) {
        for (JavaCastComponent component : components) {
            IOpenCast openCast = component.getCastFactory(cxt).getCast(from, to);
            if (openCast != null) {
                return openCast;
            }
        }
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.newconf.IConfigurationElement#validate(org.openl.newconf. IConfigurationContext)
     */
    @Override
    public void validate(IConfigurableResourceContext cxt) {
        for (JavaCastComponent component : components) {
            component.validate(cxt);
        }
    }

}
