package br.com.f2e.ovenplatform.media.infrastructure.aws;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class AwsMediaConfiguration {

  @Bean
  S3Client s3Client(AwsMediaProperties properties, DefaultCredentialsProvider credentialsProvider) {

    return S3Client.builder()
        .region(Region.of(properties.region()))
        .credentialsProvider(credentialsProvider)
        .httpClientBuilder(UrlConnectionHttpClient.builder())
        .build();
  }

  @Bean
  S3Presigner s3Presigner(
      AwsMediaProperties properties, DefaultCredentialsProvider credentialsProvider) {

    return S3Presigner.builder()
        .region(Region.of(properties.region()))
        .credentialsProvider(credentialsProvider)
        .build();
  }

  @Bean
  DefaultCredentialsProvider awsCredentialsProvider() {
    return DefaultCredentialsProvider.builder().build();
  }
}
