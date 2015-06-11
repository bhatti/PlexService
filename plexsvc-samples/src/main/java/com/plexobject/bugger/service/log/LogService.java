package com.plexobject.bugger.service.log;

import org.apache.log4j.Logger;

import com.plexobject.encode.CodecType;
import com.plexobject.handler.Request;
import com.plexobject.handler.RequestHandler;
import com.plexobject.service.Method;
import com.plexobject.service.Protocol;
import com.plexobject.service.ServiceConfig;

// @ServiceConfig(protocol = Protocol.HTTP, 
// endpoint = "/logs", method = Method.POST,
// contentType = "application/json")
@ServiceConfig(protocol = Protocol.JMS, endpoint = "queue://{scope}-log-service-queue", method = Method.MESSAGE, codec = CodecType.JSON)
public class LogService implements RequestHandler {
    private static final Logger log = Logger.getLogger(LogService.class);

    @Override
    public void handle(Request<Object> request) {
        log.info(request.toString());
    }

}
