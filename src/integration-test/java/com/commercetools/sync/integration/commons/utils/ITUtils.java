package com.commercetools.sync.integration.commons.utils;

import com.commercetools.sync.commons.utils.CtpQueryUtils;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereRequest;
import io.sphere.sdk.models.Resource;
import io.sphere.sdk.queries.QueryDsl;
import io.sphere.sdk.types.commands.TypeDeleteCommand;
import io.sphere.sdk.types.queries.TypeQuery;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;

public final class ITUtils {

    /**
     * Deletes all Types from CTP projects defined by the {@code sphereClient}
     *
     * @param ctpClient defines the CTP project to delete the Types from.
     */
    public static void deleteTypes(@Nonnull final SphereClient ctpClient) {
        queryAndExecute(ctpClient, TypeQuery.of(), TypeDeleteCommand::of);
    }

    /**
     * Deletes all Types from CTP projects defined by the {@code CTP_SOURCE_CLIENT} and {@code CTP_TARGET_CLIENT}.
     */
    public static void deleteTypesFromTargetAndSource() {
        deleteTypes(CTP_TARGET_CLIENT);
        deleteTypes(CTP_SOURCE_CLIENT);
    }

    /**
     * Applies the {@code resourceToRequestMapper} function on each page, resulting from the {@code query} executed by
     * the {@code ctpClient}, to map each resource to a {@link SphereRequest} and then executes these requests in
     * parallel within each page.
     *
     * @param ctpClient               defines the CTP project to apply the query on.
     * @param query                   query that should be made on the CTP project.
     * @param resourceToRequestMapper defines a mapper function that should be applied on each resource, in the fetched
     *                                page from the query on the specified CTP project, to map it to a
     *                                {@link SphereRequest}.
     */
    public static <T extends Resource, C extends QueryDsl<T, C>> void queryAndExecute(
        @Nonnull final SphereClient ctpClient,
        @Nonnull final QueryDsl<T, C> query,
        @Nonnull final Function<T, SphereRequest<T>> resourceToRequestMapper) {

        queryAndCompose(ctpClient, query, resource -> ctpClient.execute(resourceToRequestMapper.apply(resource)));
    }

    /**
     * Applies the {@code resourceToStageMapper} function on each page, resulting from the {@code query} executed by
     * the {@code ctpClient}, to map each resource to a {@link CompletionStage} and then executes these stages in
     * parallel within each page.
     *
     *
     * @param ctpClient             defines the CTP project to apply the query on.
     * @param query                 query that should be made on the CTP project.
     * @param resourceToStageMapper defines a mapper function that should be applied on each resource, in the fetched
     *                              page from the query on the specified CTP project, to map it to a
     *                              {@link CompletionStage} which will be executed (in a blocking fashion) after
     *                              every page fetch.
     *
     */
    public static <T extends Resource, C extends QueryDsl<T, C>, S> void queryAndCompose(
        @Nonnull final SphereClient ctpClient,
        @Nonnull final QueryDsl<T, C> query,
        @Nonnull final Function<T, CompletionStage<S>> resourceToStageMapper) {

        // TODO: GITHUB ISSUE #248
        final Consumer<List<T>> pageConsumer =
            pageElements -> CompletableFuture.allOf(pageElements.stream()
                                                                .map(resourceToStageMapper)
                                                                .map(CompletionStage::toCompletableFuture)
                                                                .toArray(CompletableFuture[]::new))
                                             .join();

        CtpQueryUtils.queryAll(ctpClient, query, pageConsumer)
                     .toCompletableFuture()
                     .join();
    }

    /**
     * Creates an {@link AssetDraft} with the with the given key and name.
     *
     * @param assetKey  asset draft key.
     * @param assetName asset draft name.
     * @return an {@link AssetDraft} with the with the given key and name.
     */
    public static AssetDraft createAssetDraft(@Nonnull final String assetKey,
                                              @Nonnull final LocalizedString assetName) {
        return createAssetDraftBuilder(assetKey, assetName).build();
    }

    /**
     * Creates an {@link AssetDraftBuilder} with the with the given key and name. The builder created will contain one
     * tag with the same value as the key and will contain one {@link io.sphere.sdk.models.AssetSource}
     * with the uri {@code sourceUri}.
     *
     * @param assetKey  asset draft key.
     * @param assetName asset draft name.
     * @return an {@link AssetDraftBuilder} with the with the given key and name. The builder created will contain one
     *         tag with the same value as the key and will contain one {@link io.sphere.sdk.models.AssetSource} with the
     *         uri {@code sourceUri}.
     */
    private static AssetDraftBuilder createAssetDraftBuilder(@Nonnull final String assetKey,
                                                             @Nonnull final LocalizedString assetName) {
        return AssetDraftBuilder.of(emptyList(), assetName)
                                .key(assetKey)
                                .tags(singleton(assetKey))
                                .sources(singletonList(AssetSourceBuilder.ofUri("sourceUri").build()));
    }

