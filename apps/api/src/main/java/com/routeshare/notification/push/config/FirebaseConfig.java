package com.routeshare.notification.push.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import java.io.FileInputStream;
import java.io.IOException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Initializes the Firebase Admin SDK only when push is enabled, loading the service-account JSON
 * from {@code routeshare.push.service-account-path}. Kept separate from {@link
 * PushPropertiesConfig} so the properties bean always exists even when Firebase is off.
 */
@Configuration
@ConditionalOnProperty(prefix = "routeshare.push", name = "enabled", havingValue = "true")
public class FirebaseConfig {

  @Bean
  FirebaseApp firebaseApp(PushProperties props) throws IOException {
    if (!props.ready()) {
      throw new IllegalStateException(
          "Push is enabled but routeshare.push.service-account-path is not set");
    }
    if (!FirebaseApp.getApps().isEmpty()) {
      return FirebaseApp.getInstance();
    }
    try (var in = new FileInputStream(props.serviceAccountPath())) {
      var builder = FirebaseOptions.builder().setCredentials(GoogleCredentials.fromStream(in));
      if (props.projectId() != null && !props.projectId().isBlank()) {
        builder.setProjectId(props.projectId());
      }
      return FirebaseApp.initializeApp(builder.build());
    }
  }

  @Bean
  FirebaseMessaging firebaseMessaging(FirebaseApp app) {
    return FirebaseMessaging.getInstance(app);
  }
}
