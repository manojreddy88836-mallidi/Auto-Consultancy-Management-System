package com.autoconsultancy.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.connection.ConnectionPoolSettings;
import com.mongodb.connection.ServerSettings;
import com.mongodb.connection.SocketSettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.MongoClientFactoryBean;

import java.util.concurrent.TimeUnit;

/**
 * MongoConfig — configures MongoDB client with optimized connection pooling
 * for Atlas (cloud) + Render (cloud hosting) environments.
 *
 * Key optimizations:
 * - Short connect timeout (15s) so startup fails fast if Atlas is unreachable
 * - Generous socket timeout (30s) for slow Atlas free-tier responses
 * - Small connection pool (max 10) — Render free tier has limited CPU/RAM
 * - Heartbeat tuning to avoid excessive ping traffic on Atlas free tier
 */
@Configuration
@EnableMongoAuditing
public class MongoConfig {

    @Value("${spring.data.mongodb.uri:mongodb://localhost:27017/shop_management}")
    private String mongoUri;

    @Bean
    public MongoClientFactoryBean mongoClient() {
        MongoClientFactoryBean factory = new MongoClientFactoryBean();

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(mongoUri))
                .applyToConnectionPoolSettings(pool -> pool
                        .maxSize(10)                       // max connections in pool
                        .minSize(2)                        // keep 2 warm connections ready
                        .maxConnectionIdleTime(60, TimeUnit.SECONDS)  // close idle connections after 60s
                        .maxConnectionLifeTime(300, TimeUnit.SECONDS) // recycle connections every 5 min
                        .maxWaitTime(15, TimeUnit.SECONDS)            // wait max 15s for a free connection
                )
                .applyToSocketSettings(socket -> socket
                        .connectTimeout(15, TimeUnit.SECONDS)  // fail fast if Atlas unreachable
                        .readTimeout(30, TimeUnit.SECONDS)     // allow 30s for slow Atlas reads
                )
                .applyToServerSettings(server -> server
                        .heartbeatFrequency(20, TimeUnit.SECONDS)     // ping Atlas every 20s (not every 10s)
                        .minHeartbeatFrequency(5, TimeUnit.SECONDS)   // minimum 5s between pings
                )
                .applyToClusterSettings(cluster -> cluster
                        .serverSelectionTimeout(30, TimeUnit.SECONDS) // give Atlas 30s to respond on cold start
                )
                .build();

        factory.setMongoClientSettings(settings);
        return factory;
    }
}
