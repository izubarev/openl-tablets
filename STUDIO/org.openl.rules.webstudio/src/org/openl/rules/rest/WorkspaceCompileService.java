package org.openl.rules.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.openl.dependency.CompiledDependency;
import org.openl.message.OpenLMessage;
import org.openl.rules.project.instantiation.IDependencyLoader;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.dependencies.WebStudioWorkspaceRelatedDependencyManager;
import org.openl.rules.webstudio.web.MessageHandler;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StringUtils;
import org.springframework.stereotype.Service;

@Service
@Path("/compile/")
@Produces(MediaType.APPLICATION_JSON)
public class WorkspaceCompileService {

    private static final MessageHandler messageHandler = new MessageHandler();

    @GET
    @Path("progress/{messageId}/{messageIndex}")
    public Response getCompile(@PathParam("messageId") final Long messageId, @PathParam("messageIndex") final Integer messageIndex) {
        Map<String, Object> compileModuleInfo = new HashMap<>();
        WebStudio webStudio = WebStudioUtils.getWebStudio(WebStudioUtils.getSession());
        if (webStudio != null) {
            ProjectModel model = webStudio.getModel();
            WebStudioWorkspaceRelatedDependencyManager webStudioWorkspaceDependencyManager = model.getWebStudioWorkspaceDependencyManager();
            if (webStudioWorkspaceDependencyManager != null) {
                int compiledCounter = 0;
                List<MessageDescription> newMessages = new ArrayList<>();
                List<IDependencyLoader> loaders = webStudioWorkspaceDependencyManager.getDependencyLoaders().values()
                        .stream().flatMap(Collection::stream).filter(d -> !d.isProject()).collect(Collectors.toList());
                for (IDependencyLoader dependencyLoader : loaders) {
                    if (dependencyLoader == null) {
                        continue;
                    }
                    CompiledDependency compiledDependency = dependencyLoader.getRefToCompiledDependency();
                    if (compiledDependency != null) {
                        for (OpenLMessage message : compiledDependency.getCompiledOpenClass().getMessages()) {
                            MessageDescription messageDescription = getMessageDescription(message, model);
                            newMessages.add(messageDescription);
                        }
                        compiledCounter++;
                    }
                }
                compileModuleInfo.put("dataType", "new");
                if (messageIndex != -1 && messageId != -1) {
                    MessageDescription messageDescription = newMessages.get(messageIndex);
                    if (messageDescription.getId() == messageId) {
                        newMessages = newMessages.subList(messageIndex + 1, newMessages.size());
                        compileModuleInfo.put("dataType", "add");
                    }
                }

                compileModuleInfo.put("modulesCount", loaders.size());
                compileModuleInfo.put("modulesCompiled", compiledCounter);
                compileModuleInfo.put("messages", newMessages);
                compileModuleInfo.put("messageId", newMessages.get(newMessages.size()-1).getId());
                compileModuleInfo.put("messageIndex", newMessages.size() - 1);
            }
        }
        return Response.ok(compileModuleInfo).build();
    }

    private MessageDescription getMessageDescription(OpenLMessage message, ProjectModel model){
        String url = messageHandler.getSourceUrl(message, model);
        if (StringUtils.isBlank(url)) {
            url = messageHandler.getUrlForEmptySource(message);
        }
        return new MessageDescription(message.getId(), message.getSummary(), message.getSeverity(), url);
    }

}

