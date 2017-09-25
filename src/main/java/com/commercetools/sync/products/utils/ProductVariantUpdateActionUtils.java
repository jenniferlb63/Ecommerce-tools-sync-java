package com.commercetools.sync.products.utils;

import com.commercetools.sync.commons.exceptions.BuildUpdateActionException;
import com.commercetools.sync.products.AttributeMetaData;
import com.commercetools.sync.products.ProductSyncOptions;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Image;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.attributes.Attribute;
import io.sphere.sdk.products.attributes.AttributeDraft;
import io.sphere.sdk.products.commands.updateactions.AddExternalImage;
import io.sphere.sdk.products.commands.updateactions.RemoveImage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.commercetools.sync.commons.utils.CollectionUtils.filterCollection;
import static com.commercetools.sync.products.utils.ProductVariantAttributeUpdateActionUtils.buildProductVariantAttributeUpdateAction;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

// TODO: Add JAVADOC AND TESTS
public final class ProductVariantUpdateActionUtils {
    private static final String FAILED_TO_BUILD_ATTRIBUTE_UPDATE_ACTION = "Failed to build a "
        + "setAttribute/setAttributeInAllVariants update action for the attribute with the name '%s' in the "
        + "ProductVariantDraft with key '%s' on the product with key '%s'. Reason: %s";
    private static final String FAILED_TO_BUILD_ATTRIBUTE_UPDATE_ACTIONS = "Failed to build attribute update actions"
        + "for the ProductVariantDraft with key '%s' on the product with key '%s'. Reason: %s";
    private static final String FAILED_TO_BUILD_VARIANT_IMAGES_UPDATE_ACTIONS = "Failed to build "
        + "addExternalImage/removeImage update actions for the ProductVariantDraft with key '%s' on the product with"
        + " key '%s'. Reason: %s";
    private static final String BLANK_VARIANT_SKU = "ProductVariant with the key '%s' has a blank SKU.";
    private static final String NULL_PRODUCT_VARIANT_ATTRIBUTE = "AttributeDraft is null.";

    /**
     * Compares the attributes of a {@link ProductVariantDraft} and a {@link ProductVariant} to build either
     * {@link io.sphere.sdk.products.commands.updateactions.SetAttribute} or
     * {@link io.sphere.sdk.products.commands.updateactions.SetAttributeInAllVariants} update actions.
     * TODO: Add JavaDoc
     *
     * @param productKey         TODO
     * @param oldProductVariant  TODO
     * @param newProductVariant  TODO
     * @param attributesMetaData TODO
     * @param syncOptions        TODO
     * @return TODO
     */
    @Nonnull
    public static List<UpdateAction<Product>> buildProductVariantAttributesUpdateActions(
        @Nullable final String productKey,
        @Nonnull final ProductVariant oldProductVariant,
        @Nonnull final ProductVariantDraft newProductVariant,
        @Nonnull final Map<String, AttributeMetaData> attributesMetaData,
        @Nonnull final ProductSyncOptions syncOptions) {

        final List<UpdateAction<Product>> updateActions = new ArrayList<>();
        final List<AttributeDraft> newProductVariantAttributes = newProductVariant.getAttributes();
        if (newProductVariantAttributes == null) {
            return updateActions;
        }

        // TODO: NEED TO HANDLE REMOVED ATTRIBUTES FROM OLD PRODUCT VARIANT.

        final String oldProductVariantSku = oldProductVariant.getSku();
        if (isBlank(oldProductVariantSku)) {
            final String nullSkuErrorMessage = format(BLANK_VARIANT_SKU, oldProductVariant.getKey());
            final String errorMessage = format(FAILED_TO_BUILD_ATTRIBUTE_UPDATE_ACTIONS, newProductVariant.getKey(),
                productKey, nullSkuErrorMessage);
            syncOptions.applyErrorCallback(errorMessage, new BuildUpdateActionException(errorMessage));
            return updateActions;
        }

        for (AttributeDraft newProductVariantAttribute : newProductVariantAttributes) {
            if (newProductVariantAttribute == null) {
                final String errorMessage = format(FAILED_TO_BUILD_ATTRIBUTE_UPDATE_ACTION, null,
                    newProductVariant.getKey(), productKey, NULL_PRODUCT_VARIANT_ATTRIBUTE);
                syncOptions.applyErrorCallback(errorMessage, new BuildUpdateActionException(errorMessage));
                continue;
            }

            final String newProductVariantAttributeName = newProductVariantAttribute.getName();
            final Optional<Attribute> oldProductVariantAttributeOptional = oldProductVariant
                .findAttribute(newProductVariantAttributeName);

            final Attribute oldProductVariantAttribute = oldProductVariantAttributeOptional.orElse(null);
            final AttributeMetaData attributeMetaData = attributesMetaData.get(newProductVariantAttributeName);

            try {
                final Optional<UpdateAction<Product>> variantAttributeUpdateActionOptional =
                    buildProductVariantAttributeUpdateAction(oldProductVariantSku, oldProductVariantAttribute,
                        newProductVariantAttribute, attributeMetaData);
                variantAttributeUpdateActionOptional.ifPresent(updateActions::add);
            } catch (@Nonnull final BuildUpdateActionException buildUpdateActionException) {
                final String errorMessage = format(FAILED_TO_BUILD_ATTRIBUTE_UPDATE_ACTION,
                    newProductVariantAttributeName, newProductVariant.getKey(), productKey,
                    buildUpdateActionException.getMessage());
                syncOptions.applyErrorCallback(errorMessage, buildUpdateActionException);
            }
        }
        return updateActions;
    }


