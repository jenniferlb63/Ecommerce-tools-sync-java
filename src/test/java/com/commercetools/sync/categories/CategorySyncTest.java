package com.commercetools.sync.categories;

import com.commercetools.sync.categories.helpers.CategorySyncStatistics;
import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.services.CategoryService;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.SphereException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static com.commercetools.sync.categories.CategorySyncMockUtils.getMockCategoryDraft;
import static com.commercetools.sync.commons.MockUtils.getMockCategoryService;
import static com.commercetools.sync.commons.MockUtils.getMockTypeService;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CategorySyncTest {
    private CategorySync categorySync;
    private CategorySyncOptions categorySyncOptions;

    private List<String> errorCallBackMessages;
    private List<Throwable> errorCallBackExceptions;

    /**
     * Initializes instances of  {@link CategorySyncOptions} and {@link CategorySync} which will be used by some
     * of the unit test methods in this test class.
     */
    @Before
    public void setup() {
        errorCallBackMessages = new ArrayList<>();
        errorCallBackExceptions = new ArrayList<>();
        final SphereClient ctpClient = mock(SphereClient.class);
        categorySyncOptions = CategorySyncOptionsBuilder.of(ctpClient)
                                                        .setErrorCallBack((errorMessage, exception) -> {
                                                            errorCallBackMessages.add(errorMessage);
                                                            errorCallBackExceptions.add(exception);
                                                        })
                                                        .build();
        categorySync = new CategorySync(categorySyncOptions, getMockTypeService(), getMockCategoryService());
    }

    @Test
    public void sync_WithEmptyListOfDrafts_ShouldNotProcessAnyCategories() {
        final CategoryService mockCategoryService = getMockCategoryService();
        when(mockCategoryService.fetchMatchingCategoriesByKeys(any()))
            .thenReturn(CompletableFuture.completedFuture(Collections.emptySet()));

        final CategorySync mockCategorySync =
            new CategorySync(categorySyncOptions, getMockTypeService(), mockCategoryService);

        final CategorySyncStatistics syncStatistics = mockCategorySync.sync(new ArrayList<>())
                                                                      .toCompletableFuture().join();

        assertThat(syncStatistics.getCreated()).isEqualTo(0);
        assertThat(syncStatistics.getFailed()).isEqualTo(0);
        assertThat(syncStatistics.getUpdated()).isEqualTo(0);
        assertThat(syncStatistics.getProcessed()).isEqualTo(0);
        assertThat(syncStatistics.getReportMessage()).isEqualTo("Summary: 0 categories were processed in "
            + "total (0 created, 0 updated and 0 categories failed to sync).");
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
    }

    @Test
    public void sync_WithANullDraft_ShouldBeCountedAsFailed() {
        final CategoryService mockCategoryService = getMockCategoryService();
        when(mockCategoryService.fetchMatchingCategoriesByKeys(any()))
            .thenReturn(CompletableFuture.completedFuture(Collections.emptySet()));

        final CategorySync mockCategorySync =
            new CategorySync(categorySyncOptions, getMockTypeService(), mockCategoryService);

        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();
        categoryDrafts.add(null);

        final CategorySyncStatistics syncStatistics = mockCategorySync.sync(categoryDrafts)
                                                                      .toCompletableFuture().join();
        assertThat(syncStatistics.getCreated()).isEqualTo(0);
        assertThat(syncStatistics.getFailed()).isEqualTo(1);
        assertThat(syncStatistics.getUpdated()).isEqualTo(0);
        assertThat(syncStatistics.getProcessed()).isEqualTo(1);
        assertThat(syncStatistics.getReportMessage()).isEqualTo("Summary: 1 categories were processed"
            + " in total (0 created, 0 updated and 1 categories failed to sync).");
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).isEqualTo("CategoryDraft is null.");
        assertThat(errorCallBackExceptions).hasSize(1);
        assertThat(errorCallBackExceptions.get(0)).isEqualTo(null);
    }

    @Test
    public void sync_WithADraftWithNoSetKey_ShouldFailSync() {
        final CategoryService mockCategoryService = getMockCategoryService();
        when(mockCategoryService.fetchMatchingCategoriesByKeys(any()))
            .thenReturn(CompletableFuture.completedFuture(Collections.emptySet()));

        final CategorySync mockCategorySync =
            new CategorySync(categorySyncOptions, getMockTypeService(), mockCategoryService);

        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();
        categoryDrafts.add(getMockCategoryDraft(Locale.ENGLISH, "noKeyDraft", "no-key-id-draft",null));


        final CategorySyncStatistics syncStatistics = mockCategorySync.sync(categoryDrafts)
                                                                      .toCompletableFuture().join();
        assertThat(syncStatistics.getCreated()).isEqualTo(0);
        assertThat(syncStatistics.getFailed()).isEqualTo(1);
        assertThat(syncStatistics.getUpdated()).isEqualTo(0);
        assertThat(syncStatistics.getProcessed()).isEqualTo(1);
        assertThat(syncStatistics.getReportMessage()).isEqualTo("Summary: 1 categories were processed"
            + " in total (0 created, 0 updated and 1 categories failed to sync).");
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).isEqualTo("CategoryDraft with name: "
            + "LocalizedString(en -> noKeyDraft) doesn't have a key.");
        assertThat(errorCallBackExceptions).hasSize(1);
        assertThat(errorCallBackExceptions.get(0)).isEqualTo(null);
    }

    @Test
    public void sync_WithNoExistingCategory_ShouldCreateCategory() {
        final CategoryService mockCategoryService = getMockCategoryService();
        when(mockCategoryService.fetchMatchingCategoriesByKeys(any()))
            .thenReturn(CompletableFuture.completedFuture(Collections.emptySet()));

        final CategorySync mockCategorySync =
            new CategorySync(categorySyncOptions, getMockTypeService(), mockCategoryService);

        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();
        categoryDrafts.add(getMockCategoryDraft(Locale.ENGLISH, "name", "newKey", "parentKey",
            "customTypeId", new HashMap<>()));

        final CategorySyncStatistics syncStatistics = mockCategorySync.sync(categoryDrafts)
                                                                      .toCompletableFuture().join();

        assertThat(syncStatistics.getCreated()).isEqualTo(1);
        assertThat(syncStatistics.getFailed()).isEqualTo(0);
        assertThat(syncStatistics.getUpdated()).isEqualTo(0);
        assertThat(syncStatistics.getProcessed()).isEqualTo(1);
        assertThat(syncStatistics.getReportMessage()).isEqualTo("Summary: 1 categories were processed"
            + " in total (1 created, 0 updated and 0 categories failed to sync).");
        assertThat(errorCallBackMessages).hasSize(0);
        assertThat(errorCallBackExceptions).hasSize(0);
    }

    @Test
    public void sync_WithExistingCategory_ShouldUpdateCategory() {
        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();
        categoryDrafts.add(getMockCategoryDraft(Locale.ENGLISH, "name", "key", "parentKey",
            "customTypeId", new HashMap<>()));

        final CategorySyncStatistics syncStatistics = categorySync.sync(categoryDrafts)
                                                                  .toCompletableFuture().join();
        assertThat(syncStatistics.getCreated()).isEqualTo(0);
        assertThat(syncStatistics.getFailed()).isEqualTo(0);
        assertThat(syncStatistics.getUpdated()).isEqualTo(1);
        assertThat(syncStatistics.getProcessed()).isEqualTo(1);
        assertThat(syncStatistics.getReportMessage()).isEqualTo("Summary: 1 categories were processed"
            + " in total (0 created, 1 updated and 0 categories failed to sync).");
        assertThat(errorCallBackMessages).hasSize(0);
        assertThat(errorCallBackExceptions).hasSize(0);
    }

    @Test
    public void sync_WithIdenticalExistingCategory_ShouldNotUpdateCategory() {
        final CategoryDraft categoryDraft = getMockCategoryDraft(Locale.ENGLISH,
            "name",
            "slug",
            "key",
            "externalId",
            "description",
            "metaDescription",
            "metaTitle",
            "metaKeywords",
            "orderHint",
            "parentId");
        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();
        categoryDrafts.add(categoryDraft);

        final CategorySyncStatistics syncStatistics = categorySync.sync(categoryDrafts).toCompletableFuture().join();

        assertThat(syncStatistics.getCreated()).isEqualTo(0);
        assertThat(syncStatistics.getFailed()).isEqualTo(0);
        assertThat(syncStatistics.getUpdated()).isEqualTo(0);
        assertThat(syncStatistics.getProcessed()).isEqualTo(1);
        assertThat(syncStatistics.getReportMessage()).isEqualTo("Summary: 1 categories were processed"
            + " in total (0 created, 0 updated and 0 categories failed to sync).");
        assertThat(errorCallBackMessages).hasSize(0);
        assertThat(errorCallBackExceptions).hasSize(0);
    }

    //TODO
    @Ignore("Can't mock service needs to be in integration tests")
    @Test
    public void sync_WithExistingCategoryButExceptionOnFetch_ShouldFailSync() {
        CompletableFuture<Set<Category>> futureThrowingSphereException = CompletableFuture.supplyAsync(() -> {
            throw new SphereException();
        });
        final CategoryService mockCategoryService = getMockCategoryService();
        when(mockCategoryService.fetchMatchingCategoriesByKeys(any()))
            .thenReturn(futureThrowingSphereException);

        final CategorySync categorySync = new CategorySync(categorySyncOptions, getMockTypeService(),
            mockCategoryService);
        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();
        categoryDrafts.add(getMockCategoryDraft(Locale.ENGLISH, "name", "slug", "key"));

        final CategorySyncStatistics syncStatistics = categorySync.sync(categoryDrafts).toCompletableFuture().join();

        assertThat(syncStatistics.getCreated()).isEqualTo(0);
        assertThat(syncStatistics.getFailed()).isEqualTo(1);
        assertThat(syncStatistics.getUpdated()).isEqualTo(0);
        assertThat(syncStatistics.getProcessed()).isEqualTo(1);
        assertThat(syncStatistics.getReportMessage()).isEqualTo("Summary: 1 categories were processed in "
            + "total (0 created, 0 updated and 1 categories failed to sync).");
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).contains("Failed to fetch category with key:'key'");
        assertThat(errorCallBackExceptions).hasSize(1);
        assertThat(errorCallBackExceptions.get(0)).isExactlyInstanceOf(CompletionException.class);
    }

    //TODO
    @Ignore("Can't mock service needs to be in integration tests")
    @Test
    public void sync_WithNoExistingCategoryButExceptionOnCreate_ShouldFailSync() {
        final CompletableFuture<Optional<Category>> futureThrowingSphereException =
            CompletableFuture.supplyAsync(() -> {
                throw new SphereException();
            });
        final CategoryService categoryService = getMockCategoryService();
        when(categoryService.createCategory(any())).thenReturn(futureThrowingSphereException);

        final CategorySync categorySync = new CategorySync(categorySyncOptions, getMockTypeService(), categoryService);
        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();
        categoryDrafts.add(getMockCategoryDraft(Locale.ENGLISH, "name", "slug", "newKey"));


        final CategorySyncStatistics syncStatistics = categorySync.sync(categoryDrafts).toCompletableFuture().join();

        assertThat(syncStatistics.getCreated()).isEqualTo(0);
        assertThat(syncStatistics.getFailed()).isEqualTo(1);
        assertThat(syncStatistics.getUpdated()).isEqualTo(0);
        assertThat(syncStatistics.getProcessed()).isEqualTo(1);
        assertThat(syncStatistics.getReportMessage()).isEqualTo(
            "Summary: 1 categories were processed in total "
                + "(0 created, 0 updated and 1 categories failed to sync).");
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).contains("Failed to create category with key:'key'");
        assertThat(errorCallBackExceptions).hasSize(1);
        assertThat(errorCallBackExceptions.get(0)).isExactlyInstanceOf(CompletionException.class);
    }

    @Test
    public void sync_WithExistingCategoryButExceptionOnUpdate_ShouldFailSync() {
        final CompletableFuture<Optional<Category>> futureThrowingSphereException =
            CompletableFuture.supplyAsync(() -> {
                throw new SphereException();
            });
        final CategoryService categoryService = getMockCategoryService();
        when(categoryService.updateCategory(any(), any())).thenReturn(futureThrowingSphereException);
        final CategorySync categorySync = new CategorySync(categorySyncOptions, getMockTypeService(), categoryService);
        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();
        categoryDrafts.add(getMockCategoryDraft(Locale.ENGLISH, "name", "slug", "key"));

        final CategorySyncStatistics syncStatistics = categorySync.sync(categoryDrafts).toCompletableFuture().join();
        assertThat(syncStatistics.getCreated()).isEqualTo(0);
        assertThat(syncStatistics.getFailed()).isEqualTo(1);
        assertThat(syncStatistics.getUpdated()).isEqualTo(0);
        assertThat(syncStatistics.getProcessed()).isEqualTo(1);
        assertThat(syncStatistics.getReportMessage()).isEqualTo(
            "Summary: 1 categories were processed in total "
                + "(0 created, 0 updated and 1 categories failed to sync).");
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).contains("Failed to update category with key:'key'");
        assertThat(errorCallBackExceptions).hasSize(1);
        assertThat(errorCallBackExceptions.get(0)).isExactlyInstanceOf(CompletionException.class);
    }

    @Test
    public void sync_WithExistingCategoryButWithNotAllowedUuidReferenceResolution_ShouldFailSync() {
        final CategorySync categorySync = new CategorySync(categorySyncOptions, getMockTypeService(),
            getMockCategoryService());
        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();

        final String parentUuid = String.valueOf(UUID.randomUUID());
        categoryDrafts.add(getMockCategoryDraft(Locale.ENGLISH, "name", "key", parentUuid,
            "customTypeId", new HashMap<>()));

        final CategorySyncStatistics syncStatistics = categorySync.sync(categoryDrafts).toCompletableFuture().join();
        assertThat(syncStatistics.getCreated()).isEqualTo(0);
        assertThat(syncStatistics.getFailed()).isEqualTo(1);
        assertThat(syncStatistics.getUpdated()).isEqualTo(0);
        assertThat(syncStatistics.getProcessed()).isEqualTo(1);
        assertThat(syncStatistics.getReportMessage()).isEqualTo(
            "Summary: 1 categories were processed in total "
                + "(0 created, 0 updated and 1 categories failed to sync).");
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).isEqualTo(format("Failed to resolve references on CategoryDraft with"
            + " key:'key'. Reason: %s: Failed to resolve parent reference on CategoryDraft"
            + " with key:'key'. Reason: Found a UUID in the id field. Expecting a key without a UUID"
            + " value. If you want to allow UUID values for reference keys, please use the setAllowUuidKeys(true)"
            + " option in the sync options.", ReferenceResolutionException.class.getCanonicalName()));
        assertThat(errorCallBackExceptions).hasSize(1);
        assertThat(errorCallBackExceptions.get(0)).isExactlyInstanceOf(ReferenceResolutionException.class);
    }

    @Test
    public void sync_WithExistingCategoryButWithNullParentReference_ShouldFailSync() {
        final CategorySync categorySync = new CategorySync(categorySyncOptions, getMockTypeService(),
            getMockCategoryService());
        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();

        categoryDrafts.add(getMockCategoryDraft(Locale.ENGLISH, "name", "key", null,
            "customTypeId", new HashMap<>()));

        final CategorySyncStatistics syncStatistics = categorySync.sync(categoryDrafts).toCompletableFuture().join();
        assertThat(syncStatistics.getCreated()).isEqualTo(0);
        assertThat(syncStatistics.getFailed()).isEqualTo(1);
        assertThat(syncStatistics.getUpdated()).isEqualTo(0);
        assertThat(syncStatistics.getProcessed()).isEqualTo(1);
        assertThat(syncStatistics.getReportMessage()).isEqualTo(
            "Summary: 1 categories were processed in total "
                + "(0 created, 0 updated and 1 categories failed to sync).");
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).isEqualTo(format("Failed to resolve references on CategoryDraft with"
            + " key:'key'. Reason: %s: Failed to resolve parent reference on CategoryDraft"
            + " with key:'key'. Reason: Key is blank (null/empty) on both expanded reference object"
            + " and reference id field.", ReferenceResolutionException.class.getCanonicalName()));
        assertThat(errorCallBackExceptions).hasSize(1);
        assertThat(errorCallBackExceptions.get(0)).isExactlyInstanceOf(ReferenceResolutionException.class);
    }

    @Test
    public void sync_WithExistingCategoryButWithEmptyParentReference_ShouldFailSync() {
        final CategorySync categorySync = new CategorySync(categorySyncOptions, getMockTypeService(),
            getMockCategoryService());
        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();

        categoryDrafts.add(getMockCategoryDraft(Locale.ENGLISH, "name", "key", "",
            "customTypeId", new HashMap<>()));

        final CategorySyncStatistics syncStatistics = categorySync.sync(categoryDrafts).toCompletableFuture().join();
        assertThat(syncStatistics.getCreated()).isEqualTo(0);
        assertThat(syncStatistics.getFailed()).isEqualTo(1);
        assertThat(syncStatistics.getUpdated()).isEqualTo(0);
        assertThat(syncStatistics.getProcessed()).isEqualTo(1);
        assertThat(syncStatistics.getReportMessage()).isEqualTo(
            "Summary: 1 categories were processed in total "
                + "(0 created, 0 updated and 1 categories failed to sync).");
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).isEqualTo(format("Failed to resolve references on CategoryDraft with"
            + " key:'key'. Reason: %s: Failed to resolve parent reference on CategoryDraft"
            + " with key:'key'. Reason: Key is blank (null/empty) on both expanded reference object"
            + " and reference id field.", ReferenceResolutionException.class.getCanonicalName()));
        assertThat(errorCallBackExceptions).hasSize(1);
        assertThat(errorCallBackExceptions.get(0)).isExactlyInstanceOf(ReferenceResolutionException.class);
    }

    @Test
    public void sync_WithExistingCategoryButWithEmptyCustomTypeReference_ShouldFailSync() {
        final CategorySync categorySync = new CategorySync(categorySyncOptions, getMockTypeService(),
            getMockCategoryService());
        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();

        categoryDrafts.add(getMockCategoryDraft(Locale.ENGLISH, "name", "key", "parentKey",
            "", new HashMap<>()));

        final CategorySyncStatistics syncStatistics = categorySync.sync(categoryDrafts).toCompletableFuture().join();
        assertThat(syncStatistics.getCreated()).isEqualTo(0);
        assertThat(syncStatistics.getFailed()).isEqualTo(1);
        assertThat(syncStatistics.getUpdated()).isEqualTo(0);
        assertThat(syncStatistics.getProcessed()).isEqualTo(1);
        assertThat(syncStatistics.getReportMessage()).isEqualTo(
            "Summary: 1 categories were processed in total "
                + "(0 created, 0 updated and 1 categories failed to sync).");
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).isEqualTo(format("Failed to resolve references on CategoryDraft with"
            + " key:'key'. Reason: %s: Failed to resolve custom type reference on "
            + "CategoryDraft with key:'key'. Reason: Reference 'id' field value is blank (null/"
            + "empty).", ReferenceResolutionException.class.getCanonicalName()));
        assertThat(errorCallBackExceptions).hasSize(1);
        assertThat(errorCallBackExceptions.get(0)).isExactlyInstanceOf(CompletionException.class);
        assertThat(errorCallBackExceptions.get(0).getCause()).isExactlyInstanceOf(ReferenceResolutionException.class);
    }

    @Test
    public void sync_WithNotAllowedUuidCustomTypeKey_ShouldFailSync() {
        final CategorySync categorySync = new CategorySync(categorySyncOptions, getMockTypeService(),
            getMockCategoryService());
        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();

        final String uuidCustomTypeKey = UUID.randomUUID().toString();
        categoryDrafts.add(getMockCategoryDraft(Locale.ENGLISH, "name", "key", "parentKey",
            uuidCustomTypeKey, new HashMap<>()));

        final CategorySyncStatistics syncStatistics = categorySync.sync(categoryDrafts).toCompletableFuture().join();
        assertThat(syncStatistics.getCreated()).isEqualTo(0);
        assertThat(syncStatistics.getFailed()).isEqualTo(1);
        assertThat(syncStatistics.getUpdated()).isEqualTo(0);
        assertThat(syncStatistics.getProcessed()).isEqualTo(1);
        assertThat(syncStatistics.getReportMessage()).isEqualTo(
            "Summary: 1 categories were processed in total "
                + "(0 created, 0 updated and 1 categories failed to sync).");
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).isEqualTo(format("Failed to resolve references on CategoryDraft with"
            + " key:'key'. Reason: %s: Failed to resolve custom type reference on "
            + "CategoryDraft with key:'key'. Reason: Found a UUID in the id field. Expecting a key"
            + " without a UUID value. If you want to allow UUID values for reference keys, please use the"
            + " setAllowUuidKeys(true) option in the sync options.",
            ReferenceResolutionException.class.getCanonicalName()));
        assertThat(errorCallBackExceptions).hasSize(1);
        assertThat(errorCallBackExceptions.get(0)).isExactlyInstanceOf(CompletionException.class);
        assertThat(errorCallBackExceptions.get(0).getCause()).isExactlyInstanceOf(ReferenceResolutionException.class);
    }

    @Test
    public void sync_WithAllowedUuidCustomTypeKey_ShouldSync() {
        categorySyncOptions = CategorySyncOptionsBuilder.of(mock(SphereClient.class))
                                                        .setAllowUuidKeys(true)
                                                        .build();
        final CategorySync categorySync = new CategorySync(categorySyncOptions, getMockTypeService(),
            getMockCategoryService());
        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();

        final String uuidCustomTypeKey = UUID.randomUUID().toString();
        categoryDrafts.add(getMockCategoryDraft(Locale.ENGLISH, "name", "key", "parentKey",
            uuidCustomTypeKey, new HashMap<>()));

        final CategorySyncStatistics syncStatistics = categorySync.sync(categoryDrafts).toCompletableFuture().join();
        assertThat(syncStatistics.getCreated()).isEqualTo(0);
        assertThat(syncStatistics.getFailed()).isEqualTo(0);
        assertThat(syncStatistics.getUpdated()).isEqualTo(1);
        assertThat(syncStatistics.getProcessed()).isEqualTo(1);
        assertThat(syncStatistics.getReportMessage()).isEqualTo(
            "Summary: 1 categories were processed in total "
                + "(0 created, 1 updated and 0 categories failed to sync).");
        assertThat(errorCallBackMessages).hasSize(0);
        assertThat(errorCallBackExceptions).hasSize(0);
    }

}
