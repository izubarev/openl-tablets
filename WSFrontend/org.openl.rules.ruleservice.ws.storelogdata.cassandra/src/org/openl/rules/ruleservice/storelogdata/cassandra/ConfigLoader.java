package org.openl.rules.ruleservice.storelogdata.cassandra;

import java.util.Arrays;
import java.util.Properties;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;

import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.internal.core.config.typesafe.DefaultDriverConfigLoader;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.impl.Parseable;

final class ConfigLoader {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigLoader.class);

    private ConfigLoader() {
    }

    private static boolean validateProperty(Environment env, String propName) {
        try {
            env.getProperty(propName);
            return true;
        } catch (Exception e) {
            if (LOG.isWarnEnabled()) {
                if (e instanceof IllegalArgumentException) {
                    LOG.warn("Failed to load spring property '{}'. {}", propName, e.getMessage());
                } else {
                    LOG.warn("Failed to load spring property '{}'.", propName, e);
                }
            }
            return false;
        }
    }

    @SuppressWarnings("rawtypes")
    private static Properties getApplicationContextProperties(ApplicationContext applicationContext) {
        Environment env = applicationContext.getEnvironment();
        Properties props = new Properties();
        MutablePropertySources propSources = ((AbstractEnvironment) env).getPropertySources();
        StreamSupport.stream(propSources.spliterator(), false)
            .filter(ps -> ps instanceof EnumerablePropertySource)
            .map(ps -> ((EnumerablePropertySource) ps).getPropertyNames())
            .flatMap(Arrays::stream)
            .filter(propName -> validateProperty(env, propName))
            .forEach(propName -> props.setProperty(propName, env.getProperty(propName)));
        return props;
    }

    private static ConfigObject loadApplicationContextProperties(ApplicationContext applicationContext) {
        return Parseable
            .newProperties(getApplicationContextProperties(applicationContext),
                ConfigParseOptions.defaults().setOriginDescription("spring context properties"))
            .parse();
    }

    static DriverConfigLoader fromApplicationContext(ApplicationContext applicationContext) {
        return new DefaultDriverConfigLoader(() -> {
            ConfigFactory.invalidateCaches();
            Config config = ConfigFactory.defaultOverrides()
                .withFallback(loadApplicationContextProperties(applicationContext))
                .withFallback(ConfigFactory.defaultReference())
                .resolve();
            return config.getConfig(DefaultDriverConfigLoader.DEFAULT_ROOT_PATH);
        });
    }

}
