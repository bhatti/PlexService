package com.plexobject.service.netty;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.plexobject.domain.Constants;
import com.plexobject.encode.CodecType;
import com.plexobject.encode.ObjectCodecFactory;
import com.plexobject.handler.AbstractResponseDelegate;
import com.plexobject.handler.Response;

public class WebsocketResponseDelegate extends AbstractResponseDelegate {
    private static final Logger log = LoggerFactory
            .getLogger(WebsocketResponseDelegate.class);
    private final Channel channel;
    private final Map<String, Object> properties = new HashMap<>();

    public WebsocketResponseDelegate(final CodecType codecType,
            final Channel channel) {
        super(codecType);
        this.channel = channel;
    }

    public void addSessionId(String value) {
        properties.put(Constants.SESSION_ID, value);
    }

    @Override
    public void send(Object payload) {
        String sessionId = (String) properties.get(Constants.SESSION_ID);
        if (sessionId != null) {
            addSessionId(sessionId);
        }
        try {
            Response response = new Response(properties, payload);
            String responseText = ObjectCodecFactory.getInstance()
                    .getObjectCodec(codecType).encode(response);
            channel.write(new TextWebSocketFrame(responseText));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Failed to send " + payload + ", " + this, e);
            }
            throw new RuntimeException(e);
        }
    }

    @Override
    public int hashCode() {
        String sessionId = (String) properties.get(Constants.SESSION_ID);
        if (sessionId != null) {
            return sessionId.hashCode();
        }

        return channel.localAddress().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        WebsocketResponseDelegate other = (WebsocketResponseDelegate) obj;
        String sessionId = (String) properties.get(Constants.SESSION_ID);
        String otherSessionId = (String) other.properties
                .get(Constants.SESSION_ID);
        if (sessionId != null && otherSessionId != null) {
            return sessionId.equals(otherSessionId);
        }
        return channel.localAddress().equals(other.channel.localAddress());
    }

    @Override
    public String toString() {
        return "WebsocketResponse " + channel;
    }

}
