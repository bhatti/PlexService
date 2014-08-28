package com.plexobject.stock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.apache.activemq.broker.BrokerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.plexobject.bridge.WebToJmsBridge;
import com.plexobject.bridge.WebToJmsEntry;
import com.plexobject.bugger.Main;
import com.plexobject.domain.ValidationException;
import com.plexobject.encode.CodecType;
import com.plexobject.handler.Request;
import com.plexobject.handler.RequestHandler;
import com.plexobject.service.ServiceConfig;
import com.plexobject.service.ServiceConfig.GatewayType;
import com.plexobject.service.ServiceConfig.Method;
import com.plexobject.service.ServiceRegistry;
import com.plexobject.util.Configuration;

@ServiceConfig(gateway = GatewayType.WEBSOCKET, requestClass = Void.class, endpoint = "/quotes", method = Method.MESSAGE, codec = CodecType.JSON)
// @ServiceConfig(gateway = GatewayType.JMS, requestClass = Void.class, endpoint
// = "queue:quotes-queue", method = Method.MESSAGE, codec = CodecType.JSON)
public class QuoteServer implements RequestHandler {
    public enum Action {
        SUBSCRIBE, UNSUBSCRIBE
    }

    static final Logger log = LoggerFactory.getLogger(QuoteServer.class);

    private QuoteStreamer quoteStreamer = new QuoteStreamer();

    @Override
    public void handle(Request request) {
        String symbol = request.getProperty("symbol");
        String actionVal = request.getProperty("action");
        log.info("Received " + request);
        ValidationException
                .builder()
                .assertNonNull(symbol, "undefined_symbol", "symbol",
                        "symbol not specified")
                .assertNonNull(actionVal, "undefined_action", "action",
                        "action not specified").end();
        Action action = Action.valueOf(actionVal.toUpperCase());
        if (action == Action.SUBSCRIBE) {
            quoteStreamer.add(symbol, request.getResponseBuilder());
        } else {
            quoteStreamer.remove(symbol, request.getResponseBuilder());
        }
    }

    private static void startJmsBroker() throws Exception {
        BrokerService broker = new BrokerService();

        broker.addConnector("tcp://localhost:61616");

        broker.start();
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: java " + Main.class.getName()
                    + " properties-file");
            System.exit(1);
        }
        Configuration config = new Configuration(args[0]);
        QuoteServer service = new QuoteServer();
        ServiceConfig serviceConfig = service.getClass().getAnnotation(
                ServiceConfig.class);
        if (serviceConfig.gateway() == GatewayType.JMS) {
            startJmsBroker();
            Collection<WebToJmsEntry> entries = Arrays
                    .asList(new WebToJmsEntry(CodecType.JSON, "/quotes",
                            serviceConfig.method(), serviceConfig.endpoint(), 5));
            WebToJmsBridge bridge = new WebToJmsBridge(config, entries,
                    GatewayType.WEBSOCKET);
            bridge.startBridge();
        }
        //
        Collection<RequestHandler> services = new ArrayList<>();
        services.add(new QuoteServer());
        //
        ServiceRegistry serviceRegistry = new ServiceRegistry(config, services,
                null);

        serviceRegistry.start();
        Thread.currentThread().join();
    }
}