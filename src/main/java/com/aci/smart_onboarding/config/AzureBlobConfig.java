package com.aci.smart_onboarding.config;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureBlobConfig {

  private final AzureBlobProperties azureBlobProperties;

  public AzureBlobConfig(AzureBlobProperties azureBlobProperties) {
    this.azureBlobProperties = azureBlobProperties;
  }

  @Bean
  public BlobServiceClient blobServiceClient() {
    return new BlobServiceClientBuilder()
        .connectionString(azureBlobProperties.getConnectionString())
        .buildClient();
  }

  @Bean
  public BlobContainerClient blobContainerClient(BlobServiceClient blobServiceClient) {
    return blobServiceClient.getBlobContainerClient(azureBlobProperties.getContainerName());
  }
}
