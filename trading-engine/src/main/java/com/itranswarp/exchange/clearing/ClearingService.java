package com.itranswarp.exchange.clearing;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.itranswarp.exchange.assets.AssetService;
import com.itranswarp.exchange.assets.Transfer;
import com.itranswarp.exchange.enums.AssetEnum;
import com.itranswarp.exchange.match.MatchDetailRecord;
import com.itranswarp.exchange.match.MatchResult;
import com.itranswarp.exchange.model.trade.OrderEntity;
import com.itranswarp.exchange.order.OrderService;
import com.itranswarp.exchange.support.LoggerSupport;

/**
 * 清算系统，处理撮合结果，涉及到账户资金变动。
 */
@Component
public class ClearingService extends LoggerSupport {

    final AssetService assetService;

    final OrderService orderService;

    public ClearingService(@Autowired AssetService assetService, @Autowired OrderService orderService) {
        this.assetService = assetService;
        this.orderService = orderService;
    }

    /**
     * 两种情况：1. 如果是买入的话，如果你买入价格比挂单高，以挂单价格成交，也即以低价格成交。
     *         2. 如果是卖出的话，如果你卖出价格比挂单低，以挂单价格成交，也即以挂单高价格成交。
     * 两种情况都以对手盘为为准。
     * @param result
     */
    public void clearMatchResult(MatchResult result) {
        OrderEntity taker = result.takerOrder;
        switch (taker.direction) {
        case BUY -> {
            // 买入时，按Maker的价格成交：
            for (MatchDetailRecord detail : result.matchDetails) {
                if (logger.isDebugEnabled()) {
                    logger.debug(
                            "clear buy matched detail: price = {}, quantity = {}, takerOrderId = {}, makerOrderId = {}, takerUserId = {}, makerUserId = {}",
                            detail.price(), detail.quantity(), detail.takerOrder().id, detail.makerOrder().id,
                            detail.takerOrder().userId, detail.makerOrder().userId);
                }
                OrderEntity maker = detail.makerOrder();
                BigDecimal matched = detail.quantity();
                if (taker.price.compareTo(maker.price) > 0) {
                    // 实际买入价比报价低，部分USD退回账户:
                    BigDecimal unfreezeQuote = taker.price.subtract(maker.price).multiply(matched);
                    logger.debug("unfree extra unused quote {} back to taker user {}", unfreezeQuote, taker.userId);
                    assetService.unfreeze(taker.userId, AssetEnum.USD, unfreezeQuote);
                }
                // 买方USD转入卖方账户:
                assetService.transfer(Transfer.FROZEN_TO_AVAILABLE, taker.userId, maker.userId, AssetEnum.USD,
                        maker.price.multiply(matched));
                // 卖方BTC转入买方账户:
                assetService.transfer(Transfer.FROZEN_TO_AVAILABLE, maker.userId, taker.userId, AssetEnum.BTC, matched);
                // 删除完全成交的Maker:
                if (maker.unfilledQuantity.signum() == 0) {
                    orderService.removeOrder(maker.id);
                }
            }
            // 删除完全成交的Taker:
            if (taker.unfilledQuantity.signum() == 0) {
                orderService.removeOrder(taker.id);
            }
        }
        case SELL -> {
            for (MatchDetailRecord detail : result.matchDetails) {
                if (logger.isDebugEnabled()) {
                    logger.debug(
                            "clear sell matched detail: price = {}, quantity = {}, takerOrderId = {}, makerOrderId = {}, takerUserId = {}, makerUserId = {}",
                            detail.price(), detail.quantity(), detail.takerOrder().id, detail.makerOrder().id,
                            detail.takerOrder().userId, detail.makerOrder().userId);
                }
                // maker挂单，taker吃单
                OrderEntity maker = detail.makerOrder();
                BigDecimal matched = detail.quantity();
                // 卖方BTC转入买方账户:
                assetService.transfer(Transfer.FROZEN_TO_AVAILABLE, taker.userId, maker.userId, AssetEnum.BTC, matched);
                // 买方USD转入卖方账户:
                assetService.transfer(Transfer.FROZEN_TO_AVAILABLE, maker.userId, taker.userId, AssetEnum.USD,
                        maker.price.multiply(matched));
                // 删除完全成交的Maker:
                if (maker.unfilledQuantity.signum() == 0) {
                    orderService.removeOrder(maker.id);
                }
            }
            // 删除完全成交的Taker:
            if (taker.unfilledQuantity.signum() == 0) {
                orderService.removeOrder(taker.id);
            }
        }
        default -> throw new IllegalArgumentException("Invalid direction.");
        }
    }

    public void clearCancelOrder(OrderEntity order) {
        switch (order.direction) {
        case BUY -> {
            // 解冻USD = 价格 x 未成交数量
            assetService.unfreeze(order.userId, AssetEnum.USD, order.price.multiply(order.unfilledQuantity));
        }
        case SELL -> {
            // 解冻BTC = 未成交数量
            assetService.unfreeze(order.userId, AssetEnum.BTC, order.unfilledQuantity);
        }
        default -> throw new IllegalArgumentException("Invalid direction.");
        }
        // 从OrderService中删除订单:
        orderService.removeOrder(order.id);
    }
}
