package com.commercetools.sync.integration.commons;


import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.queries.CategoryQuery;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import static com.commercetools.sync.commons.utils.CtpQueryUtils.queryAll;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_KEY;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategories;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategoriesCustomType;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.deleteAllCategories;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getCategoryDraftsWithPrefix;
import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypes;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static org.assertj.core.api.Assertions.assertThat;

public class CtpQueryUtilsIT {

    /**
     * Delete all categories and types from target project. Then create custom types for target CTP project categories.
     */
    @BeforeClass
    public static void setup() {
        deleteAllCategories(CTP_TARGET_CLIENT);
        deleteTypes(CTP_TARGET_CLIENT);
        createCategoriesCustomType(OLD_CATEGORY_CUSTOM_TYPE_KEY, Locale.ENGLISH, "anyName", CTP_TARGET_CLIENT);
    }

    /**
     * Deletes Categories and Types from target CTP projects, then it populates target CTP project with category test
     * data.
     */
    @Before
    public void setupTest() {
        deleteAllCategories(CTP_TARGET_CLIENT);
    }

    /**
     * Cleans up the target test data that were built in this test class.
     */
    @AfterClass
    public static void tearDown() {
        deleteAllCategories(CTP_TARGET_CLIENT);
        deleteTypes(CTP_TARGET_CLIENT);
    }

    @Test
    public void queryAll_WithKeyCollectorConsumerOn100CategoriesWithCustomPageSize_ShouldCollectKeys() {
        final int numberOfCategories = 100;
        createCategories(CTP_TARGET_CLIENT, getCategoryDraftsWithPrefix(Locale.ENGLISH, "new",
            null, numberOfCategories));
        final List<String> categoryKeys = new ArrayList<>();

        final Consumer<List<Category>> categoryPageConsumer = categoryPageResults ->
            categoryPageResults.forEach(category -> categoryKeys.add(category.getKey()));

        queryAll(CTP_TARGET_CLIENT, CategoryQuery.of(), categoryPageConsumer, 10).toCompletableFuture().join();
        assertThat(categoryKeys).hasSize(numberOfCategories);
    }

    @Test
    public void queryAll_WithKeyCollectorConsumerOn600Categories_ShouldCollectKeys() {
        final int numberOfCategories = 600;
        createCategories(CTP_TARGET_CLIENT, getCategoryDraftsWithPrefix(Locale.ENGLISH, "new",
            null, numberOfCategories));
        final List<String> categoryKeys = new ArrayList<>();

        final Consumer<List<Category>> categoryPageConsumer = categoryPageResults ->
            categoryPageResults.forEach(category -> categoryKeys.add(category.getKey()));

        queryAll(CTP_TARGET_CLIENT, CategoryQuery.of(), categoryPageConsumer).toCompletableFuture().join();
        assertThat(categoryKeys).hasSize(numberOfCategories);
    }

    @Test
    public void queryAll_WithKeyCollectorCallbackOn600Categories_ShouldFetchAllCategories() {
        final int numberOfCategories = 600;
        createCategories(CTP_TARGET_CLIENT, getCategoryDraftsWithPrefix(Locale.ENGLISH, "new",
            null, numberOfCategories));

        final List<List<Category>> categoryPages =
            queryAll(CTP_TARGET_CLIENT, CategoryQuery.of(), (categories -> categories)).toCompletableFuture().join();
        assertThat(categoryPages).hasSize(2);
        assertThat(categoryPages.get(0)).hasSize(500);
        assertThat(categoryPages.get(1)).hasSize(100);
    }

}
