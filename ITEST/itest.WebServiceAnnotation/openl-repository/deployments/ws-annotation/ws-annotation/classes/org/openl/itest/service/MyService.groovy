package org.openl.itest.service

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.openl.itest.service.internal.InputInterceptor;
import org.openl.itest.service.internal.MyType;
import org.openl.itest.service.internal.OutputInterceptor;
import org.openl.itest.service.internal.ResponseInterceptor;
import org.openl.itest.service.internal.VirtualMethodHandler;
import org.openl.rules.ruleservice.core.annotations.Name;
import org.openl.rules.ruleservice.core.annotations.ServiceExtraMethod;
import org.openl.rules.ruleservice.core.interceptors.RulesType;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallAfterInterceptor;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallBeforeInterceptor;

interface MyService {

    Integer parse(String num);

    @ServiceCallBeforeInterceptor([InputInterceptor.class])
    @ServiceCallAfterInterceptor([OutputInterceptor.class])
    MyType parse1(@RulesType("java.lang.String") Object p);
    // will be skipped because of such method does not exist in rules

    @ServiceCallAfterInterceptor([ResponseInterceptor.class])
    Response parse2(@RulesType("java.lang.String") Object p);

    @GET
    @Path("parse/{num}")
    Integer parse3(@PathParam("num") String num);

    @POST
    @Path("parseX")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_XML)
    @ServiceCallBeforeInterceptor([InputInterceptor.class])
    @ServiceCallAfterInterceptor([OutputInterceptor.class])
    MyType parse4(String num);

    @GET
    @Path("parseXQueryParam")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_XML)
    @ServiceCallBeforeInterceptor([InputInterceptor.class])
    @ServiceCallAfterInterceptor([OutputInterceptor.class])
    MyType parse4QueryParam(@QueryParam("numParam") String num);

    @GET
    @Path("parseXPathParam/{num}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_XML)
    @ServiceCallBeforeInterceptor([InputInterceptor.class])
    @ServiceCallAfterInterceptor([OutputInterceptor.class])
    MyType parse4PathParam(@PathParam("num") String num);

    @ServiceExtraMethod(VirtualMethodHandler.class)
    Double virtual(@Name("text") String num);

    @ServiceExtraMethod(VirtualMethodHandler.class)
    Double virtual2(@Name("first") String num, @Name("second") String arg);

    MyType ping(MyType type);

    @ServiceCallAfterInterceptor(value = [ToDoubleServiceMethodAfterAdvice.class,
            NoConvertorServiceMethodAfterAdvice.class])
    Double parse5(String num);

    @ServiceCallAfterInterceptor(value = [OpenLTypeServiceMethodAfterAdvice.class])
    Double parse6(String num);
}

