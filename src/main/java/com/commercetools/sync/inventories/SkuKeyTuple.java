package com.commercetools.sync.inventories;

import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * Helper class that holds {@code sku} of {@link io.sphere.sdk.inventory.InventoryEntry} and the {@code key} of the
 * {@link io.sphere.sdk.channels.Channel} it belongs to. Presence of sku and key in created instance is similar as for
 * inventory entry resource: supply channel key can be {@code null} value since it is optional in inventory entry,
 * but sku has to be not null neither not empty.
 * Can be used as key in Maps.
 */
final class SkuKeyTuple {

    static final String SKU_NOT_SET_MESSAGE = "Can't create SkuKeyTuple instance of inventory entry with no SKU";

    private final String sku;

    @Nullable
    private final String key;

    private SkuKeyTuple(final String sku, final String key) {
        this.sku = sku;
        this.key = key;
    }

    /**
     * Returns new {@link SkuKeyTuple} of given {@link InventoryEntry}. If contains
     * {@link io.sphere.sdk.models.Reference} to supply channel, then reference should be expanded, so that key is
     * taken from it. If reference is null or is not expanded it's result in {@code null} key value.
     * @param existingEntry must have sku set and not empty
     * @return new instance of {@link SkuKeyTuple}
     * @throws IllegalArgumentException when sku of {@code existingEntry} is null or empty string
     */
    static SkuKeyTuple of(@Nonnull final InventoryEntry existingEntry) {
        final String sku = existingEntry.getSku();
        if (isEmpty(sku)) {
            throw new IllegalArgumentException(SKU_NOT_SET_MESSAGE);
        }
        if (existingEntry.getSupplyChannel() != null && existingEntry.getSupplyChannel().getObj() != null) {
            return new SkuKeyTuple(sku, existingEntry.getSupplyChannel().getObj().getKey());
        } else {
            return new SkuKeyTuple(sku, null);
        }
    }

    /**
     * Returns new {@link SkuKeyTuple} of given {@link InventoryEntryDraft}. Key value would result in:
     * <ul>
     *     <li>
     *         referenced id - when supply channel {@link io.sphere.sdk.models.Reference} is present but not expanded
     *     </li>
     *     <li>
     *         referenced object's key - when supply channel {@link io.sphere.sdk.models.Reference} is present and
     *         expanded
     *     </li>
     *     <li>{@code null} - otherwise</li>
     * </ul>
     * @param newEntryDraft must have sku set and not empty
     * @return new instance of {@link SkuKeyTuple}
     * @throws IllegalArgumentException when sku of {@code existingEntry} is null or empty string
     */
    static SkuKeyTuple of(@Nonnull final InventoryEntryDraft newEntryDraft) {
        final String sku = newEntryDraft.getSku();
        if (isEmpty(sku)) {
            throw new IllegalArgumentException(SKU_NOT_SET_MESSAGE);
        }
        String key = null;
        if (newEntryDraft.getSupplyChannel() != null) {
            if (newEntryDraft.getSupplyChannel().getObj() != null) {
                key = newEntryDraft.getSupplyChannel().getObj().getKey();
            } else {
                key = newEntryDraft.getSupplyChannel().getId();
            }
        }
        return new SkuKeyTuple(sku, key);
    }

    @Nonnull
    String getSku() {
        return sku;
    }

    @Nullable
    String getKey() {
        return key;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        SkuKeyTuple that = (SkuKeyTuple) obj;

        if (!getSku().equals(that.getSku())) {
            return false;
        }
        return getKey() != null ? getKey().equals(that.getKey()) : that.getKey() == null;
    }

    @Override
    public int hashCode() {
        int result = getSku().hashCode();
        result = 31 * result ^ (getKey() != null ? getKey().hashCode() : 0);
        return result;
    }
}
