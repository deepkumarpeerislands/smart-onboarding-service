package com.aci.smart_onboarding.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.protocol.ProtocolVersion;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@Slf4j
public class RedisConfig {

  @Value("${spring.redis.host:localhost}")
  private String redisHost;

  @Value("${spring.redis.port:6379}")
  private int redisPort;

  @Value("${spring.redis.password:}")
  private String redisPassword;

  @Value("${spring.redis.ssl:false}")
  private boolean sslEnabled;

  @Value("${spring.redis.timeout:10000}")
  private int timeout;

  @Value("${spring.redis.connect-timeout:10000}")
  private int connectTimeout;

  @Bean(name = "customReactiveRedisConnectionFactory")
  @Primary
  public ReactiveRedisConnectionFactory customReactiveRedisConnectionFactory() {
    RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
    config.setHostName(redisHost);
    config.setPort(redisPort);

    if (redisPassword != null && !redisPassword.isEmpty()) {
      config.setPassword(redisPassword);
    }

    // Configure Lettuce client for Azure Redis Cache
    ClientOptions clientOptions =
        ClientOptions.builder()
            .protocolVersion(ProtocolVersion.RESP2)
            .socketOptions(
                SocketOptions.builder()
                    .connectTimeout(Duration.ofMillis(connectTimeout))
                    .keepAlive(true)
                    .build())
            .timeoutOptions(io.lettuce.core.TimeoutOptions.enabled(Duration.ofMillis(timeout)))
            .build();

    LettuceClientConfiguration.LettuceClientConfigurationBuilder builder =
        LettuceClientConfiguration.builder()
            .clientOptions(clientOptions)
            .commandTimeout(Duration.ofMillis(timeout))
            .shutdownTimeout(Duration.ofMillis(100));

    if (sslEnabled) {
      builder.useSsl(); // ✅ Enable SSL for Azure Redis
    }

    LettuceClientConfiguration clientConfig = builder.build();

    return new LettuceConnectionFactory(config, clientConfig);
  }

  @Bean
  @Primary
  public ReactiveRedisTemplate<String, String> customReactiveRedisTemplate(
      ReactiveRedisConnectionFactory customReactiveRedisConnectionFactory) {

    StringRedisSerializer serializer = new StringRedisSerializer();

    RedisSerializationContext<String, String> serializationContext =
        RedisSerializationContext.<String, String>newSerializationContext()
            .key(serializer)
            .value(serializer)
            .hashKey(serializer)
            .hashValue(serializer)
            .build();

    return new ReactiveRedisTemplate<>(customReactiveRedisConnectionFactory, serializationContext);
  }

  @Bean
  public CommandLineRunner redisConnectionTest(
      ReactiveRedisTemplate<String, String> redisTemplate) {
    return args -> {
      log.info("🔍 Testing Redis connection...");
      log.info("📍 Redis Host: {}", redisHost);
      log.info("🔌 Redis Port: {}", redisPort);
      log.info("🔒 SSL Enabled: {}", sslEnabled);

      redisTemplate
          .opsForValue()
          .set("connection-test", "success")
          .then(redisTemplate.opsForValue().get("connection-test"))
          .doOnSuccess(
              result -> {
                log.info("✅ Redis connection successful!");
                log.info("📊 Test result: {}", result);
              })
          .doOnError(
              error -> {
                log.error("❌ Redis connection failed! Error: {}", error.getMessage());
                log.error(
                    "Failed to connect to Redis at {}:{} with SSL {}",
                    redisHost,
                    redisPort,
                    sslEnabled);
                log.error("Please check:");
                log.error("1. Azure Redis Cache is running and accessible");
                log.error("2. Network connectivity to {}:{}", redisHost, redisPort);
                log.error("3. SSL is enabled for this connection");
                log.error("4. Access key is correct");
              })
          .subscribe();
    };
  }
}
