package com.aci.smart_onboarding.model;

import java.time.Instant;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "vectors")
@Data
public class Artifact {

  @Id private String id;

  @Field("collection_id")
  private String collectionId;

  @Field("document_id")
  private String documentId;

  @Field("chunk_id")
  private String chunkId;

  @Field("chunk_index")
  private int chunkIndex;

  @Field("document_name")
  private String documentName;

  private String text;
  private double[] vector;
  private Metadata metadata;
  private Instant createdAt;

  @Data
  public static class Metadata {
    @Field("original_size")
    private int originalSize;

    @Field("content_encoding")
    private String contentEncoding;

    @Field("processed_with")
    private String processedWith;

    @Field("chunk_index")
    private int chunkIndex;

    @Field("total_chunks")
    private int totalChunks;

    @Field("chunking_enabled")
    private boolean chunkingEnabled;

    @Field("chunking_method")
    private String chunkingMethod;
  }
}
