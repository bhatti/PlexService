package com.plexobject.basic;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.plexobject.encode.CodecType;
import com.plexobject.handler.Request;
import com.plexobject.handler.RequestHandler;
import com.plexobject.service.Method;
import com.plexobject.service.Protocol;
import com.plexobject.service.ServiceConfig;


@ServiceConfig(protocol = Protocol.HTTP, endpoint = "/array", method = Method.GET, codec = CodecType.JSON)
public class ArrayService implements RequestHandler {
    private static final Logger log = LoggerFactory
            .getLogger(ReverseService.class);

    public ArrayService() {
        log.info("Array Service Started");
    }

    @Override
    public void handle(Request request) {
        Integer count = request.getIntegerProperty("count");
        if (count == null) {
            count = 1;
        }
        List<Map<String, Object>> response = new ArrayList<>();
        for (int i=0; i<count; i++) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("date", new Date());
            entry.put("id", i+1);
            response.add(entry);
        }
        request.getResponseDispatcher().send(response);
    }
}
