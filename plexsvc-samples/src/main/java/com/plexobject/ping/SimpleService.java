package com.plexobject.ping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.plexobject.encode.CodecType;
import com.plexobject.handler.Request;
import com.plexobject.handler.RequestHandler;
import com.plexobject.service.Method;
import com.plexobject.service.Protocol;
import com.plexobject.service.ServiceConfig;

@ServiceConfig(protocol = Protocol.HTTP, endpoint = "/person", method = Method.POST, payloadClass = Person.class, codec = CodecType.JSON)
public class SimpleService implements RequestHandler {
    private static final Logger log = LoggerFactory
            .getLogger(ReverseService.class);

    public SimpleService() {
        log.info("Simple Service Started");
    }

    @Override
    public void handle(Request request) {
        Person person = request.getPayload();
        log.info("Received " + person);
        if (person == null) {
            person = new Person();
        }
        person.setId(System.currentTimeMillis());
        person.setName(person.getName() + System.currentTimeMillis());
        person.setEmail(person.getEmail() + System.currentTimeMillis()
                + "@gmail.com");
        request.getResponseDispatcher().send(person);
    }
}