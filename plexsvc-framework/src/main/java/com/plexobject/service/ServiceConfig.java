package com.plexobject.service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.plexobject.encode.CodecType;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ServiceConfig {
    public enum Method {
        GET, POST, PUT, DELETE, HEAD, MESSAGE
    }

    public enum Protocol {
        HTTP, WEBSOCKET, JMS, EVENT_BUS
    }

    Class<?> requestClass();

	Protocol protocol();

    CodecType codec() default CodecType.JSON;

    Method method();

    String version() default "1.0";

    String endpoint() default "";

    boolean recordStatsdMetrics() default true;

    String[] rolesAllowed() default "";
}
