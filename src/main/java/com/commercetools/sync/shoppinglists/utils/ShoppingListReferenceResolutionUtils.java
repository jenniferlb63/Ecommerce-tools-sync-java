package com.commercetools.sync.shoppinglists.utils;

import static com.commercetools.sync.commons.utils.CustomTypeReferenceResolutionUtils.mapToCustomFieldsDraft;
import static com.commercetools.sync.commons.utils.SyncUtils.getResourceIdentifierWithKey;
import static java.util.stream.Collectors.toList;

import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.expansion.ExpansionPath;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.shoppinglists.LineItem;
import io.sphere.sdk.shoppinglists.LineItemDraft;
import io.sphere.sdk.shoppinglists.LineItemDraftBuilder;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;
import io.sphere.sdk.shoppinglists.ShoppingListDraftBuilder;
import io.sphere.sdk.shoppinglists.TextLineItem;
import io.sphere.sdk.shoppinglists.TextLineItemDraft;
import io.sphere.sdk.shoppinglists.TextLineItemDraftBuilder;
import io.sphere.sdk.shoppinglists.queries.ShoppingListQuery;
import io.sphere.sdk.types.Type;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Util class which provides utilities that can be used when syncing shopping lists from a source
 * commercetools project to a target one.
 */
public final class ShoppingListReferenceResolutionUtils {

