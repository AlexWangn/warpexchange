package com.itranswarp.exchange.order;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.itranswarp.exchange.assets.AssetService;
import com.itranswarp.exchange.enums.AssetEnum;
import com.itranswarp.exchange.enums.Direction;
import com.itranswarp.exchange.model.trade.OrderEntity;

// 保持单例模式，为了在一台机器上进行唯一的定序操作
@Component
public class OrderService {

    final AssetService assetService;

    // 构造器引入，单例模式
    public OrderService(@Autowired AssetService assetService) {
        this.assetService = assetService;
    }

    // 跟踪所有活动订单: Order ID => OrderEntity
    final ConcurrentMap<Long, OrderEntity> activeOrders = new ConcurrentHashMap<>();

    // 跟踪用户活动订单: User ID => Map(Order ID => OrderEntity)，一个用户id可以有多个下单，但一个订单id只能有一个下单
    final ConcurrentMap<Long, ConcurrentMap<Long, OrderEntity>> userOrders = new ConcurrentHashMap<>();

    /**
     * 创建订单，失败返回null:
     * @param sequenceId：定序id，相同价格的订单根据定序ID进行排序
     * @param ts：时间戳，timestamp
     * @param orderId：订单ID
     * @param userId：订单关联的用户id
     * @param direction：订单方向；买或卖
     * @param price：订单价格
     * @param quantity：订单数量
     * @return
     */
    public OrderEntity createOrder(long sequenceId, long ts, Long orderId, Long userId, Direction direction,
            BigDecimal price, BigDecimal quantity) {
        switch (direction) {
        case BUY -> {
            // 买入，需冻结USD：
            if (!assetService.tryFreeze(userId, AssetEnum.USD, price.multiply(quantity))) {
                return null;
            }
        }
        case SELL -> {
            // 卖出，需冻结BTC：
            if (!assetService.tryFreeze(userId, AssetEnum.BTC, quantity)) {
                return null;
            }
        }
        default -> throw new IllegalArgumentException("Invalid direction.");
        }
        // 实例化Order
        OrderEntity order = new OrderEntity();
        order.id = orderId;
        order.sequenceId = sequenceId;
        order.userId = userId;
        order.direction = direction;
        order.price = price;
        order.quantity = quantity;
        order.unfilledQuantity = quantity;
        order.createdAt = order.updatedAt = ts;
        // 添加到ActiveOrders:
        this.activeOrders.put(order.id, order);
        // 添加到UserOrders:
        ConcurrentMap<Long, OrderEntity> uOrders = this.userOrders.get(userId);
        if (uOrders == null) {
            uOrders = new ConcurrentHashMap<>();
            this.userOrders.put(userId, uOrders);
        }
        uOrders.put(order.id, order);
        return order;
    }

    public ConcurrentMap<Long, OrderEntity> getActiveOrders() {
        return this.activeOrders;
    }

    public OrderEntity getOrder(Long orderId) {
        return this.activeOrders.get(orderId);
    }

    public ConcurrentMap<Long, OrderEntity> getUserOrders(Long userId) {
        return this.userOrders.get(userId);
    }

    // 删除活动订单:
    public void removeOrder(Long orderId) {
        // 从ActiveOrders中删除:
        OrderEntity removed = this.activeOrders.remove(orderId);
        if (removed == null) {
            throw new IllegalArgumentException("Order not found by orderId in active orders: " + orderId);
        }
        // 从UserOrders中删除:
        ConcurrentMap<Long, OrderEntity> uOrders = userOrders.get(removed.userId);
        if (uOrders == null) {
            throw new IllegalArgumentException("User orders not found by userId: " + removed.userId);
        }
        if (uOrders.remove(orderId) == null) {
            throw new IllegalArgumentException("Order not found by orderId in user orders: " + orderId);
        }
    }

    public void debug() {
        System.out.println("---------- orders ----------");
        List<OrderEntity> orders = new ArrayList<>(this.activeOrders.values());
        Collections.sort(orders);
        for (OrderEntity order : orders) {
            System.out.println("  " + order.id + " " + order.direction + " price: " + order.price + " unfilled: "
                    + order.unfilledQuantity + " quantity: " + order.quantity + " sequenceId: " + order.sequenceId
                    + " userId: " + order.userId);
        }
        System.out.println("---------- // orders ----------");
    }
}
