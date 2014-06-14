package at.ac.tuwien.sbc.ui;

import at.ac.tuwien.sbc.model.Order;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

/**
 * A container for orders that also groups orders by their id.
 */
public class OrderList {

    private final ConcurrentMap<UUID, Order> orderMap;
    private final ConcurrentLinkedQueue<Order> orders;

    public OrderList() {
        this.orderMap = new ConcurrentHashMap<UUID, Order>();
        this.orders = new ConcurrentLinkedQueue<Order>();
    }

    public OrderList(List<Order> orders) {
        int size = orders.size();
        this.orderMap = new ConcurrentHashMap<UUID, Order>(size);
        this.orders = new ConcurrentLinkedQueue<Order>();

        for (int i = 0; i < size; i++) {
            Order order = orders.get(i);
            this.orderMap.put(order.getId(), order);
            this.orders.add(order);
        }
    }

    public Order getOrder(UUID orderId) {
        return orderMap.get(orderId);
    }

    public List<Order> getList() {
        return new ArrayList<Order>(orders);
    }

    public void addAll(Order order) {
        this.orderMap.put(order.getId(), order);
        this.orders.add(order);
    }

}
