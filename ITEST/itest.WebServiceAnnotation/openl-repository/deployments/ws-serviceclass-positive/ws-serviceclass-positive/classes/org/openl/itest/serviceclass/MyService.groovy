package org.openl.itest.serviceclass

import org.openl.itest.serviceclass.internal.LongMethodAroundInterceptor
import org.openl.itest.serviceclass.internal.LongOutputInterceptor
import org.openl.itest.serviceclass.internal.Response
import org.openl.itest.serviceclass.internal.VoidMethodAroundInterceptor
import org.openl.itest.serviceclass.internal.VoidOutputInterceptor
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallAfterInterceptor
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallAroundInterceptor

interface MyService {
    Long doSomething(String str);

    long[] doArray();

    void voidMethod();

    @ServiceCallAfterInterceptor(VoidOutputInterceptor.class)
    Response voidMethodWithAfterReturnInterceptor();

    @ServiceCallAfterInterceptor(LongOutputInterceptor.class)
    Response longMethodWithAfterReturnInterceptor(String str);

    Number longMethodWithUpcast(String str);

    @ServiceCallAroundInterceptor(LongMethodAroundInterceptor.class)
    Response aroundLongMethod(String str);

    @ServiceCallAroundInterceptor(VoidMethodAroundInterceptor.class)
    Response aroundVoidMethod();
}