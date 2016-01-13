package com.plexobject.handler.ws.performance;

import java.util.Collection;

import javax.jws.WebService;

@WebService
public interface OrderService {
    Order create(Order order);

    Order get(long orderId);

    Collection<Order> findByAccounts(long accountId);
}
