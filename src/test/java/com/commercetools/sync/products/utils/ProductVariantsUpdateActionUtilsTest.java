package com.commercetools.sync.products.utils;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Image;
import io.sphere.sdk.products.PriceDraft;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.ProductVariantDraftDsl;
import io.sphere.sdk.products.attributes.AttributeDraft;
import io.sphere.sdk.products.commands.updateactions.AddVariant;
import io.sphere.sdk.products.commands.updateactions.ChangeMasterVariant;
import io.sphere.sdk.products.commands.updateactions.RemoveVariant;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.commercetools.sync.products.ProductSyncMockUtils.createProductDraftFromJson;
import static com.commercetools.sync.products.ProductSyncMockUtils.createProductFromJson;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildAddVariantUpdateAction;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildAddVariantUpdateActionFromDraft;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildChangeMasterVariantUpdateAction;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildRemoveVariantUpdateAction;
import static org.assertj.core.api.Assertions.assertThat;

public class ProductVariantsUpdateActionUtilsTest {

    public static final String OLD_PROD_WITH_VARIANTS =
            "com/commercetools/sync/products/utils/productVariantUpdateActionUtils/productOld.json";
    public static final String NEW_PROD_DRAFT_WITH_VARIANTS =
            "com/commercetools/sync/products/utils/productVariantUpdateActionUtils/productDraftNew.json";


    @Test
    public void buildRemoveVariantUpdateAction_removesMissedVariants() throws Exception {
        Product productOld = createProductFromJson(OLD_PROD_WITH_VARIANTS);
        ProductDraft productDraftNew = createProductDraftFromJson(NEW_PROD_DRAFT_WITH_VARIANTS);

        List<UpdateAction<Product>> updateActions = buildRemoveVariantUpdateAction(productOld, productDraftNew);
        assertThat(updateActions).containsExactlyInAnyOrder(RemoveVariant.of(2), RemoveVariant.of(3));
    }

    @Test
    public void buildAddVariantUpdateAction_addsNewVariants() throws Exception {
        Product productOld = createProductFromJson(OLD_PROD_WITH_VARIANTS);
        ProductDraft productDraftNew = createProductDraftFromJson(NEW_PROD_DRAFT_WITH_VARIANTS);

        ProductVariantDraft draft5 = productDraftNew.getVariants().get(1);
        ProductVariantDraft draft6 = productDraftNew.getVariants().get(2);

        List<AddVariant> updateActions = buildAddVariantUpdateAction(productOld, productDraftNew);
        assertThat(updateActions).containsExactlyInAnyOrder(
                buildAddVariantUpdateActionFromDraft(draft5),
                buildAddVariantUpdateActionFromDraft(draft6));
    }

    @Test
    public void buildChangeMasterVariantUpdateAction_changesMasterVariant() throws Exception {
        Product productOld = createProductFromJson(OLD_PROD_WITH_VARIANTS);
        ProductDraft productDraftNew = createProductDraftFromJson(NEW_PROD_DRAFT_WITH_VARIANTS);

        Optional<ChangeMasterVariant> changeMasterVariant = buildChangeMasterVariantUpdateAction(productOld, productDraftNew);
        assertThat(changeMasterVariant).isNotEmpty();
        assertThat(changeMasterVariant.orElse(null))
                .isEqualTo(ChangeMasterVariant.ofSku(productDraftNew.getMasterVariant().getSku(), true));
    }

    @Test
    public void buildAddVariantUpdateActionFromDraft_returnsProperties() throws Exception {
        List<AttributeDraft> attributeList = Collections.emptyList();
        List<PriceDraft> priceList = Collections.emptyList();
        List<Image> imageList = Collections.emptyList();
        ProductVariantDraftDsl draft = ProductVariantDraftBuilder.of()
                .attributes(attributeList)
                .prices(priceList)
                .sku("testSKU")
                .key("testKey")
                .images(imageList)
                .build();

        AddVariant addVariant = buildAddVariantUpdateActionFromDraft(draft);
        assertThat(addVariant.getAttributes()).isSameAs(attributeList);
        assertThat(addVariant.getPrices()).isSameAs(priceList);
        assertThat(addVariant.getSku()).isEqualTo("testSKU");
        assertThat(addVariant.getKey()).isEqualTo("testKey");
        assertThat(addVariant.getImages()).isSameAs(imageList);
    }

}