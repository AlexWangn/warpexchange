package com.itranswarp.exchange.match;

import java.math.BigDecimal;

import com.itranswarp.exchange.model.trade.OrderEntity;


/**
 * 1. record 是从Java16引入的一种新的用法
 * 2. record类似于Lombok库中的 @Data 注解，但是它是Java语言的一部分，不需要额外的库支持
 * 3. record 的主要用途是简化不可变数据类的定义，特别是当类中只有一些只读属性时。
 * 4. record 类中的所有成员变量都必须初始化，且不可更改，为final类型。
 */
public record MatchDetailRecord(BigDecimal price, BigDecimal quantity, OrderEntity takerOrder, OrderEntity makerOrder) {
}
