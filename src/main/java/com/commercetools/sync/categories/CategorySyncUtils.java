package com.commercetools.sync.categories;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.commands.UpdateAction;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

import static com.commercetools.sync.categories.CategoryUpdateActionsHelper.*;

public class CategorySyncUtils {

    @Nonnull
    public static List<UpdateAction<Category>> buildActions(@Nonnull final Category existingCategory,
                                                            @Nonnull final CategoryDraft newCategory) {
        List<UpdateAction<Category>> updateActions = buildCoreActions(existingCategory, newCategory);
        List<UpdateAction<Category>> assetUpdateActions = buildAssetActions(existingCategory, newCategory);
        updateActions.addAll(assetUpdateActions);
        return updateActions;
    }

    @Nonnull
    public static List<UpdateAction<Category>> buildActions(@Nonnull final Category existingCategory,
                                                            @Nonnull final Category newCategory) {
        List<UpdateAction<Category>> updateActions = buildCoreActions(existingCategory, newCategory);
        List<UpdateAction<Category>> assetUpdateActions = buildAssetActions(existingCategory, newCategory);
        updateActions.addAll(assetUpdateActions);
        return updateActions;
    }

    @Nonnull
    public static List<UpdateAction<Category>> buildCoreActions(@Nonnull final Category existingCategory,
                                                                @Nonnull final CategoryDraft newCategory) {
        List<UpdateAction<Category>> updateActions = new ArrayList<>();
        buildChangeNameUpdateAction(existingCategory, newCategory)
                .map(updateActions::add);

        buildChangeSlugUpdateAction(existingCategory, newCategory)
                .map(updateActions::add);

        buildSetDescriptionUpdateAction(existingCategory, newCategory)
                .map(updateActions::add);

        buildChangeParentUpdateAction(existingCategory, newCategory)
                .map(updateActions::add);

        buildChangeOrderHintUpdateAction(existingCategory, newCategory)
                .map(updateActions::add);

        buildSetMetaTitleUpdateAction(existingCategory, newCategory)
                .map(updateActions::add);

        buildSetMetaDescriptionUpdateAction(existingCategory, newCategory)
                .map(updateActions::add);

        buildSetMetaKeywordsUpdateAction(existingCategory, newCategory)
                .map(updateActions::add);

        updateActions.addAll(buildSetCustomTypeUpdateActions(existingCategory, newCategory));
        return updateActions;
    }

    @Nonnull
    public static List<UpdateAction<Category>> buildCoreActions(@Nonnull final Category existingCategory,
                                                                @Nonnull final Category newCategory) {
        return new ArrayList<>();
    }

    /**
     * - addAsset
     * - removeAsset
     * - changeAssetOrder
     * - changeAssetName
     * - setAssetDescription
     * - setAssetTags
     * - setAssetSources
     * - setAssetCustomType
     * - setAssetCustomField
     *
     * @param existingCategory
     * @param newCategory
     * @return
     */
    @Nonnull
    public static List<UpdateAction<Category>> buildAssetActions(@Nonnull final Category existingCategory,
                                                                 @Nonnull final CategoryDraft newCategory) {
        return new ArrayList<>();
    }

    @Nonnull
    public static List<UpdateAction<Category>> buildAssetActions(@Nonnull final Category existingCategory,
                                                                 @Nonnull final Category newCategory) {
        return new ArrayList<>();
    }
}
