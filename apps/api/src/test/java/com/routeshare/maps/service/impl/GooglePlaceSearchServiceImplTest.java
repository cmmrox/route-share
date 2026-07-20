package com.routeshare.maps.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.routeshare.common.cache.InMemoryJsonCache;
import com.routeshare.maps.config.GoogleMapsProperties;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

class GooglePlaceSearchServiceImplTest {
  private static final String DETAILS_OK =
      """
      {"id":"place-1","formattedAddress":"Colombo Fort Railway Station, Colombo",
       "location":{"latitude":6.9337,"longitude":79.8500}}
      """;
  private static final String AUTOCOMPLETE_OK =
      """
      {"suggestions":[{"placePrediction":{"placeId":"place-1",
        "text":{"text":"Colombo Fort Railway Station"},
        "structuredFormat":{"mainText":{"text":"Colombo Fort"},
          "secondaryText":{"text":"Colombo"}}}}]}
      """;

  @Test
  @SuppressWarnings("unchecked")
  void detailsUsesEssentialsFieldMaskAndSessionToken() throws Exception {
    var httpClient = mock(HttpClient.class);
    HttpResponse<String> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(200);
    when(response.body()).thenReturn(DETAILS_OK);
    when(httpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(response);
    var service =
        new GooglePlaceSearchServiceImpl(
            new GoogleMapsProperties(true, "test-key"),
            new ObjectMapper(),
            httpClient,
            new InMemoryJsonCache());

    var details = service.details("place-1", "session-token-1");

    var captor = ArgumentCaptor.forClass(HttpRequest.class);
    verify(httpClient).send(captor.capture(), any(HttpResponse.BodyHandler.class));
    HttpRequest request = captor.getValue();
    // Essentials-tier fields only: displayName is Pro-tier and would triple the SKU price.
    assertThat(request.headers().firstValue("X-Goog-FieldMask"))
        .hasValue("id,formattedAddress,location");
    assertThat(request.uri().getQuery()).contains("sessionToken=session-token-1");
    assertThat(details.placeId()).isEqualTo("place-1");
    assertThat(details.coordinate().latitude()).isEqualTo(6.9337);
    assertThat(details.label()).isEqualTo("Colombo Fort Railway Station, Colombo");
  }

  @Test
  @SuppressWarnings("unchecked")
  void detailsAreCachedByPlaceIdSoRepeatSelectionsSkipTheProvider() throws Exception {
    var httpClient = mock(HttpClient.class);
    HttpResponse<String> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(200);
    when(response.body()).thenReturn(DETAILS_OK);
    when(httpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(response);
    var service =
        new GooglePlaceSearchServiceImpl(
            new GoogleMapsProperties(true, "test-key"),
            new ObjectMapper(),
            httpClient,
            new InMemoryJsonCache());

    var first = service.details("place-1", null);
    var second = service.details("place-1", null);

    assertThat(second).isEqualTo(first);
    verify(httpClient, times(1)).send(any(), any(HttpResponse.BodyHandler.class));
  }

  @Test
  @SuppressWarnings("unchecked")
  void autocompleteSendsSessionTokenAndRegionInBody() throws Exception {
    var httpClient = mock(HttpClient.class);
    HttpResponse<String> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(200);
    when(response.body()).thenReturn(AUTOCOMPLETE_OK);
    when(httpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(response);
    var service =
        new GooglePlaceSearchServiceImpl(
            new GoogleMapsProperties(true, "test-key"),
            new ObjectMapper(),
            httpClient,
            new InMemoryJsonCache());

    var suggestions = service.autocomplete("colombo fort", 6.93, 79.85, "session-token-1");

    var captor = ArgumentCaptor.forClass(HttpRequest.class);
    verify(httpClient).send(captor.capture(), any(HttpResponse.BodyHandler.class));
    String body = bodyOf(captor.getValue());
    assertThat(body).contains("\"sessionToken\":\"session-token-1\"");
    assertThat(body).contains("\"includedRegionCodes\":[\"lk\"]");
    assertThat(body).contains("locationBias");
    assertThat(suggestions).hasSize(1);
    assertThat(suggestions.getFirst().placeId()).isEqualTo("place-1");
    assertThat(suggestions.getFirst().label()).isEqualTo("Colombo Fort");
  }

  @Test
  void shortQueryReturnsEmptyWithoutCallingGoogle() {
    var httpClient = mock(HttpClient.class);
    var service =
        new GooglePlaceSearchServiceImpl(
            new GoogleMapsProperties(true, "test-key"),
            new ObjectMapper(),
            httpClient,
            new InMemoryJsonCache());

    assertThat(service.autocomplete("c", null, null, null)).isEmpty();
    org.mockito.Mockito.verifyNoInteractions(httpClient);
  }

  @Test
  void failsFastWhenMapsNotConfigured() {
    var service =
        new GooglePlaceSearchServiceImpl(
            new GoogleMapsProperties(false, ""),
            new ObjectMapper(),
            mock(HttpClient.class),
            new InMemoryJsonCache());

    assertThatThrownBy(() -> service.autocomplete("colombo", null, null, null))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("not configured");
  }

  private static String bodyOf(HttpRequest request) {
    var subscriber = java.net.http.HttpResponse.BodySubscribers.ofString(StandardCharsets.UTF_8);
    var publisher = request.bodyPublisher().orElseThrow();
    publisher.subscribe(
        new java.util.concurrent.Flow.Subscriber<>() {
          @Override
          public void onSubscribe(java.util.concurrent.Flow.Subscription subscription) {
            subscriber.onSubscribe(subscription);
          }

          @Override
          public void onNext(java.nio.ByteBuffer item) {
            subscriber.onNext(java.util.List.of(item));
          }

          @Override
          public void onError(Throwable throwable) {
            subscriber.onError(throwable);
          }

          @Override
          public void onComplete() {
            subscriber.onComplete();
          }
        });
    return subscriber.getBody().toCompletableFuture().join();
  }
}
