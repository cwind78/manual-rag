package com.prj.manualrag.common.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {

        ConnectionProvider provider =
                ConnectionProvider.builder("manual-rag-pool")
                        .maxConnections(100)
                        .pendingAcquireMaxCount(500)
                        .pendingAcquireTimeout(Duration.ofSeconds(30))
                        .maxIdleTime(Duration.ofSeconds(30))
                        .maxLifeTime(Duration.ofMinutes(5))
                        .build();

        HttpClient httpClient =
                HttpClient.create(provider)
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                        .responseTimeout(Duration.ofSeconds(60))
                        .doOnConnected(connection ->
                                connection
                                        .addHandlerLast(
                                                new ReadTimeoutHandler(60, TimeUnit.SECONDS))
                                        .addHandlerLast(
                                                new WriteTimeoutHandler(60, TimeUnit.SECONDS)));

        return WebClient.builder()
                .defaultHeader(HttpHeaders.USER_AGENT,
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .defaultHeader(HttpHeaders.ACCEPT,
                        "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE,
                        "ko-KR,ko;q=0.9,en-US;q=0.8")
                .defaultHeader(HttpHeaders.REFERER,
                        "http://localhost:8081/")
                .clientConnector(
                        new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
