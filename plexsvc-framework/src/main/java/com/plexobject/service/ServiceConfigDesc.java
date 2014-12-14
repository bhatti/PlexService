package com.plexobject.service;

import java.io.Serializable;
import java.util.Arrays;

import com.plexobject.bridge.web.WebToJmsEntry;
import com.plexobject.encode.CodecType;
import com.plexobject.handler.RequestHandler;

public class ServiceConfigDesc implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Method method;
    private final Protocol protocol;
    private final Class<?> payloadClass;
    private final CodecType codecType;
    private final String version;
    private final String endpoint;
    private final boolean recordStatsdMetrics;
    private final String[] rolesAllowed;
    private final int concurrency;

    public static class Builder {
        private Method method;
        private Protocol protocol;
        private Class<?> payloadClass = Void.class;
        private CodecType codecType;
        private String version;
        private String endpoint;
        private boolean recordStatsdMetrics;
        private String[] rolesAllowed;
        private int concurrency;

        public Builder(WebToJmsEntry e) {
            if (e != null) {
                this.method = e.getMethod();
                this.protocol = e.getMethod() == Method.MESSAGE ? Protocol.WEBSOCKET
                        : Protocol.HTTP;
                this.payloadClass = Void.class;
                this.codecType = e.getCodecType();
                this.version = "";
                this.endpoint = e.getEndpoint();
                this.recordStatsdMetrics = true;
                this.concurrency = e.getConcurrency();
                this.rolesAllowed = new String[0];
            }
        }

        public Builder(Object handler) {
            this(handler.getClass());
        }

        public Builder(Class<?> handler) {
            this(handler.getAnnotation(ServiceConfig.class));
        }

        public Builder(ServiceConfig config) {
            if (config != null) {
                this.method = config.method();
                this.protocol = config.protocol();
                this.payloadClass = config.payloadClass();
                this.codecType = config.codec();
                this.version = config.version();
                this.endpoint = config.endpoint();
                this.recordStatsdMetrics = config.recordStatsdMetrics();
                this.rolesAllowed = config.rolesAllowed();
                this.concurrency = config.concurrency();
            }
        }

        public Builder setMethod(Method method) {
            this.method = method;
            return this;
        }

        public Builder setProtocol(Protocol protocol) {
            this.protocol = protocol;
            return this;
        }

        public Builder setPayloadClass(Class<?> payloadClass) {
            this.payloadClass = payloadClass;
            return this;
        }

        public Builder setCodecType(CodecType codecType) {
            this.codecType = codecType;
            return this;
        }

        public Builder setVersion(String version) {
            this.version = version;
            return this;
        }

        public Builder setEndpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public Builder setRecordStatsdMetrics(boolean recordStatsdMetrics) {
            this.recordStatsdMetrics = recordStatsdMetrics;
            return this;
        }

        public Builder setRolesAllowed(String[] rolesAllowed) {
            this.rolesAllowed = rolesAllowed;
            return this;
        }

        public Builder setConcurrency(int concurrency) {
            this.concurrency = concurrency;
            return this;
        }

        public ServiceConfigDesc build() {
            return new ServiceConfigDesc(method, protocol, payloadClass,
                    codecType, version, endpoint, recordStatsdMetrics,
                    rolesAllowed, concurrency);
        }
    }

    public ServiceConfigDesc(Object handler) {
        this(handler.getClass());
    }

    public ServiceConfigDesc(Class<?> handler) {
        this(handler.getAnnotation(ServiceConfig.class));
    }

    public ServiceConfigDesc(ServiceConfig config) {
        this(config.method(), config.protocol(), config.payloadClass(), config
                .codec(), config.version(), config.endpoint(), config
                .recordStatsdMetrics(), config.rolesAllowed(), config
                .concurrency());
    }

    public ServiceConfigDesc(Method method, Protocol protocol,
            Class<?> payloadClass, CodecType codecType, String version,
            String endpoint, boolean recordStatsdMetrics,
            String[] rolesAllowed, int concurrency) {
        this.method = method;
        this.protocol = protocol;
        this.payloadClass = payloadClass;
        this.codecType = codecType;
        this.version = version;
        this.endpoint = endpoint;
        this.recordStatsdMetrics = recordStatsdMetrics;
        this.rolesAllowed = rolesAllowed;
        this.concurrency = concurrency;
    }

    public Method method() {
        return method;
    }

    public Protocol protocol() {
        return protocol;
    }

    public Class<?> payloadClass() {
        return payloadClass;
    }

    public CodecType codec() {
        return codecType;
    }

    public String version() {
        return version;
    }

    public String endpoint() {
        return endpoint;
    }

    public boolean recordStatsdMetrics() {
        return recordStatsdMetrics;
    }

    public String[] rolesAllowed() {
        return rolesAllowed;
    }

    public int getConcurrency() {
        return concurrency;
    }

    public static Builder builder(WebToJmsEntry e) {
        return new Builder(e);
    }

    public static Builder builder(RequestHandler handler) {
        return new Builder(handler);
    }

    @Override
    public String toString() {
        return "ServiceConfigDesc [method=" + method + ", protocol=" + protocol
                + ", payloadClass=" + payloadClass + ", codecType=" + codecType
                + ", version=" + version + ", endpoint=" + endpoint
                + ", recordStatsdMetrics=" + recordStatsdMetrics
                + ", rolesALlowed=" + Arrays.toString(rolesAllowed) + "]";
    }
}
