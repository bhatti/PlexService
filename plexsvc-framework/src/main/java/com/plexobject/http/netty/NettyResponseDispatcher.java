package com.plexobject.http.netty;

import static io.netty.handler.codec.http.HttpHeaders.Names.LOCATION;
import static io.netty.handler.codec.http.HttpHeaders.Names.SET_COOKIE;
import static io.netty.handler.codec.http.HttpResponseStatus.FOUND;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpHeaders.Values;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.ServerCookieEncoder;
import io.netty.util.CharsetUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.plexobject.http.Handledable;
import com.plexobject.http.HttpResponse;
import com.plexobject.http.HttpResponseDispatcher;

public class NettyResponseDispatcher extends HttpResponseDispatcher {
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String CONNECTION = "Connection";
    private static final String CONTENT_LENGTH = "Content-Length";

    public NettyResponseDispatcher(final HttpRequest request,
            final ChannelHandlerContext ctx) {
        super(getHandledable(request), getHttpResponse(request, ctx));
    }

    private static Handledable getHandledable(final HttpRequest request) {
        return new Handledable() {
            @Override
            public void setHandled(boolean h) {
            }
        };
    }

    private static HttpResponse getHttpResponse(final HttpRequest request,
            ChannelHandlerContext ctx) {
        final boolean keepAlive = HttpHeaders.isKeepAlive(request);
        return new HttpResponse() {
            private final Map<String, String> headers = new HashMap<>();
            private final Map<String, String> cookies = new HashMap<>();
            private int status = HttpResponse.SC_OK;
            private String errorMessage;
            private String location;

            @Override
            public void setContentType(String type) {
                headers.put(CONTENT_TYPE, type);
            }

            @Override
            public void addCookie(String name, String value) {
                cookies.put(name, value);
            }

            @Override
            public void sendRedirect(String location) throws IOException {
                this.location = location;
                FullHttpResponse response = new DefaultFullHttpResponse(
                        HTTP_1_1, FOUND);
                response.headers().set(LOCATION, location);
                // Close the connection as soon as the error message is
                // sent.
                ctx.writeAndFlush(response).addListener(
                        ChannelFutureListener.CLOSE);
            }

            @Override
            public void addHeader(String name, String value) {
                headers.put(name, value);
            }

            @Override
            public void setStatus(int sc) {
                status = sc;
            }

            @Override
            public void send(String contents) throws IOException {
                if (location != null || errorMessage != null) {
                    return;
                }
                FullHttpResponse response = new DefaultFullHttpResponse(
                        HTTP_1_1, HttpResponseStatus.valueOf(status),
                        Unpooled.copiedBuffer(contents, CharsetUtil.UTF_8));
                response.headers().set(CONTENT_LENGTH,
                        response.content().readableBytes());
                for (Map.Entry<String, String> e : headers.entrySet()) {
                    response.headers().set(e.getKey(), e.getValue());
                }
                for (Map.Entry<String, String> e : cookies.entrySet()) {
                    response.headers()
                            .add(SET_COOKIE,
                                    ServerCookieEncoder.encode(e.getKey(),
                                            e.getValue()));
                }
                if (!keepAlive) {
                    ctx.write(response)
                            .addListener(ChannelFutureListener.CLOSE);
                } else {
                    response.headers().set(CONNECTION, Values.KEEP_ALIVE);
                    ctx.write(response);
                }
                ctx.flush();
                log.info(">>>>> Sending " + response + ", contents '" + contents + "'\n\n", new Exception("st"));
            }

            @Override
            public void sendError(int sc, String msg) throws IOException {
                this.status = sc;
                this.errorMessage = msg;
                FullHttpResponse response = new DefaultFullHttpResponse(
                        HTTP_1_1, HttpResponseStatus.valueOf(status),
                        Unpooled.copiedBuffer("Failure: " + status + "\r\n",
                                CharsetUtil.UTF_8));
                response.headers().set(CONTENT_TYPE,
                        "text/plain; charset=UTF-8");

                // Close the connection as soon as the error message is sent.
                ctx.writeAndFlush(response).addListener(
                        ChannelFutureListener.CLOSE);
            }
        };
    }
}