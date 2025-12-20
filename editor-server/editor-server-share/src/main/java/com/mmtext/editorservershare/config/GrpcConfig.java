package com.mmtext.editorservershare.config;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Configuration for gRPC clients
 */
@Configuration
public class GrpcConfig {

    private static final Logger log = LoggerFactory.getLogger(GrpcConfig.class);

    @Bean
    public ManagedChannel authServiceChannel(
            @Value("${grpc.auth.service.host:auth-server}") String host,
            @Value("${grpc.auth.service.port:9090}") int port) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .keepAliveTime(30, TimeUnit.SECONDS)
                .keepAliveTimeout(5, TimeUnit.SECONDS)
                .keepAliveWithoutCalls(true)
                .maxInboundMessageSize(10 * 1024 * 1024) // 10MB
                .build();

        log.info("Created gRPC channel for Auth Service at {}:{}", host, port);
        return channel;
    }

    @Bean
    public ManagedChannel editorServiceChannel(
            @Value("${grpc.editor.service.host:editor-server-snapshot-1}") String host,
            @Value("${grpc.editor.service.port:9091}") int port) {

        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .keepAliveTime(30, TimeUnit.SECONDS)
                .keepAliveTimeout(5, TimeUnit.SECONDS)
                .keepAliveWithoutCalls(true)
                .maxInboundMessageSize(10 * 1024 * 1024) // 10MB
                .build();

        log.info("Created gRPC channel for Editor Service at {}:{}", host, port);
        return channel;
    }
}