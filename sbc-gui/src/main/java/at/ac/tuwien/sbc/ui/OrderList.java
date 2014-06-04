package at.ac.tuwien.sbc.ui;

import at.ac.tuwien.sbc.model.Order;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

public class OrderList {

    private final ConcurrentMap<UUID, Order> orderMap;
    private final ConcurrentLinkedQueue<Order> orders;

    public OrderList() {
        this.orderMap = new ConcurrentHashMap<UUID, Order>();
        this.orders = new ConcurrentLinkedQueue<Order>();
    }

    public OrderList(List<Order> orders) {
        this.orderMap = new ConcurrentHashMap<UUID, Order>();
        this.orders = new ConcurrentLinkedQueue<Order>();
        addAll(orders);
    }

    public Order getOrder(UUID orderId) {
        return orderMap.get(orderId);
    }

    public List<Order> getList() {
        return new ArrayList<Order>(orders);
    }

    public void addAll(List<Order> orders) {
        for (Order order : orders) {
            this.orderMap.put(order.getId(), order);
            this.orders.add(order);
        }
    }

}
