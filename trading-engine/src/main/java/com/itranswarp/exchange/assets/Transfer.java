package com.itranswarp.exchange.assets;

public enum Transfer {

    // 可用转可用，例如：充值，提现
    AVAILABLE_TO_AVAILABLE,

    // 可用转冻结，例如：下单
    AVAILABLE_TO_FROZEN,

    // 冻结转可用，例如：取消订单
    FROZEN_TO_AVAILABLE;
}
