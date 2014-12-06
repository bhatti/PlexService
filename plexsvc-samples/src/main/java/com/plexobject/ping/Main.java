package com.plexobject.ping;

import java.util.Arrays;
import java.util.Collection;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;

import com.plexobject.bridge.web.WebToJmsBridge;
import com.plexobject.bridge.web.WebToJmsEntry;
import com.plexobject.encode.CodecType;
import com.plexobject.http.servlet.ServiceRegistryCallback;
import com.plexobject.service.Method;
import com.plexobject.service.Protocol;
import com.plexobject.service.ServiceConfigDesc;
import com.plexobject.service.ServiceRegistry;
import com.plexobject.util.Configuration;

public class Main implements ServiceRegistryCallback {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: java " + Main.class.getName()
                    + " properties-file [web|jms]");
            System.exit(1);
        }
        BasicConfigurator.configure();
        LogManager.getRootLogger().setLevel(Level.INFO);
        Configuration config = new Configuration(args[0]);
        PingService pingService = new PingService();

        String type = args[1];
        ServiceRegistry serviceRegistry = new ServiceRegistry(config, null);
        ReverseService reverseService = new ReverseService();
        SimpleService simpleService = new SimpleService();

        // ensure activemq is already running
        if ("jms".equalsIgnoreCase(type)) {
            serviceRegistry.add(
                    pingService,
                    ServiceConfigDesc.builder(pingService)
                            .setMethod(Method.MESSAGE)
                            .setProtocol(Protocol.JMS)
                            .setEndpoint("queue:ping").build());
            Collection<WebToJmsEntry> entries = Arrays.asList(
                    new WebToJmsEntry(CodecType.JSON, "/ping", Method.GET,
                            "queue:ping", 5), new WebToJmsEntry(CodecType.JSON,
                            "/ping", Method.MESSAGE, "queue:ping", 5));
            new WebToJmsBridge(entries, serviceRegistry, config);
        } else if ("websocket".equalsIgnoreCase(type)) {
            serviceRegistry.add(
                    pingService,
                    ServiceConfigDesc.builder(pingService)
                            .setMethod(Method.MESSAGE)
                            .setProtocol(Protocol.WEBSOCKET)
                            .setEndpoint("/ping").build());
            serviceRegistry.add(pingService,
                    ServiceConfigDesc.builder(pingService)
                            .setMethod(Method.GET).setProtocol(Protocol.HTTP)
                            .setEndpoint("/ping").build());
        } else {
            serviceRegistry.add(pingService);
        }
        serviceRegistry.add(reverseService);
        serviceRegistry.add(simpleService);
        serviceRegistry.start();
        Thread.currentThread().join();
    }

    @Override
    public void created(ServiceRegistry serviceRegistry) {
        BasicConfigurator.configure();
        LogManager.getRootLogger().setLevel(Level.INFO);

        PingService pingService = new PingService();
        ReverseService reverseService = new ReverseService();
        SimpleService simpleService = new SimpleService();

        serviceRegistry.add(pingService);
        serviceRegistry.add(reverseService);
        serviceRegistry.add(simpleService);
    }
}
