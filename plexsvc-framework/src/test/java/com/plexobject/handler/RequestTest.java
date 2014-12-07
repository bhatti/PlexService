package com.plexobject.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.plexobject.domain.Constants;
import com.plexobject.service.Method;
import com.plexobject.service.Protocol;

public class RequestTest {
    @Test
    public void testCreateUsingDefaultConstructor() throws Exception {
        Request request = new Request();
        assertNull(request.getProtocol());
        assertNull(request.getMethod());
        assertNull( request.getEndpoint());
        assertNull(request.getResponseDispatcher());
        assertNull(request.getSessionId());
        assertNull( request.getPayload());
    }

    @Test
    public void testCreateUsingConstructor() throws Exception {
        Map<String, Object> properties = new HashMap<>();
        properties.put(Constants.SESSION_ID, "id");
        Map<String, Object> headers = new HashMap<>();
        AbstractResponseDispatcher dispatcher = new AbstractResponseDispatcher() {
            @Override
            public void addSessionId(String value) {
            }
        };
        String payload = "{}";
        Request request = new Request(Protocol.HTTP, Method.GET, "/w",
                properties, headers, payload, dispatcher);
        assertEquals(Protocol.HTTP, request.getProtocol());
        assertEquals(Method.GET, request.getMethod());
        assertEquals("/w", request.getEndpoint());
        assertEquals(dispatcher, request.getResponseDispatcher());
        assertEquals("id", request.getSessionId());
        assertEquals("{}", request.getPayload());
        assertTrue(request.toString().contains("/w"));
        assertTrue(request.getCreatedAt() <= System.currentTimeMillis());
    }

    @Test
    public void testCreateUsingBuilder() throws Exception {
        Request.Builder builder = Request.builder();
        builder.setSessionId(null);
        builder.setSessionId("id");
        AbstractResponseDispatcher dispatcher = new AbstractResponseDispatcher() {
            @Override
            public void addSessionId(String value) {
            }
        };
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> headers = new HashMap<>();

        builder.setProperties(properties);
        builder.setHeaders(headers);
        builder.setResponseDispatcher(dispatcher);
        builder.setPayload("{}");
        builder.setProtocol(Protocol.HTTP);
        builder.setMethod(Method.GET);
        builder.setEndpoint("/w");
        Request request = builder.build();
        assertEquals(Protocol.HTTP, request.getProtocol());
        assertEquals(Method.GET, request.getMethod());
        assertEquals("/w", request.getEndpoint());
        assertEquals(dispatcher, request.getResponseDispatcher());
        assertEquals("id", request.getSessionId());
        assertEquals("{}", request.getPayload());
    }
    

    @Test
    public void testHandleUnknown() throws Exception {
        Map<String, Object> properties = new HashMap<>();
        properties.put("p1", "v1");
        Map<String, Object> headers = new HashMap<>();
        String payload = "{}";
        Request request = new Request(Protocol.HTTP, Method.GET, "/w",
                properties, headers, payload, null);
        request.handleUnknown(null, null);
        request.handleUnknown(Constants.PAYLOAD, "payload");
        assertEquals("payload", request.getPayload());
        request.handleUnknown("key", "1");
        assertEquals("1", request.getProperty("key"));
        request.handleUnknown("key", 1);
        assertEquals(new Integer(1), request.getProperty("key"));
        request.handleUnknown("key", 1L);
        assertEquals(new Long(1), request.getProperty("key"));
        request.handleUnknown("key", 'k');
        assertEquals(new Character('k'), request.getProperty("key"));
        request.handleUnknown("key", true);
        assertEquals(Boolean.TRUE, request.getProperty("key"));
        request.handleUnknown("key", properties);
        assertEquals("v1", request.getProperty("p1"));
    }
    @Test
    public void testGetSetProperties() throws Exception {
        Map<String, Object> properties = new HashMap<>();
        properties.put("p1", "v1");
        Map<String, Object> headers = new HashMap<>();
        Request request = new Request(Protocol.HTTP, Method.GET, "/w",
                properties, headers, null, null);
        request.setPayload("pay");
        assertEquals("pay", request.getPayload());
        
        assertTrue(request.hasProperty("p1"));
        assertFalse(request.hasProperty("p2"));
        assertTrue(request.getPropertyNames().contains("p1"));
        assertEquals("v1", request.getStringProperty("p1"));
        request.setProperty("p2", 2);
        assertEquals("2", request.getStringProperty("p2"));
        assertEquals(new Long(2), request.getLongProperty("p2"));
        assertEquals(properties, request.getProperties());
        assertNull(request.getStringProperty("p3"));
        assertNull(request.getLongProperty("p3"));
        request.setProperty("p3", 3L);
        assertEquals(new Long(3), request.getLongProperty("p3"));
        request.setProperty("p3", "3");
        assertEquals(new Long(3), request.getLongProperty("p3"));
        request.setProperty("p4", "true");
        assertTrue(request.getBooleanProperty("p4"));
        request.setProperty("p4", true);
        assertTrue(request.getBooleanProperty("p4", false));
        request.setProperty("p4", 4);
        assertFalse(request.getBooleanProperty("p4", false));
        assertFalse(request.getBooleanProperty("p5", false));

    }
    @Test
    public void testGetSetHeaders() throws Exception {
        Map<String, Object> properties = new HashMap<>();
        properties.put(Constants.SESSION_ID, "id");
        Map<String, Object> headers = new HashMap<>();
        headers.put("head1", "val1");
        headers.put("head2", 2);
        Request request = new Request(Protocol.HTTP, Method.GET, "/w",
                properties, headers, null, null);
        assertTrue(request.getHeaderNames().contains("head1"));
        assertEquals("val1", request.getHeader("head1"));
        assertEquals("2", request.getHeader("head2"));
        assertNull(request.getHeader("head3"));
        request.setHeader("head3", 3);
        assertEquals("3", request.getHeader("head3"));
        assertEquals(headers, request.getHeaders());
    }
}