  /**
   * Returns a {@link List}&lt;{@link ShoppingListDraft}&gt; consisting of the results of applying
   * the mapping from {@link ShoppingList} to {@link ShoppingListDraft} with considering reference
   * resolution.
   *
   * <table>
   *   <caption>Mapping of Reference fields for the reference resolution</caption>
   *   <thead>
   *     <tr>
   *       <th>Reference field</th>
   *       <th>from</th>
   *       <th>to</th>
   *     </tr>
   *   </thead>
   *   <tbody>
   *     <tr>
   *        <td>customer</td>
   *        <td>{@link Reference}&lt;{@link Customer}&gt;</td>
   *        <td>{@link ResourceIdentifier}&lt;{@link Customer}&gt;</td>
   *     </tr>
   *     <tr>
   *        <td>custom.type</td>
   *        <td>{@link Reference}&lt;{@link Type}&gt;</td>
   *        <td>{@link ResourceIdentifier}&lt;{@link Type}&gt;</td>
   *     </tr>
   *     <tr>
   *        <td>lineItems.custom.type</td>
   *        <td>{@link Set}&lt;{@link Reference}&lt;{@link Type}&gt;&gt;</td>
   *        <td>{@link Set}&lt;{@link ResourceIdentifier}&lt;{@link Type}&gt;&gt;</td>
   *     </tr>
   *     <tr>
   *        <td>textLineItems.custom.type</td>
   *        <td>{@link Set}&lt;{@link Reference}&lt;{@link Type}&gt;&gt;</td>
   *        <td>{@link Set}&lt;{@link ResourceIdentifier}&lt;{@link Type}&gt;&gt;</td>
   *     </tr>
   *   </tbody>
   * </table>
   *
   * <p><b>Note:</b> The aforementioned references should contain Id in the map(cache) with a key
   * value. Any reference, which have its id in place and not replaced by the key, it would not be
   * found in the map. In this case, this reference will be considered as existing resources on the
   * target commercetools project and the library will issues an update/create API request without
   * reference resolution.
   *
   * @param shoppingLists the shopping lists without expansion of references.
   * @param referenceIdToKeyCache the instance that manages cache.
   * @return a {@link List} of {@link ShoppingListDraft} built from the supplied {@link List} of
   *     {@link ShoppingList}.
   */
  @Nonnull
  public static List<ShoppingListDraft> mapToShoppingListDrafts(
      @Nonnull final List<ShoppingList> shoppingLists,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {

    return shoppingLists.stream()
        .filter(Objects::nonNull)
        .map(shoppingList -> mapToShoppingListDraft(shoppingList, referenceIdToKeyCache))
        .collect(toList());
  }

  /**
   * Returns a @link ShoppingListDraft} consisting of the result of applying the mapping from {@link
   * ShoppingList} to {@link ShoppingListDraft} with considering reference resolution.
   *
   * <table>
   *   <caption>Mapping of Reference fields for the reference resolution</caption>
   *   <thead>
   *     <tr>
   *       <th>Reference field</th>
   *       <th>from</th>
   *       <th>to</th>
   *     </tr>
   *   </thead>
   *   <tbody>
   *     <tr>
   *        <td>customer</td>
   *        <td>{@link Reference}&lt;{@link Customer}&gt;</td>
   *        <td>{@link ResourceIdentifier}&lt;{@link Customer}&gt;</td>
   *     </tr>
   *     <tr>
   *        <td>custom.type</td>
   *        <td>{@link Reference}&lt;{@link Type}&gt;</td>
   *        <td>{@link ResourceIdentifier}&lt;{@link Type}&gt;</td>
   *     </tr>
   *     <tr>
   *        <td>lineItems.custom.type</td>
   *        <td>{@link Set}&lt;{@link Reference}&lt;{@link Type}&gt;&gt;</td>
   *        <td>{@link Set}&lt;{@link ResourceIdentifier}&lt;{@link Type}&gt;&gt;</td>
   *     </tr>
   *     <tr>
   *        <td>textLineItems.custom.type</td>
   *        <td>{@link Set}&lt;{@link Reference}&lt;{@link Type}&gt;&gt;</td>
   *        <td>{@link Set}&lt;{@link ResourceIdentifier}&lt;{@link Type}&gt;&gt;</td>
   *     </tr>
   *   </tbody>
   * </table>
   *
   * <p><b>Note:</b> The aforementioned references should contain Id in the map(cache) with a key
   * value. Any reference, which have its id in place and not replaced by the key, it would not be
   * found in the map. In this case, this reference will be considered as existing resources on the
   * target commercetools project and the library will issues an update/create API request without
   * reference resolution.
   *
   * @param shoppingList the shopping list without expansion of references.
   * @param referenceIdToKeyCache the instance that manages cache.
   * @return a {@link ShoppingListDraft} built from the supplied {@link ShoppingList}.
   */
  @Nonnull
  public static ShoppingListDraft mapToShoppingListDraft(
      @Nonnull final ShoppingList shoppingList,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {

    return ShoppingListDraftBuilder.of(shoppingList.getName())
        .description(shoppingList.getDescription())
        .key(shoppingList.getKey())
        .customer(getResourceIdentifierWithKey(shoppingList.getCustomer(), referenceIdToKeyCache))
        .slug(shoppingList.getSlug())
        .lineItems(mapToLineItemDrafts(shoppingList.getLineItems(), referenceIdToKeyCache))
        .textLineItems(
            mapToTextLineItemDrafts(shoppingList.getTextLineItems(), referenceIdToKeyCache))
        .custom(mapToCustomFieldsDraft(shoppingList, referenceIdToKeyCache))
        .deleteDaysAfterLastModification(shoppingList.getDeleteDaysAfterLastModification())
        .anonymousId(shoppingList.getAnonymousId())
        .build();
  }

  @Nullable
  @SuppressWarnings("PMD.ReturnEmptyCollectionRatherThanNull")
  private static List<LineItemDraft> mapToLineItemDrafts(
      @Nullable final List<LineItem> lineItems,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {

    if (lineItems == null) {
      return null;
    }

    return lineItems.stream()
        .filter(Objects::nonNull)
        .map(lineItem -> mapToLineItemDraft(lineItem, referenceIdToKeyCache))
        .filter(Objects::nonNull)
        .collect(toList());
  }

  @Nullable
  private static LineItemDraft mapToLineItemDraft(
      @Nonnull final LineItem lineItem,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {

    if (lineItem.getVariant() != null) {
      return LineItemDraftBuilder.ofSku(lineItem.getVariant().getSku(), lineItem.getQuantity())
          .addedAt(lineItem.getAddedAt())
          .custom(mapToCustomFieldsDraft(lineItem.getCustom(), referenceIdToKeyCache))
          .build();
    }

    return null;
  }

  @Nullable
  @SuppressWarnings("PMD.ReturnEmptyCollectionRatherThanNull")
  private static List<TextLineItemDraft> mapToTextLineItemDrafts(
      @Nullable final List<TextLineItem> textLineItems,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {

    if (textLineItems == null) {
      return null;
    }

    return textLineItems.stream()
        .filter(Objects::nonNull)
        .map(textLineItem -> mapToTextLineItemDraft(textLineItem, referenceIdToKeyCache))
        .collect(toList());
  }

  @Nonnull
  private static TextLineItemDraft mapToTextLineItemDraft(
      @Nonnull final TextLineItem textLineItem,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {

    return TextLineItemDraftBuilder.of(textLineItem.getName(), textLineItem.getQuantity())
        .description(textLineItem.getDescription())
        .addedAt(textLineItem.getAddedAt())
        .custom(mapToCustomFieldsDraft(textLineItem.getCustom(), referenceIdToKeyCache))
        .build();
  }

  /**
   * Builds a {@link ShoppingListQuery} for fetching shopping lists from a source CTP project with
   * only the Variants expanded for the sync:
   *
   * <ul>
   *   <li>Variants of the LineItems
   * </ul>
   *
   * <p>Note: Please only use this util if you desire to sync the aforementioned reference from a
   * source commercetools project. Otherwise, it is more efficient to build the query without
   * expansion, if it is not needed, to avoid unnecessarily bigger payloads fetched from the source
   * project.
   *
   * @return the query for fetching shopping lists from the source CTP project without references
   *     expanded except for Variants of the LineItems.
   */
  public static ShoppingListQuery buildShoppingListQuery() {
    return ShoppingListQuery.of().plusExpansionPaths(ExpansionPath.of("lineItems[*].variant"));
  }

  private ShoppingListReferenceResolutionUtils() {}
}
