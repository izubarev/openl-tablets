package org.openl.rules.ruleservice.rest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.deployer.RulesDeployInputException;
import org.openl.rules.ruleservice.deployer.RulesDeployerService;
import org.openl.rules.ruleservice.management.ServiceManager;

/**
 * REST endpoint to deploy OpenL rules to the Web Service
 *
 * @author Vladyslav Pikus
 */
@Path("/deploy")
@Produces("application/json")
public class RulesDeployerRestController {

    private RulesDeployerService rulesDeployerService;
    private ServiceManager serviceManager;

    @Resource
    public void setServiceManager(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }

    @Resource
    public void setRulesDeployerService(RulesDeployerService rulesDeployerService) {
        this.rulesDeployerService = rulesDeployerService;
    }

    /**
     * Deploys target zip input stream
     */
    @POST
    @Consumes("application/zip")
    public Response deploy(@Context HttpServletRequest request) throws Exception {
        try {
            rulesDeployerService.deploy(request.getInputStream(), true);
            return Response.status(Status.CREATED).build();
        } catch (RulesDeployInputException e) {
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    /**
     * Redeploys target zip input stream
     */
    @POST
    @Path("/{deployPath:.+}")
    @Consumes("application/zip")
    public Response deploy(@PathParam("deployPath") final String deployPath,
            @Context HttpServletRequest request) throws Exception {
        try {
            rulesDeployerService.deploy(deployPath, request.getInputStream(), true);
            return Response.status(Status.CREATED).build();
        } catch (RulesDeployInputException e) {
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    /**
     * Read a file by the given path name.
     *
     * @return the file descriptor.
     * @throws IOException if not possible to read the file.
     */
    @GET
    @Path("/{deployPath:.+}")
    @Produces("application/zip")
    public Response read(@PathParam("deployPath") final String deployPath) throws Exception {
        OpenLService service = serviceManager.getServiceByDeploy(deployPath);
        if (service == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        InputStream read = rulesDeployerService.read(service.getDeployPath());
        final String encodedFileName = URLEncoder.encode(deployPath + ".zip", StandardCharsets.UTF_8.name());
        return Response.ok(read)
            .header("Content-Disposition",
                "attachment; filename='" + encodedFileName + "'; filename*=UTF-8''" + encodedFileName)
            .build();
    }

    /**
     * Delete a service.
     *
     * @param deployPath the name of the service to delete.
     */
    @DELETE
    @Path("/{deployPath:.+}")
    public Response delete(@PathParam("deployPath") final String deployPath) throws Exception {
        OpenLService service = serviceManager.getServiceByDeploy(deployPath);
        if (service == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        boolean deleted = rulesDeployerService.delete(service.getDeployPath());
        return Response.status(deleted ? Response.Status.OK : Status.NOT_FOUND).build();
    }
}
