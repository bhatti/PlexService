package com.plexobject.service;

import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.plexobject.domain.Redirectable;
import com.plexobject.domain.Statusable;
import com.plexobject.encode.CodecType;
import com.plexobject.encode.ObjectCodecFactory;
import com.plexobject.handler.Request;
import com.plexobject.handler.RequestHandler;
import com.plexobject.http.DefaultHttpRequestHandler;
import com.plexobject.http.DefaultHttpServiceContainer;
import com.plexobject.http.HttpResponse;
import com.plexobject.http.HttpServerFactory;
import com.plexobject.jms.JmsClient;
import com.plexobject.jms.JmsServiceContainer;
import com.plexobject.metrics.ServiceMetrics;
import com.plexobject.metrics.ServiceMetricsRegistry;
import com.plexobject.route.RouteResolver;
import com.plexobject.security.RoleAuthorizer;
import com.plexobject.util.Configuration;
import com.plexobject.validation.IRequiredFieldValidator;
import com.plexobject.validation.RequiredFieldValidator;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;

/**
 * This class defines registry for service handlers
 * 
 * @author shahzad bhatti
 *
 */
public class ServiceRegistry implements ServiceContainer, ServiceRegistryMBean {
    private static final Logger log = LoggerFactory
            .getLogger(ServiceRegistry.class);

    private final Map<Protocol, ServiceContainer> containers = new HashMap<>();
    private final Map<RequestHandler, ServiceConfigDesc> handlerConfigs = new HashMap<>();
    private final RoleAuthorizer authorizer;
    private final JmsClient jmsClient;
    private boolean running;
    private final StatsDClient statsd;
    private ServiceMetricsRegistry serviceMetricsRegistry;
    private IRequiredFieldValidator requiredFieldValidator = new RequiredFieldValidator();
    private MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

    public ServiceRegistry(Configuration config, RoleAuthorizer authorizer,
            JmsClient jmsClient) {
        this.authorizer = authorizer;
        this.jmsClient = jmsClient;
        this.containers.putAll(getDefaultServiceContainers(config, authorizer));
        String statsdHost = config.getProperty("statsd.host");
        if (statsdHost != null) {
            String servicePrefix = config.getProperty("serviceConfigs", "");
            this.statsd = new NonBlockingStatsDClient(config.getProperty(
                    "statsd.prefix", servicePrefix), statsdHost,
                    config.getInteger("statsd.port", 8125));
        } else {
            this.statsd = null;
        }
        serviceMetricsRegistry = new ServiceMetricsRegistry(this, statsd);
        try {
            mbs.registerMBean(this, new ObjectName(
                    "PlexService:name=ServiceRegistry"));
        } catch (InstanceAlreadyExistsException e) {
        } catch (Exception e) {
            log.error("Could not register mbean for service-registry", e);
        }
    }

    @Override
    public synchronized boolean isRunning() {
        return running;
    }

    public ServiceConfigDesc getServiceConfig(RequestHandler h) {
        ServiceConfigDesc config = handlerConfigs.get(h);
        if (config == null) {
            config = new ServiceConfigDesc(h);
        }
        return config;
    }

    @Override
    public void add(RequestHandler h) {
        add(h, new ServiceConfigDesc(h));
    }

    public synchronized void add(RequestHandler h, ServiceConfigDesc config) {
        Objects.requireNonNull(config, "service handler " + h
                + " doesn't define ServiceConfig annotation");
        handlerConfigs.put(h, config);
        ServiceContainer container = containers.get(config.protocol());
        Objects.requireNonNull(container,
                "Unsupported container for service handler " + h);
        if (!container.exists(h)) {
            registerMetricsJMX(h);
            registerServiceHandlerLifecycle(h);
            container.add(h);
        }
    }

    public synchronized ServiceMetricsRegistry getServiceMetricsRegistry() {
        return serviceMetricsRegistry;
    }

    private void registerServiceHandlerLifecycle(RequestHandler h) {
        String objName = getPackageName(h) + h.getClass().getSimpleName()
                + ":type=Lifecycle";
        try {
            mbs.registerMBean(new ServiceHandlerLifecycle(this, h),
                    new ObjectName(objName));
        } catch (InstanceAlreadyExistsException e) {
        } catch (Exception e) {
            log.error("Could not register mbean " + objName, e);
        }
    }

    private static String getPackageName(RequestHandler h) {
        return h.getClass().getPackage().getName().replaceAll(".*\\.", "")
                + ".";
    }

    private void registerMetricsJMX(RequestHandler h) {
        String objName = getPackageName(h) + h.getClass().getSimpleName()
                + ":type=Metrics";
        ServiceMetrics metrics = serviceMetricsRegistry.getServiceMetrics(h);
        try {
            mbs.registerMBean(metrics, new ObjectName(objName));
        } catch (InstanceAlreadyExistsException e) {
        } catch (Exception e) {
            log.error("Could not register mbean " + objName, e);
        }
    }

