package com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.interceptedretry;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@Priority(Integer.MAX_VALUE)
@Interceptor
@LogInterceptorBinding
public class LoggingInterceptor {

    @AroundInvoke
    public Object aroundInvokeMethod(InvocationContext ctx) throws Exception {
        InterceptedRetryOnEJBServlet.log();
        return ctx.proceed();
    }
}
