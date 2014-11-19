package com.plexobject.bugger.service.user;

import com.plexobject.bugger.model.User;
import com.plexobject.bugger.repository.UserRepository;
import com.plexobject.encode.CodecType;
import com.plexobject.handler.AbstractResponseDispatcher;
import com.plexobject.handler.Request;
import com.plexobject.handler.RequestHandler;
import com.plexobject.security.AuthException;
import com.plexobject.service.Method;
import com.plexobject.service.Protocol;
import com.plexobject.service.ServiceConfig;
import com.plexobject.validation.Field;
import com.plexobject.validation.RequiredFields;

//@ServiceConfig(protocol = Protocol.HTTP, endpoint = "/login", method = Method.POST, codec = CodecType.JSON)
@ServiceConfig(protocol = Protocol.JMS, endpoint = "queue:{scope}-login-service-queue", method = Method.MESSAGE, codec = CodecType.JSON)
@RequiredFields({ @Field(name = "username"),
        @Field(name = "password") })
public class LoginService extends AbstractUserService implements RequestHandler {
    public LoginService(UserRepository userRepository) {
        super(userRepository);
    }

    @Override
    public void handle(Request request) {
        log.info("PAYLOAD " + request.getPayload());
        String username = request.getStringProperty("username");
        String password = request.getStringProperty("password");

        User user = userRepository.authenticate(username, password);
        AbstractResponseDispatcher responseBuilder = request
                .getResponseDispatcher();
        if (user == null) {
            throw new AuthException(request.getSessionId(),
                    "failed to authenticate");
        } else {
            responseBuilder.addSessionId(userRepository.getSessionId(user));
            responseBuilder.send(user);
        }
    }
}
