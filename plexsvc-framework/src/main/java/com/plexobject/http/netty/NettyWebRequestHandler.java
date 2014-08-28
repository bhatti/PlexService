package com.plexobject.http.netty;

import static io.netty.handler.codec.http.HttpHeaders.Names.COOKIE;
import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.CharsetUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.plexobject.handler.AbstractResponseDispatcher;
import com.plexobject.http.WebRequestHandler;
import com.plexobject.service.ServiceConfig.Method;

@Sharable
public class NettyWebRequestHandler extends ChannelInboundHandlerAdapter {
    private static final Logger log = LoggerFactory
            .getLogger(NettyWebRequestHandler.class);

    private final WebRequestHandler handler;

    public NettyWebRequestHandler(WebRequestHandler handler) {
        this.handler = handler;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof HttpRequest) {
            HttpRequest req = (HttpRequest) msg;

            if (HttpHeaders.is100ContinueExpected(req)) {
                ctx.write(new DefaultFullHttpResponse(HTTP_1_1, CONTINUE));
                return;
            }

            AbstractResponseDispatcher dispatcher = new NettyResponseDispatcher(
                    req, ctx);
            String payload = null;

            if (req instanceof HttpContent) {
                HttpContent content = (HttpContent) req;
                payload = content.content().toString(CharsetUtil.UTF_8);
            }
            Method method = Method.valueOf(req.getMethod().name());
            String uri = req.getUri();
            int n = uri.indexOf("?");
            if (n != -1) {
                uri = uri.substring(0, n);
            }
            handler.handle(method, uri, payload, getParams(req),
                    getHeaders(req), dispatcher);

            if (req instanceof LastHttpContent) {
                ctx.close();
            }
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelInactive();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        log.warn("exceptionCaught: Channel failed with remote address "
                + ctx.channel().remoteAddress(), cause.getCause());
    }

    public static Map<String, Object> getParams(HttpRequest request) {
        Map<String, Object> params = new HashMap<>();
        QueryStringDecoder decoder = new QueryStringDecoder(request.getUri());
        for (Map.Entry<String, List<String>> e : decoder.parameters()
                .entrySet()) {
            params.put(e.getKey(), e.getValue().get(0));
        }

        // if (request.getMethod() == HttpMethod.POST) {
        // ChannelBuffer content = request.getDecoderResult();
        // if (content.readable()) {
        // String param = content.toString(WaarpStringUtils.UTF8);
        // QueryStringDecoder queryStringDecoder2 = new QueryStringDecoder(
        // "/?" + param);
        // params = queryStringDecoder2.getParameters();
        // } else {
        // params = null;
        // }
        // }
        // params.put(Constants.HOST, request.getRemoteHost());
        return params;

    }

    public static Map<String, Object> getHeaders(HttpRequest request) {
        Map<String, Object> result = new HashMap<>();
        String cookieString = request.headers().get(COOKIE);
        if (cookieString != null) {
            Set<Cookie> cookies = CookieDecoder.decode(cookieString);
            if (!cookies.isEmpty()) {
                // Reset the cookies if necessary.
                for (Cookie cookie : cookies) {
                    result.put(cookie.getName(), cookie.getValue());
                    // response.headers().add(SET_COOKIE,
                    // ServerCookieEncoder.encode(cookie));
                }
            }
        }
        for (String name : request.headers().names()) {
            String value = request.headers().get(name);
            result.put(name, value);
        }
        return result;
    }

}