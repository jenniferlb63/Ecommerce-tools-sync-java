package com.commercetools.sync.inventories;

import com.commercetools.sync.inventories.helpers.InventorySyncStatistics;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.ChannelDraft;
import io.sphere.sdk.channels.ChannelRole;
import io.sphere.sdk.channels.commands.ChannelCreateCommand;
import io.sphere.sdk.channels.queries.ChannelQuery;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.InventoryEntryDraftBuilder;
import io.sphere.sdk.inventory.commands.InventoryEntryCreateCommand;
import io.sphere.sdk.models.Reference;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import static com.commercetools.sync.commons.utils.SphereClientUtils.getCtpClientOfSourceProject;
import static com.commercetools.sync.commons.utils.SphereClientUtils.getCtpClientOfTargetProject;
import static com.commercetools.sync.inventories.InventoryIntegrationTestUtils.*;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contains integration tests of inventory sync.
 */
public class InventorySyncTest {

    private static final String SKU_1 = "100000";
    private static final String SKU_2 = "200000";

    private static final Long QUANTITY_ON_STOCK_1 = 1L;
    private static final Long QUANTITY_ON_STOCK_2 = 2L;

    private static final Integer RESTOCKABLE_IN_DAYS_1 = 1;
    private static final Integer RESTOCKABLE_IN_DAYS_2 = 2;

    private static final ZonedDateTime EXPECTED_DELIVERY_1 =
        ZonedDateTime.of(2017, 4, 1, 10, 0, 0, 0, ZoneId.of("UTC"));
    private static final ZonedDateTime EXPECTED_DELIVERY_2 =
        ZonedDateTime.of(2017, 5, 1, 20, 0, 0, 0, ZoneId.of("UTC"));

    private static final String SUPPLY_CHANNEL_KEY_1 = "channel-key_1";
    private static final String SUPPLY_CHANNEL_KEY_2 = "channel-key_2";

    private BlockingSphereClient targetProjectClient;
    private BlockingSphereClient sourceProjectClient;

    @Before
    public void setup() {
        this.sourceProjectClient = getCtpClientOfSourceProject().getClient();
        this.targetProjectClient = getCtpClientOfTargetProject().getClient();
        deleteInventoriesAndSupplyChannels();
        populateTargetProject();
    }

    @AfterClass
    public static void cleanup() {
        deleteInventoriesAndSupplyChannels();
    }

    @Test
    public void syncDrafts_WithUpdatedDraft_ShouldUpdateEntryInCtp() {
        //assert ctp entry before sync
        Optional<InventoryEntry> existingEntry =
            getInventoryEntryBySkuAndSupplyChannel(targetProjectClient, SKU_1, null);
        assertThat(existingEntry).isNotEmpty();
        assertValues(existingEntry.get(), QUANTITY_ON_STOCK_1, EXPECTED_DELIVERY_1, RESTOCKABLE_IN_DAYS_1);

        //prepare sync data
        final InventoryEntryDraft draftToSync = InventoryEntryDraftBuilder
            .of(SKU_1, QUANTITY_ON_STOCK_2, EXPECTED_DELIVERY_2, RESTOCKABLE_IN_DAYS_2, null)
            .build();
        final InventorySyncOptions options = InventorySyncOptionsBuilder.of(getCtpClientOfTargetProject())
            .build();

        //sync and assert sync result
        final InventorySync syncer = new InventorySync(options);
        final InventorySyncStatistics syncResult = syncer.syncDrafts(singletonList(draftToSync))
            .toCompletableFuture()
            .join();
        assertStatistics(syncResult, 1, 0, 1, 0);

        //assert ctp entry after sync
        existingEntry = getInventoryEntryBySkuAndSupplyChannel(targetProjectClient, SKU_1, null);
        assertThat(existingEntry).isNotEmpty();
        assertValues(existingEntry.get(), QUANTITY_ON_STOCK_2, EXPECTED_DELIVERY_2, RESTOCKABLE_IN_DAYS_2);
    }

    @Test
    public void syncDrafts_WithNewDraft_ShouldCreateDraftInCtp() {
        //assert ctp entry before sync
        Optional<InventoryEntry> existingEntry =
            getInventoryEntryBySkuAndSupplyChannel(targetProjectClient, SKU_2, null);
        assertThat(existingEntry).isEmpty();

        //prepare sync data
        final InventoryEntryDraft draftToSync = InventoryEntryDraftBuilder
            .of(SKU_2, QUANTITY_ON_STOCK_2, EXPECTED_DELIVERY_2, RESTOCKABLE_IN_DAYS_2, null)
            .build();
        final InventorySyncOptions options = InventorySyncOptionsBuilder.of(getCtpClientOfTargetProject())
            .build();

        //sync and assert sync result
        final InventorySync syncer = new InventorySync(options);
        final InventorySyncStatistics syncResult = syncer.syncDrafts(singletonList(draftToSync))
            .toCompletableFuture()
            .join();
        assertStatistics(syncResult, 1, 1, 0, 0);

        //assert ctp entry after sync
        existingEntry = getInventoryEntryBySkuAndSupplyChannel(targetProjectClient, SKU_2, null);
        assertThat(existingEntry).isNotEmpty();
        assertValues(existingEntry.get(), QUANTITY_ON_STOCK_2, EXPECTED_DELIVERY_2, RESTOCKABLE_IN_DAYS_2);
    }

