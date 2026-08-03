package com.routeshare.location.config;

import com.routeshare.location.service.RealtimeChannelService;
import java.security.Principal;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.*;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.*;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

@Configuration
@EnableWebSocketMessageBroker
public class LocationWebSocketConfig implements WebSocketMessageBrokerConfigurer {
  private final RealtimeChannelService channels;
  private final String[] allowedOrigins;

  public LocationWebSocketConfig(
      RealtimeChannelService channels,
      @Value("${routeshare.realtime.allowed-origin-patterns:http://localhost:*,http://127.0.0.1:*}")
          String allowedOriginPatterns) {
    this.channels = channels;
    this.allowedOrigins = allowedOriginPatterns.split(",");
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.enableSimpleBroker("/queue");
    registry.setApplicationDestinationPrefixes("/app");
    registry.setUserDestinationPrefix("/user");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry
        .addEndpoint("/api/v1/realtime/connect")
        .setAllowedOriginPatterns(allowedOrigins)
        .addInterceptors(tokenHandshake())
        .setHandshakeHandler(principalHandshake());
  }

  private HandshakeInterceptor tokenHandshake() {
    return new HandshakeInterceptor() {
      @Override
      public boolean beforeHandshake(
          ServerHttpRequest request,
          ServerHttpResponse response,
          WebSocketHandler handler,
          Map<String, Object> attributes) {
        String token =
            org.springframework.web.util.UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .getFirst("token");
        if (token == null || token.isBlank()) {
          response.setStatusCode(HttpStatus.UNAUTHORIZED);
          return false;
        }
        try {
          attributes.put("appUserId", channels.consumeToken(token));
          return true;
        } catch (RuntimeException ex) {
          response.setStatusCode(HttpStatus.UNAUTHORIZED);
          return false;
        }
      }

      @Override
      public void afterHandshake(
          ServerHttpRequest request,
          ServerHttpResponse response,
          WebSocketHandler handler,
          Exception exception) {}
    };
  }

  private DefaultHandshakeHandler principalHandshake() {
    return new DefaultHandshakeHandler() {
      @Override
      protected Principal determineUser(
          ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        return () -> String.valueOf(attributes.get("appUserId"));
      }
    };
  }
}
