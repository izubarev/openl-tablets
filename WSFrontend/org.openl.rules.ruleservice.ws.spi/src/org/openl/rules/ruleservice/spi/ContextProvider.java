package org.openl.rules.ruleservice.spi;

import javax.servlet.ServletContext;

import org.springframework.context.ApplicationContext;

/**
 * SPI created for obtaining the Spring Application Context
 */
public interface ContextProvider {

    ApplicationContext getContext(ServletContext sc);

}
