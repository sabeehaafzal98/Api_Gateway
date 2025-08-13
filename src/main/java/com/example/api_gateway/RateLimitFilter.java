package com.example.api_gateway;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.sql.Ref;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends AbstractGatewayFilterFactory<RateLimitFilter.Config> {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    public RateLimitFilter(){
        super(Config.class);

    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String clientIp = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
            Bucket bucket= cache.computeIfAbsent(clientIp,this::newBucket);
            if(bucket.tryConsume(1)){
                return chain.filter(exchange);
            }else {
                exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                return exchange.getResponse().setComplete();
            }
        };
    }

    private Bucket newBucket(String s) {
        //5 request per minute
        Refill refill= Refill.intervally(5, Duration.ofMinutes(1));

        Bandwidth limit = Bandwidth.classic(5,refill);
        return Bucket4j.builder().addLimit(limit).build();
    }


    public static class Config{

    }
}
