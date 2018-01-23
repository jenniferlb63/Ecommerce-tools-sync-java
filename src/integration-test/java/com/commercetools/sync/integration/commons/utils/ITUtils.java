package com.commercetools.sync.integration.commons.utils;

import com.commercetools.sync.commons.helpers.BaseSyncStatistics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereRequest;
import io.sphere.sdk.models.Resource;
import io.sphere.sdk.queries.QueryDsl;
import io.sphere.sdk.types.commands.TypeDeleteCommand;
import io.sphere.sdk.types.queries.TypeQuery;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static io.sphere.sdk.queries.QueryExecutionUtils.queryAll;

public final class ITUtils {

    /**
     * Deletes all Types from CTP projects defined by the {@code sphereClient}
     *
     * @param ctpClient defines the CTP project to delete the Types from.
     */
    public static void deleteTypes(@Nonnull final SphereClient ctpClient) {
        queryAndApply(ctpClient, TypeQuery::of, TypeDeleteCommand::of);
    }

    /**
     * Deletes all Types from CTP projects defined by the {@code CTP_SOURCE_CLIENT} and {@code CTP_TARGET_CLIENT}.
     */
    public static void deleteTypesFromTargetAndSource() {
        deleteTypes(CTP_TARGET_CLIENT);
        deleteTypes(CTP_SOURCE_CLIENT);
    }

    /**
     * Builds a JSON String that represents the fields of the supplied instance of {@link BaseSyncStatistics}.
     * Note: The order of the fields in the built JSON String depends on the order of the instance variables in this
     * class.
     *
     * @param statistics the instance of {@link BaseSyncStatistics} from which to create a JSON String.
     * @return a JSON String representation of the statistics object.
     */
    public static String getStatisticsAsJSONString(@Nonnull final BaseSyncStatistics statistics)
        throws JsonProcessingException {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(statistics);
    }

    /**
     * Applies the {@code pageMapper} function on each page fetched from the supplied {@code queryRequestSupplier} on
     * the supplied {@code ctpClient}.
     *
     * @param ctpClient            defines the CTP project to apply the query on.
     * @param queryRequestSupplier defines a supplier which, when executed, returns the query that should be made on
     *                             the CTP project.
     * @param resourceMapper       defines a mapper function that should be applied on each resource in the fetched page
     *                             from the query on the specified CTP project.
     */
    public static <T extends Resource, C extends QueryDsl<T, C>> void queryAndApply(
        @Nonnull final SphereClient ctpClient,
        @Nonnull final Supplier<QueryDsl<T, C>> queryRequestSupplier,
        @Nonnull final Function<T, SphereRequest<T>> resourceMapper) {
        queryAll(ctpClient, queryRequestSupplier.get(), resourceMapper)
            .thenApply(sphereRequests -> sphereRequests.stream()
                                                       .map(ctpClient::execute)
                                                       .map(CompletionStage::toCompletableFuture))
            .thenApply(stream -> stream.toArray(CompletableFuture[]::new))
            .thenCompose(CompletableFuture::allOf)
            .toCompletableFuture().join();
    }

    public static <T extends Resource, C extends QueryDsl<T, C>> void queryAndCompose(
        @Nonnull final SphereClient ctpClient,
        @Nonnull final Supplier<QueryDsl<T, C>> queryRequestSupplier,
        @Nonnull final Function<T, CompletionStage> resourceMapper) {
        queryAll(ctpClient, queryRequestSupplier.get(), resourceMapper)
            .thenApply(stage -> stage.stream().map(CompletionStage::toCompletableFuture))
            .thenApply(stream -> stream.toArray(CompletableFuture[]::new))
            .thenCompose(CompletableFuture::allOf)
            .toCompletableFuture().join();
    }
}
