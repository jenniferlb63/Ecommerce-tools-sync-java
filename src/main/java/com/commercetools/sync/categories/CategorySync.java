package com.commercetools.sync.categories;

import com.commercetools.sync.categories.helpers.CategorySyncOptions;
import com.commercetools.sync.categories.helpers.CategorySyncStatistics;
import com.commercetools.sync.categories.utils.CategorySyncUtils;
import com.commercetools.sync.commons.Sync;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.SphereException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static com.commercetools.sync.commons.helpers.BaseSyncStatistics.getStatisticsAsJSONString;
import static java.lang.String.format;

public class CategorySync implements Sync {
    private static final Logger LOGGER = LoggerFactory.getLogger(CategorySync.class);
    private CategorySyncOptions syncOptions;
    private CategorySyncStatistics statistics;

    public CategorySync(@Nonnull final CategorySyncOptions syncOptions) {
        this.syncOptions = syncOptions;
    }

    /**
     * Traverses a {@link List} of {@link CategoryDraft} objects and tries to fetch a category, from the CTP project with
     * the configuration stored in the {@Code syncOptions} instance of this class, using the external id. If a category exists,
     * this category is synced to be the same as the new category draft in this list. If no category exist with such external id,
     * a new category, identical to this new category draft  is created.
     * <p>
     * The {@code statistics} instance is updated accordingly to whether the CTP request was carried out successfully or not.
     * If an exception was thrown on executing the request to CTP,
     * the optional error callback specified in the {@code syncOptions} is called.
     *
     * @param categoryDrafts the list of new category drafts to sync to the CTP project.
     * @param <T>            the type of the resource draft. In this case CategoryDraft.
     */
    @Override
    public <T> void syncDrafts(@Nonnull final List<T> categoryDrafts) {
        statistics = new CategorySyncStatistics();
        LOGGER.info(format("About to sync %d category drafts into CTP project with key '%s'."
                , categoryDrafts.size(), getSyncOptions().getClientConfig().getProjectKey()));
        for (int i = 0; i < categoryDrafts.size(); i++) {
            try {
                final CategoryDraft categoryDraft = (CategoryDraft) categoryDrafts.get(i);
                if (categoryDraft != null) {
                    getStatistics().incrementProcessed();
                    final String externalId = categoryDraft.getExternalId();
                    if (externalId != null) {
                        final Category oldCategory = fetchCategory(externalId);
                        if (oldCategory != null) {
                            syncCategories(oldCategory, categoryDraft);
                        } else {
                            createCategory(categoryDraft);
                        }
                    } else {
                        failSync(format("CategoryDraft with name: %s doesn't have an externalId.",
                                categoryDraft.getName().toString()), null);
                    }
                }
            } catch (ClassCastException e) {
                failSync(format("Element at position %d is not an instance of CategoryDraft.", i), e);
            }

        }
        getStatistics().calculateProcessingTime();
    }

    /**
     * Given an external id , this method tries to fetch the existing category from CTP project stored in the {@code syncOptions}
     * instance of this class.
     * <p>
     * The {@code statistics} instance is updated accordingly to whether the CTP request was carried out successfully or not.
     * If an exception was thrown on executing the request to CTP,
     * the optional error callback specified in the {@code syncOptions} is called.
     *
     * @param externalId the external id to fetch the category with.
     */
    public Category fetchCategory(@Nonnull final String externalId) {
        Category oldCategory = null;
        try {
            oldCategory = getSyncOptions().getCategoryService().fetchCategoryByExternalId(externalId);
        } catch (SphereException e) {
            failSync(format("Failed to fetch category with external id" +
                            " '%s' in CTP project with key '%s",
                    externalId, getSyncOptions().getClientConfig().getProjectKey()), e);
        }
        return oldCategory;
    }

    /**
     * Given a {@link CategoryDraft}, this method issues a blocking request to the CTP project defined by the client
     * configuration stored in the {@code syncOptions} instance of this class to create a category with the same fields as
     * this category draft.
     * <p>
     * The {@code statistics} instance is updated accordingly to whether the CTP request was carried out successfully or not.
     * If an exception was thrown on executing the request to CTP,
     * the optional error callback specified in the {@code syncOptions} is called.
     *
     * @param categoryDraft the category draft to create the category from.
     */
    private void createCategory(@Nonnull final CategoryDraft categoryDraft) {
        try {
            getSyncOptions().getCategoryService().createCategory(categoryDraft);
            getStatistics().incrementCreated();
        } catch (SphereException e) {
            failSync(format("Failed to create category with external id" +
                            " '%s' in CTP project with key '%s",
                    categoryDraft.getExternalId(), getSyncOptions().getClientConfig().getProjectKey()), e);
        }
    }

    /**
     * Given an existing {@link Category} and a new {@link CategoryDraft}, this method calculates all the update actions
     * required to synchronize the existing category to be the same as the new one. If there are update actions found, a
     * request is made to CTP to update the existing category, otherwise it doesn't issue a request.
     *
     * @param oldCategory the category which should be updated.
     * @param newCategory the category draft where we get the new data.
     */
    private void syncCategories(@Nonnull final Category oldCategory, @Nonnull final CategoryDraft newCategory) {
        final List<UpdateAction<Category>> updateActions =
                CategorySyncUtils.buildActions(oldCategory, newCategory, getSyncOptions());
        if (!updateActions.isEmpty()) {
            updateCategory(oldCategory, updateActions);
        }
    }

    /**
     * Given a {@link Category} and a {@link List} of {@link UpdateAction} elements, this method issues a blocking request
     * to the CTP project defined by the client configuration stored in the {@code syncOptions} instance of this class to
     * update the specified category with this list of update actions.
     * <p>
     * The {@code statistics} instance is updated accordingly to whether the CTP request was carried out successfully or not.
     * If an exception was thrown on executing the request to CTP,
     * the optional error callback specified in the {@code syncOptions} is called.
     *
     * @param category      the category to update.
     * @param updateActions the list of update actions to update the category with.
     */
    public void updateCategory(@Nonnull final Category category,
                               @Nonnull final List<UpdateAction<Category>> updateActions) {
        try {
            getSyncOptions().getCategoryService().updateCategory(category, updateActions);
            getStatistics().incrementUpdated();
        } catch (SphereException e) {
            failSync(format("Failed to update category with id" +
                            " '%s' in CTP project with key '%s",
                    category.getId(), getSyncOptions().getClientConfig().getProjectKey()), e);
        }
    }

    /**
     * Given a reason message as {@link String} and {@link Throwable} exception, this method calls the optional error
     * callback specified in the {@code syncOptions} and updates the {@code statistics} instance by incrementing the
     * total number of failed resources to sync.
     *
     * @param reason    the reason of failure.
     * @param exception the exception that occurred, if any.
     */
    private void failSync(@Nonnull final String reason, @Nullable final Throwable exception) {
        getSyncOptions().callUpdateActionErrorCallBack(reason, exception);
        getStatistics().incrementFailed();
    }

    /**
     * Builds a JSON String that represents the fields of the {@code statistics} instance.
     *
     * @return a JSON String that represents the fields of the {@code statistics} instance.
     */
    @Override
    @Nonnull
    public String getSummary() {
        return getStatisticsAsJSONString(getStatistics());
    }

    @Override
    public <T> void sync(@Nonnull final List<T> categories) {
        //TODO: SEE GITHUB ISSUE#12
    }

    public CategorySyncOptions getSyncOptions() {
        return syncOptions;
    }

    public CategorySyncStatistics getStatistics() {
        return statistics;
    }
}
