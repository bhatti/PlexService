package com.plexobject.bus.impl;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.plexobject.bus.EventBus;
import com.plexobject.encode.CodecType;
import com.plexobject.handler.AbstractResponseDispatcher;
import com.plexobject.handler.Request;
import com.plexobject.handler.RequestHandler;
import com.plexobject.handler.Response;
import com.plexobject.predicate.Predicate;
import com.plexobject.service.Method;
import com.plexobject.service.Protocol;

public class EventBusImplTest {
    private RequestHandler handler = new RequestHandler() {
        @Override
        public void handle(Request<Object> request) {
            requests.add(request);
        }
    };
    private Predicate<Request<Object>> filter = new Predicate<Request<Object>>() {
        @Override
        public boolean accept(Request<Object> obj) {
            return obj.getPayload().toString().contains("ok");
        }
    };

    private List<Request<Object>> requests = new ArrayList<>();
    private EventBus bus = new EventBusImpl();

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testPublishSubscribeWithoutFilter() throws Exception {
        bus.subscribe("channel", handler, null);
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> headers = new HashMap<>();
        String payload = "payload";
        Request<Object> request = Request
                .objectBuilder()
                .setProtocol(Protocol.HTTP)
                .setMethod(Method.GET)
                .setProperties(properties)
                .setHeaders(headers)
                .setEndpoint("/w")
                .setCodecType(CodecType.JSON)
                .setPayload(payload)
                .setResponse(
                        new Response(new HashMap<String, Object>(),
                                new HashMap<String, Object>(), "",
                                CodecType.JSON))
                .setResponseDispatcher(new AbstractResponseDispatcher() {
                }).build();
        bus.publish("channel", request);
        Thread.sleep(200);
        assertEquals("payload", requests.get(0).getPayload());
    }

    @Test
    public void testPublishSubscribeWithFilter() throws Exception {
        bus.subscribe("channel", handler, filter);
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> headers = new HashMap<>();
        String payload = "payload";
        Request<Object> request = Request
                .objectBuilder()
                .setProtocol(Protocol.HTTP)
                .setMethod(Method.GET)
                .setProperties(properties)
                .setHeaders(headers)
                .setEndpoint("/w")
                .setCodecType(CodecType.JSON)
                .setPayload(payload)
                .setResponse(
                        new Response(new HashMap<String, Object>(),
                                new HashMap<String, Object>(), "",
                                CodecType.JSON))
                .setResponseDispatcher(new AbstractResponseDispatcher() {
                }).build();
        bus.publish("channel", request);
        Thread.sleep(200);
        assertEquals(0, requests.size());
        request.setPayload("ok");
        bus.publish("channel", request);
        Thread.sleep(200);
        assertEquals("ok", requests.get(0).getPayload());
    }

    @Test
    public void testSubscribeUnsubscribe() throws Exception {
        long id = bus.subscribe("channel", handler, filter);
        bus.unsubscribe(id);
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> headers = new HashMap<>();
        String payload = "payload";
        Request<Object> request = Request
                .objectBuilder()
                .setProtocol(Protocol.HTTP)
                .setMethod(Method.GET)
                .setProperties(properties)
                .setHeaders(headers)
                .setEndpoint("/w")
                .setCodecType(CodecType.JSON)
                .setPayload(payload)
                .setResponse(
                        new Response(new HashMap<String, Object>(),
                                new HashMap<String, Object>(), "",
                                CodecType.JSON))
                .setResponseDispatcher(new AbstractResponseDispatcher() {
                }).build();
        bus.publish("channel", request);
        Thread.sleep(200);
        assertEquals(0, requests.size());
    }
}
