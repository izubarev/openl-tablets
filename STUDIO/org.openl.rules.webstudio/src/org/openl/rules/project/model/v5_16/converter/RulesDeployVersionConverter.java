package org.openl.rules.project.model.v5_16.converter;

import java.util.Arrays;
import java.util.List;

import org.openl.rules.project.model.ObjectVersionConverter;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.project.model.v5_16.RulesDeploy_v5_16;
import org.openl.util.CollectionUtils;

public class RulesDeployVersionConverter implements ObjectVersionConverter<RulesDeploy, RulesDeploy_v5_16> {
    @Override
    public RulesDeploy fromOldVersion(RulesDeploy_v5_16 oldVersion) {
        RulesDeploy rulesDeploy = new RulesDeploy();

        rulesDeploy.setAnnotationTemplateClassName(oldVersion.getAnnotationTemplateClassName());
        rulesDeploy.setConfiguration(oldVersion.getConfiguration());
        rulesDeploy.setInterceptingTemplateClassName(oldVersion.getInterceptingTemplateClassName());

        if (oldVersion.getLazyModulesForCompilationPatterns() != null) {
            List<RulesDeploy.WildcardPattern> lazyModulesForCompilationPatterns = CollectionUtils.map(
                Arrays.asList(oldVersion.getLazyModulesForCompilationPatterns()),
                e -> e == null ? null : new RulesDeploy.WildcardPattern(e.getValue()));
            rulesDeploy.setLazyModulesForCompilationPatterns(lazyModulesForCompilationPatterns
                .toArray(new RulesDeploy.WildcardPattern[0]));
        }

        rulesDeploy.setProvideRuntimeContext(oldVersion.isProvideRuntimeContext());
        rulesDeploy.setProvideVariations(oldVersion.isProvideVariations());

        if (oldVersion.getPublishers() != null) {
            List<RulesDeploy.PublisherType> publishers = CollectionUtils.map(Arrays.asList(oldVersion.getPublishers()),
                e -> {
                    if (e == null) {
                        return null;
                    }

                    switch (e) {
                        case WEBSERVICE:
                            return RulesDeploy.PublisherType.WEBSERVICE;
                        case RESTFUL:
                            return RulesDeploy.PublisherType.RESTFUL;
                        case RMI:
                            return RulesDeploy.PublisherType.RMI;
                        default:
                            throw new IllegalArgumentException();
                    }
                });
            rulesDeploy.setPublishers(publishers.toArray(new RulesDeploy.PublisherType[0]));
        }

        rulesDeploy.setRmiServiceClass(oldVersion.getRmiServiceClass());
        rulesDeploy.setServiceClass(oldVersion.getServiceClass());
        rulesDeploy.setServiceName(oldVersion.getServiceName());
        rulesDeploy.setUrl(oldVersion.getUrl());

        return rulesDeploy;
    }

    @Override
    public RulesDeploy_v5_16 toOldVersion(RulesDeploy currentVersion) {
        RulesDeploy_v5_16 rulesDeploy = new RulesDeploy_v5_16();

        rulesDeploy.setAnnotationTemplateClassName(currentVersion.getAnnotationTemplateClassName());
        rulesDeploy.setConfiguration(currentVersion.getConfiguration());
        rulesDeploy.setInterceptingTemplateClassName(currentVersion.getInterceptingTemplateClassName());

        if (currentVersion.getLazyModulesForCompilationPatterns() != null) {
            List<RulesDeploy_v5_16.WildcardPattern> lazyModulesForCompilationPatterns = CollectionUtils.map(
                Arrays.asList(currentVersion.getLazyModulesForCompilationPatterns()),
                oldVersion -> oldVersion == null ? null : new RulesDeploy_v5_16.WildcardPattern(oldVersion.getValue()));
            rulesDeploy.setLazyModulesForCompilationPatterns(lazyModulesForCompilationPatterns
                .toArray(new RulesDeploy_v5_16.WildcardPattern[0]));
        }

        rulesDeploy.setProvideRuntimeContext(currentVersion.isProvideRuntimeContext());
        rulesDeploy.setProvideVariations(currentVersion.isProvideVariations());

        if (currentVersion.getPublishers() != null) {
            List<RulesDeploy_v5_16.PublisherType> publishers = CollectionUtils
                .map(Arrays.asList(currentVersion.getPublishers()), oldVersion -> {
                    if (oldVersion == null) {
                        return null;
                    }

                    switch (oldVersion) {
                        case WEBSERVICE:
                            return RulesDeploy_v5_16.PublisherType.WEBSERVICE;
                        case RESTFUL:
                            return RulesDeploy_v5_16.PublisherType.RESTFUL;
                        case RMI:
                            return RulesDeploy_v5_16.PublisherType.RMI;
                        case KAFKA:
                            throw new UnsupportedOperationException("KAFKA publisher is not supported in old version.");
                        default:
                            throw new IllegalArgumentException();
                    }
                });
            rulesDeploy.setPublishers(publishers.toArray(new RulesDeploy_v5_16.PublisherType[0]));
        }

        rulesDeploy.setRmiServiceClass(currentVersion.getRmiServiceClass());
        rulesDeploy.setServiceClass(currentVersion.getServiceClass());
        rulesDeploy.setServiceName(currentVersion.getServiceName());
        rulesDeploy.setUrl(currentVersion.getUrl());

        return rulesDeploy;
    }
}
