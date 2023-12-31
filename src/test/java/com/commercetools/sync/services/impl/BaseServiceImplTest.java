package com.commercetools.sync.services.impl;

import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.helpers.ResourceKeyIdGraphQlRequest;
import com.commercetools.sync.commons.models.ResourceKeyId;
import com.commercetools.sync.commons.models.ResourceKeyIdGraphQlResult;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.customobjects.CustomObjectSyncOptions;
import com.commercetools.sync.customobjects.CustomObjectSyncOptionsBuilder;
import com.commercetools.sync.customobjects.helpers.CustomObjectCompositeIdentifier;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.services.ProductService;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.queries.CustomObjectQuery;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.products.queries.ProductProjectionQuery;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.utils.CompletableFutureUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.data.MapEntry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@SuppressWarnings("unchecked")
class BaseServiceImplTest {

  private final TriConsumer<SyncException, Optional<ProductDraft>, Optional<ProductProjection>>
      warningCallback = mock(TriConsumer.class);

  private final SphereClient client = mock(SphereClient.class);
  private ProductService service;

  @BeforeEach
  void setup() {
    final ProductSyncOptions syncOptions =
        ProductSyncOptionsBuilder.of(client)
            .warningCallback(warningCallback)
            .batchSize(20)
            .cacheSize(2)
            .build();
    service = new ProductServiceImpl(syncOptions);
  }

