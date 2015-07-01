package com.plexobject.handler.jaxws;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.plexobject.domain.Pair;
import com.plexobject.handler.Request;
import com.plexobject.handler.RequestHandler;
import com.plexobject.http.HttpResponse;
import com.plexobject.http.ServiceInvocationException;
import com.plexobject.service.ServiceRegistry;
import com.plexobject.util.ReflectUtils;

public class JaxwsDelegateHandler implements RequestHandler {
    private static final Logger logger = Logger
            .getLogger(JaxwsDelegateHandler.class);
    private static final String RESPONSE_SUFFIX = "Response";

    private final Object delegate;
    private final ServiceRegistry serviceRegistry;
    private final Map<String, JaxwsServiceMethod> methodsByName = new HashMap<>();
    private JaxwsServiceMethod defaultMethodInfo;

    public JaxwsDelegateHandler(Object delegate, ServiceRegistry registry) {
        this.delegate = delegate;
        this.serviceRegistry = registry;
    }

    public void addMethod(JaxwsServiceMethod info) {
        methodsByName.put(info.iMethod.getName(), info);
        defaultMethodInfo = info;
    }

    @Override
    public void handle(final Request request) {
        Pair<String, String> methodAndPayload = getMethodNameAndPayload(request);

        final JaxwsServiceMethod methodInfo = methodsByName
                .get(methodAndPayload.first);
        if (methodInfo == null) {
            throw new ServiceInvocationException("Unknown method "
                    + methodAndPayload.first + ", available "
                    + methodsByName.keySet() + ", request "
                    + request.getPayload(), HttpResponse.SC_NOT_FOUND);
        }
        // set method name
        request.setMethodName(methodInfo.iMethod.getName());
        //
        final String responseTag = methodAndPayload.first + RESPONSE_SUFFIX;
        try {
            // make sure you use iMethod to decode because implMethod might have
            // erased parameterized type
            // We can get input parameters either from JSON text, form/query
            // parameters or method simply takes Map so we just pass all request
            // properties
            final Object[] args = methodInfo.useNameParams() ? ReflectUtils
                    .decode(methodInfo.iMethod,
                            methodInfo.paramNamesAndDefaults,
                            request.getPropertiesAndHeaders())
                    : methodInfo.useMapProperties(methodAndPayload.second) ? new Object[] { request
                            .getPropertiesAndHeaders() } : ReflectUtils.decode(
                            methodAndPayload.second, methodInfo.iMethod,
                            request.getCodec());

            invokeWithAroundInterceptorIfNeeded(request, methodInfo,
                    responseTag, args);
        } catch (Exception e) {
            logger.error("PLEXSVC Failed to invoke " + methodInfo.iMethod
                    + ", for request " + request, e);
            request.getResponse().setPayload(e);
        }
    }

    private void invokeWithAroundInterceptorIfNeeded(final Request request,
            final JaxwsServiceMethod methodInfo, final String responseTag,
            final Object[] args) throws Exception {
        if (serviceRegistry.getAroundInterceptor() != null) {
            Callable<Object> callable = new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    invoke(request, methodInfo, responseTag, args);
                    return null;
                }
            };
            serviceRegistry.getAroundInterceptor().proceed(delegate,
                    methodInfo.iMethod.getName(), callable);
        } else {
            invoke(request, methodInfo, responseTag, args);
        }
    }

    private void invoke(Request request, final JaxwsServiceMethod methodInfo,
            String responseTag, Object[] args) throws Exception {
        if (serviceRegistry.getSecurityAuthorizer() != null) {
            serviceRegistry.getSecurityAuthorizer().authorize(request, null);
        }
        try {
            Map<String, Object> response = new HashMap<>();
            Object result = methodInfo.implMethod.invoke(delegate, args);
            if (logger.isDebugEnabled()) {
                logger.debug("****PLEXSVC MLN Invoking "
                        + methodInfo.iMethod.getName() + " with "
                        + Arrays.toString(args) + ", result " + result);
            }
            if (result != null) {
                response.put(responseTag, result);
            } else {
                response.put(responseTag, null);
            }
            request.getResponse().setPayload(response);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Exception) {
                throw (Exception) e.getCause();
            } else {
                throw e;
            }
        } catch (Exception e) {
            throw e;
        }
    }

    private static int incrWhileSkipWhitespacesAndQuotes(String text, int start) {
        while (Character.isWhitespace(text.charAt(start))
                || text.charAt(start) == '\'' || text.charAt(start) == '"') {
            start++;
        }
        return start;
    }

    private static int decrWhileSkipWhitespacesAndQuotes(String text, int end) {
        while (Character.isWhitespace(text.charAt(end))
                || text.charAt(end) == '\'' || text.charAt(end) == '"') {
            end--;
        }
        return end;
    }

    @VisibleForTesting
    Pair<String, String> getMethodNameAndPayload(Request request) {
        String text = request.getPayload();
        // hard coding to handle JSON messages
        // manual parsing because I don't want to run complete JSON parser
        int startObject = text != null ? text.indexOf('{') : -1;
        int endObject = text != null ? text.lastIndexOf('}') : -1;
        int colonPos = text.indexOf(':');

        if (text == null || startObject == -1 || endObject == -1
                || colonPos == -1) {
            String method = request.getStringProperty("methodName");
            if (method == null) {
                if (methodsByName.size() == 1) {
                    method = defaultMethodInfo.iMethod.getName();
                } else {
                    throw new IllegalArgumentException("Unsupported request "
                            + request.getProperties());
                }
            }
            //
            return Pair.of(method, text == null ? text : text.trim());
        }
        //
        int methodStart = incrWhileSkipWhitespacesAndQuotes(text,
                startObject + 1);
        int methodEnd = decrWhileSkipWhitespacesAndQuotes(text, colonPos - 1);
        String method = text.substring(methodStart, methodEnd + 1);
        //
        int payloadStart = incrWhileSkipWhitespacesAndQuotes(text, colonPos + 1);
        int payloadEnd = decrWhileSkipWhitespacesAndQuotes(text, endObject - 1);
        String payload = payloadStart <= payloadEnd ? text.substring(
                payloadStart, payloadEnd + 1).trim() : "";
        return Pair.of(method, payload);
    }
}