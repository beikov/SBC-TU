package at.ac.tuwien.sbc.ui;

import at.ac.tuwien.sbc.model.Order;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class OrderList {

    Map<UUID, Order> orders;

    public OrderList() {
        orders = new LinkedHashMap<UUID, Order>();
    }

    public OrderList(List<Order> orders) {
        this.orders = new HashMap<UUID, Order>();
        addAll(orders);
    }

    public Order getOrder(UUID orderId) {
        return orders.get(orderId);
    }

    public List<Order> getList() {
        return new ArrayList<Order>(orders.values());
    }

    public void addAll(List<Order> orders) {
        for (Order order : orders) {
            this.orders.put(order.getId(), order);
        }
    }

}
