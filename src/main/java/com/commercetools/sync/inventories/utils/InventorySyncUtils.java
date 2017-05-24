package com.commercetools.sync.inventories.utils;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.inventories.InventorySyncOptions;
import com.commercetools.sync.services.TypeService;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.commercetools.sync.commons.utils.CustomUpdateActionUtils.buildCustomUpdateActions;
import static com.commercetools.sync.inventories.utils.InventoryUpdateActionUtils.buildChangeQuantityAction;
import static com.commercetools.sync.inventories.utils.InventoryUpdateActionUtils.buildSetExpectedDeliveryAction;
import static com.commercetools.sync.inventories.utils.InventoryUpdateActionUtils.buildSetRestockableInDaysAction;
import static com.commercetools.sync.inventories.utils.InventoryUpdateActionUtils.buildSetSupplyChannelAction;
import static java.util.stream.Collectors.toList;

/**
 * This class provides factory methods for assembling update actions of inventory entries.
 */
public final class InventorySyncUtils {

    private InventorySyncUtils() {
        throw new AssertionError();
    }

    /**
     * Returns list of {@link UpdateAction} that needs to be performed on {@code oldEntry} resource so that it will
     * be synced with {@code newEntry} draft.
     *
     * @param oldEntry entry which data should be updated
     * @param newEntry draft that contain current data (that should be applied to {@code oldEntry}
     * @param syncOptions the sync options wrapper which contains options related to the sync process supplied by the
     *                    user (see {@link BaseSyncOptions} for more info)
     * @param typeService responsible for fetching the key of the old resource type from its cache
     * @return list containing {@link UpdateAction} that need to be performed on {@code oldEntry} resource so
     *      that it will be synced with {@code newEntry} or empty list when both entries are already synced
     */
    @Nonnull
    public static List<UpdateAction<InventoryEntry>> buildActions(@Nonnull final InventoryEntry oldEntry,
                                                                  @Nonnull final InventoryEntryDraft newEntry,
                                                                  @Nonnull final InventorySyncOptions syncOptions,
                                                                  @Nonnull final TypeService typeService) {
        final List<UpdateAction<InventoryEntry>> actions =
                Stream.of(
                        buildChangeQuantityAction(oldEntry, newEntry),
                        buildSetRestockableInDaysAction(oldEntry, newEntry),
                        buildSetExpectedDeliveryAction(oldEntry, newEntry),
                        buildSetSupplyChannelAction(oldEntry, newEntry))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toList());
        actions.addAll(buildCustomUpdateActions(oldEntry, newEntry, syncOptions, typeService));
        return actions;
    }
}