    @Override
    public synchronized boolean remove(RequestHandler h) {
        ServiceConfigDesc config = getServiceConfig(h);
        Objects.requireNonNull(config, "config" + h
                + " doesn't define ServiceConfig annotation");
        ServiceContainer container = containers.get(config.protocol());
        if (container == null) {
            return false;
        }
        if (container.remove(h)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean exists(RequestHandler h) {
        ServiceConfigDesc config = getServiceConfig(h);
        Objects.requireNonNull(config, "config" + h
                + " doesn't define ServiceConfig annotation");
        ServiceContainer container = containers.get(config.protocol());
        if (container == null) {
            return false;
        }
        return container.exists(h);
    }

    @Override
    public Collection<ServiceConfigDesc> getServiceConfigurations() {
        Collection<ServiceConfigDesc> configs = new HashSet<>();
        for (RequestHandler h : getHandlers()) {
            configs.add(getServiceConfig(h));
        }
        return configs;
    }

    public String dumpServiceConfigurations() {
        StringBuilder sb = new StringBuilder();
        for (ServiceConfigDesc c : getServiceConfigurations()) {
            sb.append(c.protocol() + ":" + c.method() + "->" + c.endpoint()
                    + " " + c.codec() + "\n");
        }
        return sb.toString();
    }

    @Override
    public synchronized Collection<RequestHandler> getHandlers() {
        Collection<RequestHandler> handlers = new HashSet<>();
        for (ServiceContainer g : containers.values()) {
            handlers.addAll(g.getHandlers());
        }
        return handlers;
    }

    private Map<Protocol, ServiceContainer> getDefaultServiceContainers(
            Configuration config, RoleAuthorizer authorizer) {
        final Map<Protocol, ServiceContainer> containers = new HashMap<>();
        ServiceContainer webServiceContainer = getHttpServiceContainer(config,
                authorizer,
                new ConcurrentHashMap<Method, RouteResolver<RequestHandler>>());
        try {
            containers.put(Protocol.HTTP, webServiceContainer);
            containers.put(Protocol.JMS, new JmsServiceContainer(config, this,
                    jmsClient));
            containers.put(Protocol.WEBSOCKET, webServiceContainer);
            return containers;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to add containers", e);
        }
    }

    @Override
    public synchronized void start() {
        for (ServiceContainer g : containers.values()) {
            if (g.getHandlers().size() > 0) {
                g.start();
            }
        }
        running = true;
    }

    @Override
    public synchronized void stop() {
        for (ServiceContainer g : containers.values()) {
            if (g.getHandlers().size() > 0) {
                g.stop();
            }
        }
        running = false;
    }

    /**
     * This method executes handler by encoding the payload to proper java class
     * and enforces security set by the underlying application.
     * 
     * @param request
     * @param handler
     */
    public void invoke(Request request, RequestHandler handler) {
        if (handler != null) {
            final long started = System.currentTimeMillis();
            ServiceMetrics metrics = serviceMetricsRegistry
                    .getServiceMetrics(handler);

            ServiceConfigDesc config = getServiceConfig(handler);
            if (log.isDebugEnabled()) {
                log.debug("Received request for handler "
                        + handler.getClass().getSimpleName() + ", protocol "
                        + config.protocol() + ", payload "
                        + request.getPayload() + ", params "
                        + request.getProperties());
            }

            // override payload in request
            Object payload = config.payloadClass() != Void.class ? ObjectCodecFactory
                    .getInstance()
                    .getObjectCodec(config.codec())
                    .decode((String) request.getPayload(),
                            config.payloadClass(), request.getProperties())
                    : null;

            // validate required fields
            requiredFieldValidator.validate(handler,
                    payload == null ? request.getProperties() : payload);

            // update post parameters
            if (payload != null) {
                request.setPayload(payload);
            } else if (request.getPayload() != null) {
                String[] nvArr = request.getPayload().toString().split("&");
                for (String nvStr : nvArr) {
                    String[] nv = nvStr.split("=");
                    if (nv.length == 2) {
                        request.setProperty(nv[0], nv[1]);
                    }
                }
            }
            //
            try {
                if (authorizer != null && config.rolesAllowed() != null
                        && config.rolesAllowed().length > 0
                        && !config.rolesAllowed()[0].equals("")) {
                    authorizer.authorize(request, config.rolesAllowed());
                }
                handler.handle(request);
                metrics.addResponseTime(System.currentTimeMillis() - started);
            } catch (Exception e) {
                metrics.incrementErrors();
                if (e instanceof Redirectable) {
                    if (((Redirectable) e).getLocation() != null) {
                        request.getResponseDispatcher().setLocation(
                                ((Redirectable) e).getLocation());
                    }
                }
                if (e instanceof Statusable) {
                    request.getResponseDispatcher().setStatus(
                            ((Statusable) e).getStatus());
                } else {
                    request.getResponseDispatcher().setStatus(
                            HttpResponse.SC_INTERNAL_SERVER_ERROR);
                }
                request.getResponseDispatcher().send(e);
            }
        } else {
            log.warn("Received Unknown request params "
                    + request.getProperties() + ", payload "
                    + request.getPayload());
            request.getResponseDispatcher().setCodecType(CodecType.TEXT);
            request.getResponseDispatcher()
                    .setStatus(HttpResponse.SC_NOT_FOUND);
            request.getResponseDispatcher().send("page not found");
        }
    }

    private ServiceContainer getHttpServiceContainer(
            final Configuration config,
            final RoleAuthorizer authorizer,
            final Map<Method, RouteResolver<RequestHandler>> requestHandlerPathsByMethod) {
        RequestHandler executor = new DefaultHttpRequestHandler(this,
                requestHandlerPathsByMethod);
        Lifecycle server = HttpServerFactory.getHttpServer(config, executor);
        return new DefaultHttpServiceContainer(config, this,
                requestHandlerPathsByMethod, server);
    }
}
