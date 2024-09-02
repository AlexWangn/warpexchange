package com.itranswarp.exchange;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


// 定序服务，用于处理交易请求的顺序和优先级。
@SpringBootApplication
public class TradingSequencerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TradingSequencerApplication.class, args);
    }
}
