package com.itranswarp.exchange.messaging;

import java.util.List;

import com.itranswarp.exchange.message.AbstractMessage;


/**
 * 函数式接口，方法引用
 * 有且只能有一个抽象方法
 * @param <T>
 */
@FunctionalInterface
public interface BatchMessageHandler<T extends AbstractMessage> {

    void processMessages(List<T> messages);

}