    @Test
    public void syncDrafts_WithNewSupplyChannelAndChannelsEnsured_ShouldCreateNewSupplyChannelInCtp() {
        //assert ctp channel before sync
        Optional<Channel> existingChannel = getCtpClientOfTargetProject().getClient()
            .executeBlocking(ChannelQuery.of().byKey(SUPPLY_CHANNEL_KEY_2))
            .head();
        assertThat(existingChannel).isEmpty();

        //prepare sync data
        final Reference<Channel> channelToSync = Channel.referenceOfId(SUPPLY_CHANNEL_KEY_2);
        final InventoryEntryDraft draftToSync = InventoryEntryDraftBuilder
            .of(SKU_1, QUANTITY_ON_STOCK_2, EXPECTED_DELIVERY_2, RESTOCKABLE_IN_DAYS_2, channelToSync)
            .build();
        final InventorySyncOptions options = InventorySyncOptionsBuilder.of(getCtpClientOfTargetProject())
            .ensureChannels(true)
            .build();

        //sync and assert sync result
        final InventorySync syncer = new InventorySync(options);
        final InventorySyncStatistics syncResult = syncer.syncDrafts(singletonList(draftToSync))
            .toCompletableFuture()
            .join();
        assertStatistics(syncResult, 1, 1, 0, 0);

        //assert ctp channel after sync
        assertStatistics(syncResult, 1, 1, 0, 0);
        existingChannel = getCtpClientOfTargetProject().getClient()
            .executeBlocking(ChannelQuery.of().byKey(SUPPLY_CHANNEL_KEY_2))
            .head();
        assertThat(existingChannel).isNotEmpty();
    }

    /**
     * Deletes inventory entries and supply channels from both source and target projects.
     */
    private static void deleteInventoriesAndSupplyChannels() {
        cleanupInventoryEntries(getCtpClientOfSourceProject().getClient());
        cleanupSupplyChannels(getCtpClientOfSourceProject().getClient());
        cleanupInventoryEntries(getCtpClientOfTargetProject().getClient());
        cleanupSupplyChannels(getCtpClientOfTargetProject().getClient());
    }

    /**
     * Populate target CTP project.
     * Creates supply channel of key SUPPLY_CHANNEL_KEY_1.
     * Creates inventory entry of values: SKU_1, QUANTITY_ON_STOCK_1, EXPECTED_DELIVERY_1, RESTOCKABLE_IN_DAYS_1.
     * Creates inventory entry of values: SKU_1, QUANTITY_ON_STOCK_1, EXPECTED_DELIVERY_1, RESTOCKABLE_IN_DAYS_1 and
     * reference to supply channel created before.
     */
    private void populateTargetProject() {
        final ChannelDraft channelDraft = ChannelDraft.of(SUPPLY_CHANNEL_KEY_1)
            .withRoles(ChannelRole.INVENTORY_SUPPLY);
        final String channelId = getCtpClientOfTargetProject().getClient()
            .executeBlocking(ChannelCreateCommand.of(channelDraft))
            .getId();
        final Reference<Channel> supplyChannelReference = Channel.referenceOfId(channelId);
        final InventoryEntryDraft draft1 = InventoryEntryDraftBuilder.of(SKU_1, QUANTITY_ON_STOCK_1,
            EXPECTED_DELIVERY_1, RESTOCKABLE_IN_DAYS_1, null).build();
        final InventoryEntryDraft draft2 = InventoryEntryDraftBuilder.of(SKU_1, QUANTITY_ON_STOCK_1,
            EXPECTED_DELIVERY_1, RESTOCKABLE_IN_DAYS_1, supplyChannelReference).build();
        getCtpClientOfTargetProject().getClient()
            .executeBlocking(InventoryEntryCreateCommand.of(draft1));
        getCtpClientOfTargetProject().getClient()
            .executeBlocking(InventoryEntryCreateCommand.of(draft2));
    }

    private void assertStatistics(@Nullable final InventorySyncStatistics statistics,
                                  final int expectedProcessed,
                                  final int expectedCreated,
                                  final int expectedUpdated,
                                  final int expectedFailed) {
        assertThat(statistics).isNotNull();
        assertThat(statistics.getProcessed()).isEqualTo(expectedProcessed);
        assertThat(statistics.getCreated()).isEqualTo(expectedCreated);
        assertThat(statistics.getUpdated()).isEqualTo(expectedUpdated);
        assertThat(statistics.getFailed()).isEqualTo(expectedFailed);
    }

    private void assertValues(@Nonnull final InventoryEntry inventoryEntry,
                              @Nonnull final Long quantityOnStock,
                              @Nullable final ZonedDateTime expectedDelivery,
                              @Nullable final Integer restockableInDays) {
        assertThat(inventoryEntry.getQuantityOnStock()).isEqualTo(quantityOnStock);
        assertThat(inventoryEntry.getExpectedDelivery()).isEqualTo(expectedDelivery);
        assertThat(inventoryEntry.getRestockableInDays()).isEqualTo(restockableInDays);
    }
}
