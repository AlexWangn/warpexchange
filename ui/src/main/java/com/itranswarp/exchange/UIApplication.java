package com.itranswarp.exchange;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itranswarp.exchange.client.RestClient;

// 给交易员提供一个Web操作界面，并把交易员的操作转发给后端API。
// UI相当于一个单独的服务，因此请求tradeapi时，也需要经过http请求，它本身不是交易入口，它通过转发JavaScript请求到真正的API接口。
// 因此这里要做一层代理。

@SpringBootApplication
public class UIApplication {

    public static void main(String[] args) {
        SpringApplication.run(UIApplication.class, args);
    }

    @Bean
    public RestClient createRestClient(
            @Value("#{exchangeConfiguration.apiEndpoints.tradingApi}") String tradingApiEndpoint,
            @Autowired ObjectMapper objectMapper) {
        return new RestClient.Builder(tradingApiEndpoint).build(objectMapper);
    }

    @Bean
    public WebMvcConfigurer webMvcConfigurer() {
        return new WebMvcConfigurer() {
            /**
             * Keep "/static/" prefix
             */
            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/");
            }
        };
    }
}
