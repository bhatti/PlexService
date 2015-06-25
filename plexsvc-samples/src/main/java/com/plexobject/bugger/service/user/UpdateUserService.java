package com.plexobject.bugger.service.user;

import com.plexobject.bugger.model.User;
import com.plexobject.bugger.repository.UserRepository;
import com.plexobject.encode.CodecType;
import com.plexobject.handler.Request;
import com.plexobject.handler.RequestHandler;
import com.plexobject.service.Protocol;
import com.plexobject.service.RequestMethod;
import com.plexobject.service.ServiceConfig;
import com.plexobject.validation.Field;
import com.plexobject.validation.RequiredFields;

@ServiceConfig(protocol = Protocol.JMS, payloadClass = User.class, rolesAllowed = "Administrator", endpoint = "queue://{scope}-update-user-service-queue", method = RequestMethod.MESSAGE, codec = CodecType.JSON)
@RequiredFields({ @Field(name = "id") })
public class UpdateUserService extends AbstractUserService implements
        RequestHandler {
    public UpdateUserService(UserRepository userRepository) {
        super(userRepository);
    }

    @Override
    public void handle(Request<Object> request) {
        User user = request.getPayload();

        User saved = userRepository.save(user);
        request.getResponse().setPayload(saved);
    }
}
