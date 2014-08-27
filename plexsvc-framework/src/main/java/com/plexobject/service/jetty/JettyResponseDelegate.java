package com.plexobject.service.jetty;

import java.io.IOException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;

import com.plexobject.encode.CodecType;
import com.plexobject.service.http.Handledable;
import com.plexobject.service.http.HttpResponse;
import com.plexobject.service.http.HttpResponseDelegate;

public class JettyResponseDelegate extends HttpResponseDelegate {
    public JettyResponseDelegate(final CodecType codecType,
            final Request baseRequest, final HttpServletResponse response) {
        super(codecType, getHandledable(baseRequest), getHttpResponse(response));
    }

    private static Handledable getHandledable(final Request baseRequest) {
        return new Handledable() {
            @Override
            public void setHandled(boolean h) {
                baseRequest.setHandled(h);
            }
        };
    }

    private static HttpResponse getHttpResponse(
            final HttpServletResponse response) {
        return new HttpResponse() {
            @Override
            public void setContentType(String type) {
                response.setContentType(type);
            }

            @Override
            public void addCookie(String name, String value) {
                response.addCookie(new Cookie(name, value));
            }

            @Override
            public void sendRedirect(String location) throws IOException {
                response.sendRedirect(location);
            }

            @Override
            public void addHeader(String name, String value) {
                response.addHeader(name, value);
            }

            @Override
            public void setStatus(int sc) {
                response.setStatus(sc);

            }

            @Override
            public void send(String contents) throws IOException {
                response.getWriter().println(contents);
            }

            @Override
            public void sendError(int sc, String msg) throws IOException {
                response.sendError(sc, msg);                
            }
        };
    }
}