  @AfterEach
  void cleanup() {
    reset(client, warningCallback);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" "})
  void fetchCachedResourceId_WithBlankKey_ShouldMakeNoRequestAndReturnEmptyOptional(
      final String key) {
    // test
    final Optional<String> result = service.getIdFromCacheOrFetch(key).toCompletableFuture().join();

    // assertions
    assertThat(result).isEmpty();
    verify(client, never()).execute(any());
  }

  @Test
  void fetchCachedResourceId_WithFetchResourceWithKey_ShouldReturnResourceId() {
    // preparation
    final PagedQueryResult pagedQueryResult = mock(PagedQueryResult.class);
    final ProductProjection mockProductResult = mock(ProductProjection.class);
    final String key = "testKey";
    final String id = "testId";
    when(mockProductResult.getKey()).thenReturn(key);
    when(mockProductResult.getId()).thenReturn(id);
    when(pagedQueryResult.getResults()).thenReturn(singletonList(mockProductResult));

    when(client.execute(any())).thenReturn(completedFuture(pagedQueryResult));

    // test
    final Optional<String> result = service.getIdFromCacheOrFetch(key).toCompletableFuture().join();

    // assertions
    assertThat(result).contains(id);
  }

  @Test
  void fetchCachedResourceId_WithCachedResource_ShouldReturnResourceIdWithoutMakingRequest() {
    // preparation
    final PagedQueryResult pagedQueryResult = mock(PagedQueryResult.class);
    final ProductProjection mockProductResult = mock(ProductProjection.class);
    final String key = "testKey";
    final String id = "testId";
    when(mockProductResult.getKey()).thenReturn(key);
    when(mockProductResult.getId()).thenReturn(id);
    when(pagedQueryResult.getResults()).thenReturn(singletonList(mockProductResult));
    when(client.execute(any())).thenReturn(completedFuture(pagedQueryResult));
    service.getIdFromCacheOrFetch(key).toCompletableFuture().join();

    // test
    final Optional<String> result = service.getIdFromCacheOrFetch(key).toCompletableFuture().join();

    // assertions
    assertThat(result).contains(id);
    // only 1 request of the first fetch, but no more since second time it gets it from cache.
    verify(client, times(1)).execute(any(ProductProjectionQuery.class));
  }

  @Test
  void fetchMatchingResources_WithEmptyKeySet_ShouldFetchAndCacheNothing() {
    // test
    final Set<ProductProjection> resources =
        service.fetchMatchingProductsByKeys(new HashSet<>()).toCompletableFuture().join();

    // assertions
    assertThat(resources).isEmpty();
    verify(client, never()).execute(any(ProductProjectionQuery.class));
  }

  @Test
  void fetchMatchingResources_WithSpecialCharactersInKeySet_ShouldExecuteQuery() {
    // preparation
    final String key1 = "special-\"charTest";

    final HashSet<String> resourceKeys = new HashSet<>();
    resourceKeys.add(key1);

    final PagedQueryResult result = mock(PagedQueryResult.class);
    when(result.getResults()).thenReturn(EMPTY_LIST);
    when(client.execute(any(ProductProjectionQuery.class))).thenReturn(completedFuture(result));

    // test
    service.fetchMatchingProductsByKeys(resourceKeys).toCompletableFuture().join();

    // assertions
    verify(client, times(1)).execute(any(ProductProjectionQuery.class));
  }

  @SuppressWarnings("unchecked")
  @Test
  void fetchMatchingResources_WithKeySet_ShouldFetchResourcesAndCacheKeys() {
    // preparation
    final String key1 = RandomStringUtils.random(15);
    final String key2 = RandomStringUtils.random(15);

    final HashSet<String> resourceKeys = new HashSet<>();
    resourceKeys.add(key1);
    resourceKeys.add(key2);

    final ProductProjection mock1 = mock(ProductProjection.class);
    when(mock1.getId()).thenReturn(RandomStringUtils.random(15));
    when(mock1.getKey()).thenReturn(key1);

    final ProductProjection mock2 = mock(ProductProjection.class);
    when(mock2.getId()).thenReturn(RandomStringUtils.random(15));
    when(mock2.getKey()).thenReturn(key2);

    final PagedQueryResult result = mock(PagedQueryResult.class);
    when(result.getResults()).thenReturn(Arrays.asList(mock1, mock2));

    when(client.execute(any(ProductProjectionQuery.class))).thenReturn(completedFuture(result));

    // test fetch
    final Set<ProductProjection> resources =
        service.fetchMatchingProductsByKeys(resourceKeys).toCompletableFuture().join();

    // assertions
    assertThat(resources).containsExactlyInAnyOrder(mock1, mock2);
    verify(client, times(1)).execute(any(ProductProjectionQuery.class));

    // test caching
    final Optional<String> cachedKey1 =
        service.getIdFromCacheOrFetch(mock1.getKey()).toCompletableFuture().join();

    final Optional<String> cachedKey2 =
        service.getIdFromCacheOrFetch(mock2.getKey()).toCompletableFuture().join();

    // assertions
    assertThat(cachedKey1).contains(mock1.getId());
    assertThat(cachedKey2).contains(mock2.getId());
    // still 1 request from the first #fetchMatchingProductsByKeys call
    verify(client, times(1)).execute(any(ProductProjectionQuery.class));
  }

  @Test
  void fetchMatchingResources_WithKeySetOf500_ShouldChunkAndFetchResourcesAndCacheKeys() {
    // preparation
    List<String> randomKeys = new ArrayList<>();
    IntStream.range(0, 500).forEach(ignore -> randomKeys.add(RandomStringUtils.random(15)));

    final HashSet<String> resourceKeys = new HashSet<>();
    resourceKeys.addAll(randomKeys);

    final ProductProjection mock1 = mock(ProductProjection.class);
    when(mock1.getId()).thenReturn(RandomStringUtils.random(15));
    when(mock1.getKey()).thenReturn(randomKeys.get(0));

    final ProductProjection mock2 = mock(ProductProjection.class);
    when(mock2.getId()).thenReturn(RandomStringUtils.random(15));
    when(mock2.getKey()).thenReturn(randomKeys.get(251));

    final PagedQueryResult result = mock(PagedQueryResult.class);
    when(result.getResults()).thenReturn(Arrays.asList(mock1, mock2));

    when(client.execute(any(ProductProjectionQuery.class))).thenReturn(completedFuture(result));

    // test fetch
    final Set<ProductProjection> resources =
        service.fetchMatchingProductsByKeys(resourceKeys).toCompletableFuture().join();

    // assertions
    assertThat(resources).containsExactlyInAnyOrder(mock1, mock2);
    verify(client, times(2)).execute(any(ProductProjectionQuery.class));

    // test caching
    final Optional<String> cachedKey1 =
        service.getIdFromCacheOrFetch(mock1.getKey()).toCompletableFuture().join();

    final Optional<String> cachedKey2 =
        service.getIdFromCacheOrFetch(mock2.getKey()).toCompletableFuture().join();

    // assertions
    assertThat(cachedKey1).contains(mock1.getId());
    assertThat(cachedKey2).contains(mock2.getId());
    verify(client, times(2)).execute(any(ProductProjectionQuery.class));
  }

  @Test
  void fetchMatchingResources_WithBadGateWayException_ShouldCompleteExceptionally() {
    // preparation
    final String key1 = RandomStringUtils.random(15);
    final String key2 = RandomStringUtils.random(15);

    final HashSet<String> resourceKeys = new HashSet<>();
    resourceKeys.add(key1);
    resourceKeys.add(key2);

    when(client.execute(any(ProductProjectionQuery.class)))
        .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(new BadGatewayException()));

    // test
    final CompletionStage<Set<ProductProjection>> result =
        service.fetchMatchingProductsByKeys(resourceKeys);

    // assertions
    assertThat(result)
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(BadGatewayException.class);
    verify(client).execute(any(ProductProjectionQuery.class));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" "})
  void fetchResource_WithBlankKey_ShouldMakeNoRequestAndReturnEmptyOptional(final String key) {
    // test
    final Optional<ProductProjection> optional =
        service.fetchProduct(key).toCompletableFuture().join();

    // assertions
    assertThat(optional).isEmpty();
    verify(client, never()).execute(any());
  }

  @SuppressWarnings("unchecked")
  @Test
  void fetchResource_WithKey_ShouldFetchResource() {
    // preparation
    final String resourceId = RandomStringUtils.random(15);
    final String resourceKey = RandomStringUtils.random(15);

    final ProductProjection mockProductResult = mock(ProductProjection.class);
    when(mockProductResult.getKey()).thenReturn(resourceKey);
    when(mockProductResult.getId()).thenReturn(resourceId);

    final PagedQueryResult<ProductProjection> result = mock(PagedQueryResult.class);
    when(result.head()).thenReturn(Optional.of(mockProductResult));

    when(client.execute(any())).thenReturn(completedFuture(result));

    // test
    final Optional<ProductProjection> resourceOptional =
        service.fetchProduct(resourceKey).toCompletableFuture().join();

    // assertions
    assertThat(resourceOptional).containsSame(mockProductResult);
    verify(client).execute(any(ProductProjectionQuery.class));
  }

  @Test
  void fetchResource_WithBadGateWayException_ShouldCompleteExceptionally() {
    // preparation
    when(client.execute(any(ProductProjectionQuery.class)))
        .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(new BadGatewayException()));

    // test
    final CompletionStage<Optional<ProductProjection>> result = service.fetchProduct("foo");

    // assertions
    assertThat(result)
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(BadGatewayException.class);
    verify(client, times(1)).execute(any(ProductProjectionQuery.class));
  }

  @Test
  void cacheKeysToIdsUsingGraphQl_WithEmptySetOfKeys_ShouldMakeNoRequestAndReturnEmptyOptional() {
    // test
    final Map<String, String> optional =
        service.cacheKeysToIds(emptySet()).toCompletableFuture().join();

    // assertions
    assertThat(optional).isEmpty();
    verify(client, never()).execute(any());
  }

  @Test
  void cacheKeysToIdsUsingGraphQl_WithAllCachedKeys_ShouldMakeNoRequestAndReturnCachedEntry() {
    // preparation
    final PagedQueryResult pagedQueryResult = mock(PagedQueryResult.class);
    final ProductProjection mockProductResult = mock(ProductProjection.class);
    final String key = "testKey";
    final String id = "testId";
    when(mockProductResult.getKey()).thenReturn(key);
    when(mockProductResult.getId()).thenReturn(id);
    when(pagedQueryResult.getResults()).thenReturn(singletonList(mockProductResult));
    when(client.execute(any())).thenReturn(completedFuture(pagedQueryResult));
    service.getIdFromCacheOrFetch(key).toCompletableFuture().join();

    // test
    final Map<String, String> optional =
        service.cacheKeysToIds(singleton("testKey")).toCompletableFuture().join();

    // assertions
    assertThat(optional).containsExactly(MapEntry.entry(key, id));
    verify(client, times(1)).execute(any(ProductProjectionQuery.class));
  }

  @Test
  void cacheKeysToIds_WithCachedKeysExceedingCacheSize_ShouldNotReturnLeastUsedKeys() {
    // preparation
    final PagedQueryResult pagedQueryResult = mock(PagedQueryResult.class);
    final ProductProjection product1 = mock(ProductProjection.class);
    when(product1.getKey()).thenReturn("key-1");
    when(product1.getId()).thenReturn("id-1");
    final ProductProjection product2 = mock(ProductProjection.class);
    when(product2.getKey()).thenReturn("key-2");
    when(product2.getId()).thenReturn("id-2");
    when(pagedQueryResult.getResults()).thenReturn(Arrays.asList(product1, product2));
    final ResourceKeyIdGraphQlResult resourceKeyIdGraphQlResult =
        mock(ResourceKeyIdGraphQlResult.class);
    final ResourceKeyId resourceKeyId = mock(ResourceKeyId.class);
    when(resourceKeyId.getKey()).thenReturn("testKey");
    when(resourceKeyId.getId()).thenReturn("testId");
    when(resourceKeyIdGraphQlResult.getResults()).thenReturn(singleton(resourceKeyId));
    when(client.execute(any()))
        .thenReturn(completedFuture(pagedQueryResult))
        .thenReturn(completedFuture(resourceKeyIdGraphQlResult));
    service.fetchMatchingProductsByKeys(
        Arrays.asList("key-1", "key-2").stream().collect(Collectors.toSet()));
    service.getIdFromCacheOrFetch("key-1"); // access the first added cache entry

    // test
    final Map<String, String> optional =
        service.cacheKeysToIds(singleton("testKey")).toCompletableFuture().join();

    // assertions
    assertThat(optional)
        .containsExactly(MapEntry.entry("key-1", "id-1"), MapEntry.entry("testKey", "testId"));
    verify(client, times(1)).execute(any(ProductProjectionQuery.class));
    verify(client, times(1)).execute(any(ResourceKeyIdGraphQlRequest.class));
  }

  @Test
  void cacheKeysToIdsUsingGraphQl_WithNoCachedKeys_ShouldMakeRequestAndReturnCachedEntry() {
    // preparation
    final ResourceKeyIdGraphQlResult resourceKeyIdGraphQlResult =
        mock(ResourceKeyIdGraphQlResult.class);
    final ResourceKeyId mockResourceKeyId = mock(ResourceKeyId.class);
    final String key = "testKey";
    final String id = "testId";
    when(mockResourceKeyId.getKey()).thenReturn(key);
    when(mockResourceKeyId.getId()).thenReturn(id);
    when(resourceKeyIdGraphQlResult.getResults()).thenReturn(singleton(mockResourceKeyId));
    when(client.execute(any())).thenReturn(completedFuture(resourceKeyIdGraphQlResult));

    // test
    final Map<String, String> optional =
        service.cacheKeysToIds(singleton("testKey")).toCompletableFuture().join();

    // assertions
    assertThat(optional).containsExactly(MapEntry.entry(key, id));
    verify(client, times(1)).execute(any(ResourceKeyIdGraphQlRequest.class));
  }

  @Test
  void cacheKeysToIdsUsingGraphQl_With500Keys_ShouldChunkAndMakeRequestAndReturnCachedEntry() {
    // preparation
    Set<String> randomKeys = new HashSet<>();
    IntStream.range(0, 500).forEach(ignore -> randomKeys.add(RandomStringUtils.random(15)));

    final ResourceKeyIdGraphQlResult resourceKeyIdGraphQlResult =
        mock(ResourceKeyIdGraphQlResult.class);
    final ResourceKeyId mockResourceKeyId = mock(ResourceKeyId.class);
    final String key = randomKeys.stream().findFirst().get();
    final String id = "testId";
    when(mockResourceKeyId.getKey()).thenReturn(key);
    when(mockResourceKeyId.getId()).thenReturn(id);
    when(resourceKeyIdGraphQlResult.getResults()).thenReturn(singleton(mockResourceKeyId));
    when(client.execute(any())).thenReturn(completedFuture(resourceKeyIdGraphQlResult));

    // test
    final Map<String, String> optional =
        service.cacheKeysToIds(randomKeys).toCompletableFuture().join();

    // assertions
    assertThat(optional).containsExactly(MapEntry.entry(key, id));
    verify(client, times(2)).execute(any(ResourceKeyIdGraphQlRequest.class));
  }

  @Test
  void cacheKeysToIdsUsingGraphQl_WithBadGateWayException_ShouldCompleteExceptionally() {
    // preparation
    final ResourceKeyIdGraphQlResult resourceKeyIdGraphQlResult =
        mock(ResourceKeyIdGraphQlResult.class);
    final ResourceKeyId mockResourceKeyId = mock(ResourceKeyId.class);
    final String key = "testKey";
    final String id = "testId";
    when(mockResourceKeyId.getKey()).thenReturn(key);
    when(mockResourceKeyId.getId()).thenReturn(id);
    when(resourceKeyIdGraphQlResult.getResults()).thenReturn(singleton(mockResourceKeyId));
    when(client.execute(any(ResourceKeyIdGraphQlRequest.class)))
        .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(new BadGatewayException()));

    // test
    final CompletionStage<Map<String, String>> result =
        service.cacheKeysToIds(singleton("testKey"));

    // assertions
    assertThat(result)
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(BadGatewayException.class);
    verify(client, times(1)).execute(any(ResourceKeyIdGraphQlRequest.class));
  }

  @Test
  void cacheKeysToIds_WithEmptySetOfKeys_ShouldNotMakeRequestAndReturnEmpty() {
    // preparation
    CustomObjectSyncOptions customObjectSyncOptions =
        CustomObjectSyncOptionsBuilder.of(client).build();
    CustomObjectServiceImpl serviceImpl = new CustomObjectServiceImpl(customObjectSyncOptions);

    // test
    final Map<String, String> optional =
        serviceImpl.cacheKeysToIds(emptySet()).toCompletableFuture().join();

    // assertions
    assertThat(optional).isEmpty();
    verify(client, never()).execute(any());
  }

  @Test
  void cacheKeysToIds_WithEmptyCache_ShouldMakeRequestAndReturnCacheEntries() {
    // preparation
    final CustomObjectSyncOptions customObjectSyncOptions =
        CustomObjectSyncOptionsBuilder.of(client).build();
    final CustomObjectServiceImpl serviceImpl =
        new CustomObjectServiceImpl(customObjectSyncOptions);
    final PagedQueryResult pagedQueryResult = mock(PagedQueryResult.class);
    final CustomObject customObject = mock(CustomObject.class);
    final String customObjectId = "customObjectId";
    final String customObjectContainer = "customObjectContainer";
    final String customObjectKey = "customObjectKey";

    when(customObject.getId()).thenReturn(customObjectId);
    when(customObject.getKey()).thenReturn(customObjectKey);
    when(customObject.getContainer()).thenReturn(customObjectContainer);
    when(pagedQueryResult.getResults()).thenReturn(singletonList(customObject));
    when(client.execute(any())).thenReturn(completedFuture(pagedQueryResult));

    final Map<String, String> result =
        serviceImpl
            .cacheKeysToIds(
                singleton(
                    CustomObjectCompositeIdentifier.of(customObjectKey, customObjectContainer)))
            .toCompletableFuture()
            .join();

    assertAll(
        () -> assertThat(result).hasSize(1),
        () ->
            assertThat(
                    result.get(
                        CustomObjectCompositeIdentifier.of(customObjectKey, customObjectContainer)
                            .toString()))
                .isEqualTo(customObjectId));
    verify(client).execute(any(CustomObjectQuery.class));
  }

  @Test
  void
      cacheKeysToIds_With500CustomObjectIdentifiers_ShouldChunkAndMakeRequestAndReturnCacheEntries() {
    // preparation
    Set<CustomObjectCompositeIdentifier> randomIdentifiers = new HashSet<>();
    IntStream.range(0, 500)
        .forEach(
            i ->
                randomIdentifiers.add(
                    CustomObjectCompositeIdentifier.of(
                        "customObjectId" + i, "customObjectContainer" + i)));

    final CustomObjectSyncOptions customObjectSyncOptions =
        CustomObjectSyncOptionsBuilder.of(client).build();
    final CustomObjectServiceImpl serviceImpl =
        new CustomObjectServiceImpl(customObjectSyncOptions);
    final PagedQueryResult pagedQueryResult = mock(PagedQueryResult.class);
    final CustomObject customObject = mock(CustomObject.class);
    final String customObjectId = randomIdentifiers.stream().findFirst().get().getKey();
    final String customObjectContainer =
        randomIdentifiers.stream().findFirst().get().getContainer();
    final String customObjectKey = "customObjectKey";

    when(customObject.getId()).thenReturn(customObjectId);
    when(customObject.getKey()).thenReturn(customObjectKey);
    when(customObject.getContainer()).thenReturn(customObjectContainer);
    when(pagedQueryResult.getResults()).thenReturn(singletonList(customObject));
    when(client.execute(any())).thenReturn(completedFuture(pagedQueryResult));

    // test
    serviceImpl.cacheKeysToIds(randomIdentifiers).toCompletableFuture().join();

    // assertion
    verify(client, times(2)).execute(any(CustomObjectQuery.class));
  }
}
