package org.openl.rules.rest;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.openl.dependency.CompiledDependency;
import org.openl.message.OpenLMessage;
import org.openl.rules.project.instantiation.IDependencyLoader;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.dependencies.WebStudioWorkspaceRelatedDependencyManager;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.springframework.stereotype.Service;

@Service
@Path("/compile/")
@Produces(MediaType.APPLICATION_JSON)
public class WorkspaceCompileService {

    @GET
    @Path("/progress")
    public Response getCompile() {
        Map<String, Object> compileModuleInfo = new HashMap<>();
        WebStudio webStudio = WebStudioUtils.getWebStudio(WebStudioUtils.getSession());
        if (webStudio != null) {
            int compiledCounter = 0;
            Set<OpenLMessage> messages = new HashSet<>();
            WebStudioWorkspaceRelatedDependencyManager webStudioWorkspaceDependencyManager = webStudio.getModel().getWebStudioWorkspaceDependencyManager();
            if (webStudioWorkspaceDependencyManager != null) {
                List<IDependencyLoader> loaders = webStudioWorkspaceDependencyManager.getDependencyLoaders().values()
                        .stream().flatMap(Collection::stream).filter(d -> !d.isProject()).collect(Collectors.toList());
                for (IDependencyLoader dependencyLoader : loaders) {
                    if (dependencyLoader == null) {
                        continue;
                    }
                    CompiledDependency compiledDependency = dependencyLoader.getRefToCompiledDependency();
                    if (compiledDependency != null) {
                        messages.addAll(compiledDependency.getCompiledOpenClass().getMessages());
                        compiledCounter++;
                    }
                }
                compileModuleInfo.put("modulesCount", loaders.size());
                compileModuleInfo.put("modulesCompiled", compiledCounter);
                compileModuleInfo.put("messages", messages);
            }
        }
        return Response.ok(compileModuleInfo).build();
    }
}

