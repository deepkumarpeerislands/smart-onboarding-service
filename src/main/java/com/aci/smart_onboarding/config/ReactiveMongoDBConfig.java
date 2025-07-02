package com.aci.smart_onboarding.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.bson.UuidRepresentation;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.auditing.ReactiveIsNewAwareAuditingHandler;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.ReactiveMongoTransactionManager;
import org.springframework.data.mongodb.config.AbstractReactiveMongoConfiguration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.mapping.event.ReactiveAuditingEntityCallback;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.reactive.TransactionalOperator;

@Configuration
@EnableTransactionManagement
@EnableReactiveMongoRepositories(basePackages = "com.aci.smart_onboarding.repository")
@EnableMongoAuditing
@Slf4j
public class ReactiveMongoDBConfig extends AbstractReactiveMongoConfiguration {

  @Value("${spring.data.mongodb.database}")
  private String databaseName;

  @Value("${spring.data.mongodb.uri}")
  private String mongodbUri;

  @Value("${MONGO_SERVER_SELECTION_TIMEOUT_MS:30000}")
  private int serverSelectionTimeout;

  @Value("${MONGO_CONNECT_TIMEOUT_MS:15000}")
  private int connectTimeout;

  @Value("${MONGO_SOCKET_TIMEOUT_MS:30000}")
  private int socketTimeout;

  @Override
  protected String getDatabaseName() {
    return databaseName;
  }

  @Override
  public MongoClient reactiveMongoClient() {
    ConnectionString connectionString = new ConnectionString(mongodbUri);

    MongoClientSettings settings =
        MongoClientSettings.builder()
            .applyConnectionString(connectionString)
            .uuidRepresentation(UuidRepresentation.STANDARD)
            .applyToConnectionPoolSettings(
                builder ->
                    builder.maxWaitTime(30000, TimeUnit.MILLISECONDS).maxSize(100).minSize(5))
            .applyToSocketSettings(
                builder ->
                    builder
                        .connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                        .readTimeout(socketTimeout, TimeUnit.MILLISECONDS))
            .applyToClusterSettings(
                builder ->
                    builder.serverSelectionTimeout(serverSelectionTimeout, TimeUnit.MILLISECONDS))
            .build();

    return MongoClients.create(settings);
  }

  @Bean
  @Override
  public MongoCustomConversions customConversions() {
    List<Converter<?, ?>> converters = new ArrayList<>();

    // Add converters
    converters.add(DATE_TO_LOCAL_DATE_TIME_CONVERTER);
    converters.add(LOCAL_DATE_TIME_TO_DATE_CONVERTER);
    converters.add(DATE_TO_LOCAL_DATE_CONVERTER);
    converters.add(LOCAL_DATE_TO_DATE_CONVERTER);

    return new MongoCustomConversions(converters);
  }

  // Static final converter instances
  private static final Converter<Date, LocalDateTime> DATE_TO_LOCAL_DATE_TIME_CONVERTER =
      new DateToLocalDateTimeConverter();
  private static final Converter<LocalDateTime, Date> LOCAL_DATE_TIME_TO_DATE_CONVERTER =
      new LocalDateTimeToDateConverter();
  private static final Converter<Date, LocalDate> DATE_TO_LOCAL_DATE_CONVERTER =
      new DateToLocalDateConverter();
  private static final Converter<LocalDate, Date> LOCAL_DATE_TO_DATE_CONVERTER =
      new LocalDateToDateConverter();

  // Converter implementations as static classes
  private static class DateToLocalDateTimeConverter implements Converter<Date, LocalDateTime> {
    @Override
    public LocalDateTime convert(Date source) {
      return source == null
          ? null
          : LocalDateTime.ofInstant(source.toInstant(), ZoneId.systemDefault());
    }
  }

  private static class LocalDateTimeToDateConverter implements Converter<LocalDateTime, Date> {
    @Override
    public Date convert(LocalDateTime source) {
      return source == null ? null : Date.from(source.atZone(ZoneId.systemDefault()).toInstant());
    }
  }

  private static class DateToLocalDateConverter implements Converter<Date, LocalDate> {
    @Override
    public LocalDate convert(Date source) {
      return source == null
          ? null
          : source.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
  }

  private static class LocalDateToDateConverter implements Converter<LocalDate, Date> {
    @Override
    public Date convert(LocalDate source) {
      return source == null
          ? null
          : Date.from(source.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
  }

  @Bean
  public ReactiveMongoTemplate reactiveMongoTemplate() {
    return new ReactiveMongoTemplate(reactiveMongoClient(), getDatabaseName());
  }

  @Bean
  public ReactiveTransactionManager reactiveTransactionManager(
      ReactiveMongoDatabaseFactory databaseFactory) {
    return new ReactiveMongoTransactionManager(databaseFactory);
  }

  @Bean
  public TransactionalOperator transactionalOperator(
      ReactiveTransactionManager transactionManager) {
    return TransactionalOperator.create(transactionManager);
  }

  @Bean
  public ReactiveAuditingEntityCallback reactiveAuditingEntityCallback(
      MongoMappingContext mappingContext) {
    ObjectFactory<ReactiveIsNewAwareAuditingHandler> auditingHandlerFactory =
        () -> new ReactiveIsNewAwareAuditingHandler(PersistentEntities.of(mappingContext));

    return new ReactiveAuditingEntityCallback(auditingHandlerFactory);
  }
}
