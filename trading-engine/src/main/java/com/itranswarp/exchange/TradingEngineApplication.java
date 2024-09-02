package com.itranswarp.exchange;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// 核心服务，交易引擎
@SpringBootApplication
public class TradingEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(TradingEngineApplication.class, args);
    }
}
