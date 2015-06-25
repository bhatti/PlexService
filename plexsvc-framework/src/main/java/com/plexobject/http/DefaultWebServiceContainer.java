package com.plexobject.http;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.plexobject.domain.Configuration;
import com.plexobject.handler.RequestHandler;
import com.plexobject.route.RouteResolver;
import com.plexobject.service.Lifecycle;
import com.plexobject.service.LifecycleAware;
import com.plexobject.service.RequestMethod;
import com.plexobject.service.ServiceConfigDesc;
import com.plexobject.service.ServiceRegistry;
import com.plexobject.service.impl.AbstractServiceContainer;

/**
 * This is default implementation of HTTP based service container
 * 
 * @author shahzad bhatti
 *
 */
public class DefaultWebServiceContainer extends AbstractServiceContainer {
    private final Map<RequestMethod, RouteResolver<RequestHandler>> requestHandlerEndpointsByMethod;
    private final Lifecycle server;

    public DefaultWebServiceContainer(
            final Configuration config,
            final ServiceRegistry serviceRegistry,
            final Map<RequestMethod, RouteResolver<RequestHandler>> requestHandlerEndpointsByMethod,
            final Lifecycle server) {
        super(config, serviceRegistry);
        this.requestHandlerEndpointsByMethod = requestHandlerEndpointsByMethod;
        this.server = server;
    }

    @Override
    public Collection<RequestHandler> getHandlers() {
        Collection<RequestHandler> handlers = new ArrayList<>();
        for (RouteResolver<RequestHandler> rhp : requestHandlerEndpointsByMethod
                .values()) {
            handlers.addAll(rhp.getObjects());
        }
        return handlers;
    }

    @Override
    protected void doStart() throws Exception {
        server.start();
    }

    @Override
    protected void doStop() throws Exception {
        server.stop();
    }

    @Override
    public synchronized void add(final RequestHandler handler) {
        ServiceConfigDesc config = serviceRegistry.getServiceConfig(handler);

        String endpoint = getEndpoint(handler, config);
        if (exists(handler)) {
            throw new IllegalStateException(
                    "RequestHandler is already registered for " + endpoint);
        }
        RouteResolver<RequestHandler> requestHandlerEndpoints = getRouteResolver(config
                .method());
        //
        requestHandlerEndpoints.put(endpoint, handler);
        if (handler instanceof LifecycleAware) {
            ((LifecycleAware) handler).onCreated();
        }
        logger.info("PLEXSVC Adding Web service handler "
                + handler.getClass().getSimpleName() + " for "
                + config.protocol() + ":" + config.method() + " : "
                + config.endpoint() + ", codec " + config.codec());
    }

    private RouteResolver<RequestHandler> getRouteResolver(RequestMethod method) {
        RouteResolver<RequestHandler> requestHandlerEndpoints = requestHandlerEndpointsByMethod
                .get(method);
        if (requestHandlerEndpoints == null) {
            requestHandlerEndpoints = new RouteResolver<RequestHandler>();
            requestHandlerEndpointsByMethod
                    .put(method, requestHandlerEndpoints);
        }
        return requestHandlerEndpoints;
    }

    @Override
    public synchronized boolean remove(RequestHandler handler) {
        ServiceConfigDesc config = serviceRegistry.getServiceConfig(handler);
        RouteResolver<RequestHandler> requestHandlerEndpoints = requestHandlerEndpointsByMethod
                .get(config.method());

        boolean removed = false;
        if (requestHandlerEndpoints != null) {
            String endpoint = getEndpoint(handler, config);
            removed = requestHandlerEndpoints.remove(endpoint);
            if (removed && handler instanceof LifecycleAware) {
                ((LifecycleAware) handler).onDestroyed();
                logger.info("PLEXSVC Removing service handler " + handler);
            }
        }
        return removed;
    }

    @Override
    public boolean exists(RequestHandler handler) {
        ServiceConfigDesc config = serviceRegistry.getServiceConfig(handler);
        RouteResolver<RequestHandler> requestHandlerEndpoints = requestHandlerEndpointsByMethod
                .get(config.method());

        if (requestHandlerEndpoints != null) {
            String endpoint = getEndpoint(handler, config);
            return requestHandlerEndpoints.get(endpoint,
                    new HashMap<String, Object>()) != null;
        }
        return false;
    }

    private String getEndpoint(RequestHandler handler, ServiceConfigDesc config) {
        String endpoint = config.endpoint();
        if (endpoint == null || endpoint.length() == 0) {
            throw new IllegalArgumentException("service handler " + handler
                    + "'s endpoint is empty");
        }
        return endpoint;
    }

    @Override
    public String toString() {
        return "DefaultHttpServiceContainer [requestHandlerEndpointsByMethod="
                + requestHandlerEndpointsByMethod + ", server=" + server + "]"
                + System.identityHashCode(this);
    }
}
