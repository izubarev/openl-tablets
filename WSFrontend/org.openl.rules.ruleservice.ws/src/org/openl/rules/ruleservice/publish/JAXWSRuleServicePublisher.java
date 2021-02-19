package org.openl.rules.ruleservice.publish;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceDeployException;
import org.openl.rules.ruleservice.core.RuleServiceUndeployException;
import org.openl.rules.ruleservice.publish.jaxws.JAXWSOpenLServiceEnhancer;
import org.openl.rules.ruleservice.publish.jaxws.storelogdata.AegisObjectSerializer;
import org.openl.rules.ruleservice.storelogdata.CollectObjectSerializerInterceptor;
import org.openl.rules.ruleservice.storelogdata.CollectOpenLServiceInterceptor;
import org.openl.rules.ruleservice.storelogdata.CollectOperationResourceInfoInterceptor;
import org.openl.rules.ruleservice.storelogdata.CollectPublisherTypeInterceptor;
import org.openl.rules.ruleservice.storelogdata.ObjectSerializer;
import org.openl.rules.ruleservice.storelogdata.StoreLogDataFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * DeploymentAdmin to expose services via HTTP.
 *
 * @author Marat Kamalov
 */
public class JAXWSRuleServicePublisher implements RuleServicePublisher {

    private final Logger log = LoggerFactory.getLogger(JAXWSRuleServicePublisher.class);

    private final Map<OpenLService, ServiceServer> runningServices = new HashMap<>();
    private boolean storeLogDataEnabled = false;

    @Autowired
    private ObjectFactory<JAXWSOpenLServiceEnhancer> serviceEnhancerObjectFactory;

    @Autowired
    @Qualifier("jaxwsServiceServerPrototype")
    private ObjectFactory<ServerFactoryBean> serverFactoryBeanObjectFactory;

    @Autowired
    private ObjectFactory<StoreLogDataFeature> storeLoggingFeatureObjectFactory;

    public boolean isStoreLogDataEnabled() {
        return storeLogDataEnabled;
    }

    public void setStoreLogDataEnabled(boolean storeLogDataEnabled) {
        this.storeLogDataEnabled = storeLogDataEnabled;
    }

    public ObjectFactory<ServerFactoryBean> getServerFactoryBeanObjectFactory() {
        return serverFactoryBeanObjectFactory;
    }

    public void setServerFactoryBeanObjectFactory(ObjectFactory<ServerFactoryBean> serverFactoryBeanObjectFactory) {
        this.serverFactoryBeanObjectFactory = serverFactoryBeanObjectFactory;
    }

    public ObjectFactory<StoreLogDataFeature> getStoreLoggingFeatureObjectFactory() {
        return storeLoggingFeatureObjectFactory;
    }

    public void setStoreLoggingFeatureObjectFactory(
            ObjectFactory<StoreLogDataFeature> storeLoggingFeatureObjectFactory) {
        this.storeLoggingFeatureObjectFactory = storeLoggingFeatureObjectFactory;
    }

    private ObjectSerializer getObjectSerializer(ServerFactoryBean svrFactory) {
        return new AegisObjectSerializer((AegisDatabinding) svrFactory.getDataBinding());
    }

    public ObjectFactory<JAXWSOpenLServiceEnhancer> getServiceEnhancerObjectFactory() {
        return serviceEnhancerObjectFactory;
    }

    public void setServiceEnhancerObjectFactory(ObjectFactory<JAXWSOpenLServiceEnhancer> serviceEnhancerObjectFactory) {
        this.serviceEnhancerObjectFactory = serviceEnhancerObjectFactory;
    }

