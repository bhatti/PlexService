package com.plexobject.bugger.service.user;

import com.plexobject.bugger.model.User;
import com.plexobject.bugger.repository.UserRepository;
import com.plexobject.domain.ValidationException;
import com.plexobject.encode.CodecType;
import com.plexobject.handler.AbstractResponseDelegate;
import com.plexobject.handler.Request;
import com.plexobject.handler.RequestHandler;
import com.plexobject.security.AuthException;
import com.plexobject.service.ServiceConfig;
import com.plexobject.service.ServiceConfig.GatewayType;
import com.plexobject.service.ServiceConfig.Method;
import com.plexobject.service.http.HttpResponse;

//@ServiceConfig(gateway = GatewayType.HTTP, requestClass = Void.class, endpoint = "/login", method = Method.POST, codec = CodecType.JSON)
@ServiceConfig(gateway = GatewayType.JMS, requestClass = Void.class, endpoint = "queue:{scope}-login-service-queue", method = Method.MESSAGE, codec = CodecType.JSON)
public class LoginService extends AbstractUserService implements RequestHandler {
    public LoginService(UserRepository userRepository) {
        super(userRepository);
    }

    @Override
    public void handle(Request request) {
        String username = request.getStringProperty("username");
        String password = request.getStringProperty("password");

        ValidationException
                .builder()
                .assertNonNull(username, "undefined_username", "username",
                        "username not specified")
                .assertNonNull(password, "undefined_password", "password",
                        "password not specified").end();

        User user = userRepository.authenticate(username, password);
        AbstractResponseDelegate responseBuilder = request.getResponseBuilder();
        if (user == null) {
            throw new AuthException(HttpResponse.SC_UNAUTHORIZED,
                    request.getSessionId(), request.getRemoteAddress(),
                    "failed to authenticate");
        } else {
            responseBuilder.addSessionId(userRepository.getSessionId(user));
            responseBuilder.send(user);
        }
    }
}