    /**
     * Creates an {@link AssetDraft} with the with the given key and name. The asset draft created will have custom field
     * with the type id supplied ({@code assetCustomTypeId} and the fields built from the method
     * {@link ITUtils#createCustomFieldsJsonMap()}.
     *
     * @param assetKey          asset draft key.
     * @param assetName         asset draft name.
     * @param assetCustomTypeId the asset custom type id.
     * @return an {@link AssetDraft} with the with the given key and name. The asset draft created will have custom
     *         field with the type id supplied ({@code assetCustomTypeId} and the fields built from the method
     *         {@link ITUtils#createCustomFieldsJsonMap()}.
     */
    public static AssetDraft createAssetDraft(@Nonnull final String assetKey,
                                              @Nonnull final LocalizedString assetName,
                                              @Nonnull final String assetCustomTypeId) {
        return createAssetDraft(assetKey, assetName, assetCustomTypeId, createCustomFieldsJsonMap());
    }

    /**
     * Creates an {@link AssetDraft} with the with the given key and name. The asset draft created will have custom field
     * with the type id supplied ({@code assetCustomTypeId} and the custom fields will be defined by the
     * {@code customFieldsJsonMap} supplied.
     *
     * @param assetKey          asset draft key.
     * @param assetName         asset draft name.
     * @param assetCustomTypeId the asset custom type id.
     * @param customFieldsJsonMap the custom fields of the asset custom type.
     * @return an {@link AssetDraft} with the with the given key and name. The asset draft created will have custom field
     *         with the type id supplied ({@code assetCustomTypeId} and the custom fields will be defined by the
     *         {@code customFieldsJsonMap} supplied.
     */
    public static AssetDraft createAssetDraft(@Nonnull final String assetKey,
                                              @Nonnull final LocalizedString assetName,
                                              @Nonnull final String assetCustomTypeId,
                                              @Nonnull final Map<String, JsonNode> customFieldsJsonMap) {
        return createAssetDraftBuilder(assetKey, assetName)
            .custom(CustomFieldsDraft.ofTypeIdAndJson(assetCustomTypeId, customFieldsJsonMap))
            .build();
    }

    /**
     * Creates a {@link ProductVariantDraft} draft key and sku of the value supplied {@code variantKeyAndSku} and with
     * the supplied {@code assetDrafts}.
     *
     * @param variantKeyAndSku the value of the key and sku of the created draft.
     * @param assetDrafts the assets to assign to the created draft.
     * @return a {@link ProductVariantDraft} draft key and sku of the value supplied {@code variantKeyAndSku} and with
     *         the supplied {@code assetDrafts}.
     */
    public static ProductVariantDraft createVariantDraft(@Nonnull final String variantKeyAndSku,
                                                         @Nonnull final List<AssetDraft> assetDrafts) {

        return ProductVariantDraftBuilder.of()
                                         .key(variantKeyAndSku)
                                         .sku(variantKeyAndSku)
                                         .assets(assetDrafts)
                                         .build();
    }

    /**
     * Asserts that a list of {@link Asset} and a list of {@link AssetDraft} have the same ordering of assets (assets
     * are matched by key). It asserts that the matching assets have the same name, description, custom fields, tags,
     * and asset sources.
     *
     * TODO: This should be refactored into Asset asserts helpers. GITHUB ISSUE#261
     *
     * @param assets      the list of assets to compare to the list of asset drafts.
     * @param assetDrafts the list of asset drafts to compare to the list of assets.
     */
    public static void assertAssetsAreEqual(@Nonnull final List<Asset> assets,
                                            @Nonnull final List<AssetDraft> assetDrafts) {
        IntStream.range(0, assetDrafts.size())
                 .forEach(index -> {
                     final Asset createdAsset = assets.get(index);
                     final AssetDraft assetDraft = assetDrafts.get(index);

                     assertThat(createdAsset.getName()).isEqualTo(assetDraft.getName());
                     assertThat(createdAsset.getDescription()).isEqualTo(assetDraft.getDescription());
                     assertThat(createdAsset.getKey()).isEqualTo(assetDraft.getKey());
                     assertThat(createdAsset.getCustom()).isNotNull();
                     assertThat(createdAsset.getCustom().getFieldsJsonMap())
                         .isEqualTo(assetDraft.getCustom().getFields());
                     assertThat(createdAsset.getTags()).isEqualTo(assetDraft.getTags());
                     assertThat(createdAsset.getSources()).isEqualTo(assetDraft.getSources());
                 });
    }
}
