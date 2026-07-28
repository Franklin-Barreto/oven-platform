package br.com.f2e.ovenplatform.media.infrastructure;

import br.com.f2e.ovenplatform.media.application.MediaProperties;
import br.com.f2e.ovenplatform.media.infrastructure.aws.AwsMediaProperties;
import br.com.f2e.ovenplatform.media.infrastructure.delivery.MediaDeliveryProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
  MediaProperties.class,
  MediaDeliveryProperties.class,
  AwsMediaProperties.class
})
public class MediaConfiguration {}
