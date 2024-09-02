package com.itranswarp.exchange;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// 行情模块，将撮合输出的成交信息汇总，形成K线图
@SpringBootApplication
public class QuotationApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuotationApplication.class, args);
    }
}