    @Override
    public void deploy(OpenLService service) throws RuleServiceDeployException {
        Objects.requireNonNull(service, "service cannot be null");
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(service.getClassLoader());
            ServerFactoryBean svrFactory = getServerFactoryBeanObjectFactory().getObject();
            ClassLoader origClassLoader = svrFactory.getBus().getExtension(ClassLoader.class);
            try {
                String serviceAddress = "/" + URLHelper.processURL(service.getUrl());
                svrFactory.setAddress(serviceAddress);
                JAXWSOpenLServiceEnhancer jaxwsOpenLServiceEnhancer = getServiceEnhancerObjectFactory().getObject();
                Class<?> serviceClass = jaxwsOpenLServiceEnhancer.decorateServiceInterface(service);
                svrFactory.setServiceClass(serviceClass);
                Class<?> proxyInterface = service.getServiceClass();
                Object serviceProxy = jaxwsOpenLServiceEnhancer
                    .createServiceProxy(proxyInterface, serviceClass, service);
                svrFactory.setServiceBean(serviceProxy);
                svrFactory.getBus().setExtension(service.getClassLoader(), ClassLoader.class);
                if (isStoreLogDataEnabled()) {
                    svrFactory.getFeatures().add(getStoreLoggingFeatureObjectFactory().getObject());

                    svrFactory.getInInterceptors()
                        .add(new CollectObjectSerializerInterceptor(getObjectSerializer(svrFactory)));
                    svrFactory.getInInterceptors().add(new CollectOpenLServiceInterceptor(service));
                    svrFactory.getInInterceptors()
                        .add(new CollectPublisherTypeInterceptor(RulesDeploy.PublisherType.WEBSERVICE));
                    svrFactory.getInInterceptors().add(new CollectOperationResourceInfoInterceptor());

                    svrFactory.getInFaultInterceptors()
                        .add(new CollectObjectSerializerInterceptor(getObjectSerializer(svrFactory)));
                    svrFactory.getInFaultInterceptors().add(new CollectOpenLServiceInterceptor(service));
                    svrFactory.getInFaultInterceptors()
                        .add(new CollectPublisherTypeInterceptor(RulesDeploy.PublisherType.WEBSERVICE));
                    svrFactory.getInFaultInterceptors().add(new CollectOperationResourceInfoInterceptor());
                }
                Server wsServer = svrFactory.create();

                ServiceServer serviceServer = new ServiceServer(wsServer, svrFactory.getDataBinding());
                runningServices.put(service, serviceServer);
                log.info("Service '{}' has been exposed with URL '{}'.", service.getDeployPath(), serviceAddress);
            } finally {
                svrFactory.getBus().setExtension(origClassLoader, ClassLoader.class);
            }
        } catch (Exception t) {
            throw new RuleServiceDeployException(String.format("Failed to deploy service '%s'", service.getDeployPath()), t);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    @Override
    public OpenLService getServiceByDeploy(String deployPath) {
        Objects.requireNonNull(deployPath, "deployPath cannot be null");
        for (OpenLService service : runningServices.keySet()) {
            if (service.getDeployPath().equals(deployPath)) {
                return service;
            }
        }
        return null;
    }

    @Override
    public void undeploy(OpenLService service) throws RuleServiceUndeployException {
        Objects.requireNonNull(service, "service cannot be null");
        ServiceServer server = runningServices.get(service);
        if (server == null) {
            throw new RuleServiceUndeployException(
                String.format("There is no running service '%s'.", service.getDeployPath()));
        }
        try {
            server.getServer().destroy();
            runningServices.remove(service);
            log.info("Service '{}' has been undeployed successfully.", service.getDeployPath());
        } catch (Exception t) {
            throw new RuleServiceUndeployException(String.format("Failed to undeploy service '%s'.", service.getDeployPath()),
                t);
        }

    }

    @Override
    public String getUrl(OpenLService service) {
        return URLHelper.processURL(service.getUrl());
    }

    private static class ServiceServer {
        private final Server server;
        private final DataBinding databinding;

        public ServiceServer(Server server, DataBinding dataBinding) {
            this.server = Objects.requireNonNull(server, "server cannot be null");
            this.databinding = dataBinding;
        }

        public DataBinding getDatabinding() {
            return databinding;
        }

        public Server getServer() {
            return server;
        }
    }
}