    /**
     * Compares the prices of a {@link ProductVariantDraft} and a {@link ProductVariant}.
     * TODO: Add JavaDoc
     *
     * @param oldProductVariant TODO
     * @param newProductVariant TODO
     * @param syncOptions       TODO
     * @return TODO
     */
    @Nonnull
    public static List<UpdateAction<Product>> buildProductVariantPricesUpdateActions(
        @Nonnull final ProductVariant oldProductVariant,
        @Nonnull final ProductVariantDraft newProductVariant,
        @Nonnull final ProductSyncOptions syncOptions) {
        //TODO: IMPLEMENTATION GITHUB ISSUE#99
        return Collections.emptyList();
    }

    /**
     * Compares the images of a {@link ProductVariantDraft} and a {@link ProductVariant}.
     * TODO: Add JavaDoc..
     *
     * @param productKey        TODO
     * @param oldProductVariant TODO
     * @param newProductVariant TODO
     * @param syncOptions       TODO
     * @return TODO
     */
    @Nonnull
    public static List<UpdateAction<Product>> buildProductVariantImagesUpdateActions(
        @Nullable final String productKey,
        @Nonnull final ProductVariant oldProductVariant,
        @Nonnull final ProductVariantDraft newProductVariant,
        @Nonnull final ProductSyncOptions syncOptions) {
        final List<UpdateAction<Product>> updateActions = new ArrayList<>();

        final String oldProductVariantSku = oldProductVariant.getSku();
        if (isBlank(oldProductVariantSku)) {
            final String nullSkuErrorMessage = format(BLANK_VARIANT_SKU, oldProductVariant.getKey());
            final String errorMessage = format(FAILED_TO_BUILD_VARIANT_IMAGES_UPDATE_ACTIONS,
                newProductVariant.getKey(), productKey, nullSkuErrorMessage);
            syncOptions.applyErrorCallback(errorMessage, new BuildUpdateActionException(errorMessage));
            return updateActions;
        }

        final List<Image> oldProductVariantImages = oldProductVariant.getImages();
        final List<Image> newProductVariantImages = newProductVariant.getImages();
        final Map<String, Image> oldUrlToImageMap = oldProductVariantImages.stream()
                                                                           .collect(Collectors.toMap(Image::getUrl,
                                                                               image -> image));
        final Map<String, Image> newUrlToImageMap = newProductVariantImages.stream()
                                                                           .collect(Collectors.toMap(Image::getUrl,
                                                                               image -> image));

        filterCollection(oldProductVariantImages, oldVariantImage ->
            newUrlToImageMap.get(oldVariantImage.getUrl()) == null)
            .forEach(oldImage ->
                updateActions.add(RemoveImage.ofSku(oldProductVariant.getSku(), oldImage, true)));

        filterCollection(newProductVariantImages, newVariantImage ->
            oldUrlToImageMap.get(newVariantImage.getUrl()) == null)
            .forEach(newImage ->
                updateActions.add(AddExternalImage.ofSku(oldProductVariant.getSku(), newImage, true)));
        return updateActions;
    }
}
