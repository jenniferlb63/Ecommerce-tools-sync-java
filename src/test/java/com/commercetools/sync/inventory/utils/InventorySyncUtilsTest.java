package com.commercetools.sync.inventory.utils;

import com.commercetools.sync.commons.helpers.CtpClient;
import com.commercetools.sync.inventory.InventoryEntryMock;
import com.commercetools.sync.inventory.InventorySyncOptionsBuilder;
import com.commercetools.sync.services.TypeService;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.InventoryEntryDraftBuilder;
import io.sphere.sdk.inventory.commands.updateactions.*;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.CustomFieldsDraftBuilder;
import org.junit.Before;
import org.junit.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InventorySyncUtilsTest {

    private static final String CUSTOM_TYPE_KEY = "testType";
    private static final String CUSTOM_TYPE_ID = "testId";
    private static final String CUSTOM_FIELD_1_NAME = "testField";
    private static final String CUSTOM_FIELD_2_NAME = "differentField";
    private static final String CUSTOM_FIELD_1_VALUE = "testValue";
    private static final String CUSTOM_FIELD_2_VALUE = "differentValue";

    private static final ZonedDateTime DATE_1 = ZonedDateTime.of(2017, 4, 1, 10, 0, 0, 0, ZoneId.of("UTC"));
    private static final ZonedDateTime DATE_2 = ZonedDateTime.of(2017, 5, 1, 20, 0, 0, 0, ZoneId.of("UTC"));

    private InventoryEntry inventoryEntry;
    private InventoryEntry inventoryEntryWithCustomField1;
    private InventoryEntryDraft similarDraft;
    private InventoryEntryDraft variousDraft;

    @Before
    public void setup() {
        inventoryEntry = InventoryEntryMock.of("123", 10l, 10, DATE_1)
                .withChannelRefExpanded("111", "key1")
                .build();
        inventoryEntryWithCustomField1 = InventoryEntryMock.of("123", 10l, 10, DATE_1)
                .withChannelRefExpanded("111", "key1")
                .withCustomFieldExpanded(CUSTOM_TYPE_ID, CUSTOM_TYPE_KEY, CUSTOM_FIELD_1_NAME, CUSTOM_FIELD_1_VALUE)
                .build();

        similarDraft = InventoryEntryDraftBuilder.of("123", 10l, DATE_1, 10, Channel.referenceOfId("111")).build();
        variousDraft = InventoryEntryDraftBuilder.of("321", 20l, DATE_2, 20, Channel.referenceOfId("222")).build();
    }

    @Test
    public void buildActions_WithSimilarEntries_ShouldReturnEmptyList() {
        List<UpdateAction<InventoryEntry>> actions = InventorySyncUtils
                .buildActions(inventoryEntry, similarDraft, InventorySyncOptionsBuilder.of(mock(CtpClient.class))
                        .build(), mockTypeService());

        assertThat(actions).isEmpty();
    }

    @Test
    public void buildActions_WithVariousEntries_ShouldReturnActions() {
        List<UpdateAction<InventoryEntry>> actions = InventorySyncUtils
                .buildActions(inventoryEntry, variousDraft, InventorySyncOptionsBuilder.of(mock(CtpClient.class))
                        .build(), mockTypeService());

        assertThat(actions).hasSize(4);
        assertThat(actions.get(0)).isNotNull();
        assertThat(actions.get(1)).isNotNull();
        assertThat(actions.get(2)).isNotNull();
        assertThat(actions.get(3)).isNotNull();
        assertThat(actions.get(0)).isInstanceOf(ChangeQuantity.class);
        assertThat(actions.get(1)).isInstanceOf(SetRestockableInDays.class);
        assertThat(actions.get(2)).isInstanceOf(SetExpectedDelivery.class);
        assertThat(actions.get(3)).isInstanceOfAny(SetSupplyChannel.class);
    }

    @Test
    public void buildActions_WithSimilarEntriesAndSameCustomFields_ShouldReturnEmptyList() {
        final InventoryEntryDraft draft = InventoryEntryDraftBuilder.of(similarDraft)
                .custom(getDraftOfCustomField(CUSTOM_FIELD_1_NAME, CUSTOM_FIELD_1_VALUE))
                .build();
        List<UpdateAction<InventoryEntry>> actions = InventorySyncUtils
                .buildActions(inventoryEntryWithCustomField1, draft, InventorySyncOptionsBuilder
                                .of(mock(CtpClient.class))
                                .build(),
                        mockTypeService());

        assertThat(actions).isEmpty();
    }

    @Test
    public void buildActions_WithSimilarEntriesAndNewCustomTypeSet_ShouldReturnActions() {
        final InventoryEntryDraft draft = InventoryEntryDraftBuilder.of(similarDraft)
                .custom(getDraftOfCustomField(CUSTOM_FIELD_2_NAME, CUSTOM_FIELD_2_VALUE))
                .build();
        List<UpdateAction<InventoryEntry>> actions = InventorySyncUtils
                .buildActions(inventoryEntry, draft, InventorySyncOptionsBuilder.of(mock(CtpClient.class)).build(),
                        mockTypeService());

        assertThat(actions).hasSize(1);
        assertThat(actions.get(0)).isNotNull();
        assertThat(actions.get(0)).isInstanceOf(SetCustomType.class);
    }

    @Test
    public void buildActions_WithSimilarEntriesAndRemovedExistingCustomType_ShouldReturnActions() {
        List<UpdateAction<InventoryEntry>> actions = InventorySyncUtils
                .buildActions(inventoryEntryWithCustomField1, similarDraft,InventorySyncOptionsBuilder
                                .of(mock(CtpClient.class))
                                .build(),
                        mockTypeService());

        assertThat(actions).hasSize(1);
        assertThat(actions.get(0)).isNotNull();
        assertThat(actions.get(0)).isInstanceOf(SetCustomType.class);
    }

    @Test
    public void buildActions_WithSimilarEntriesButDifferentCustomFields_ShouldReturnActions() {
        final InventoryEntryDraft draft = InventoryEntryDraftBuilder.of(similarDraft)
                .custom(getDraftOfCustomField(CUSTOM_FIELD_2_NAME, CUSTOM_FIELD_2_VALUE))
                .build();
        List<UpdateAction<InventoryEntry>> actions = InventorySyncUtils
                .buildActions(inventoryEntryWithCustomField1, draft,InventorySyncOptionsBuilder
                                .of(mock(CtpClient.class))
                                .build(),
                        mockTypeService());

        assertThat(actions).hasSize(2);
        assertThat(actions.get(0)).isNotNull();
        assertThat(actions.get(1)).isNotNull();
        assertThat(actions.get(0)).isInstanceOf(SetCustomField.class);
        assertThat(actions.get(1)).isInstanceOf(SetCustomField.class);
    }

    @Test
    public void buildActions_WithSimilarEntriesButDifferentCustomFieldValues_ShouldReturnActions() {
        final InventoryEntryDraft draft = InventoryEntryDraftBuilder.of(similarDraft)
                .custom(getDraftOfCustomField(CUSTOM_FIELD_1_NAME, CUSTOM_FIELD_2_VALUE))
                .build();
        List<UpdateAction<InventoryEntry>> actions = InventorySyncUtils
                .buildActions(inventoryEntryWithCustomField1, draft,InventorySyncOptionsBuilder
                                .of(mock(CtpClient.class))
                                .build(),
                        mockTypeService());

        assertThat(actions).hasSize(1);
        assertThat(actions.get(0)).isNotNull();
        assertThat(actions.get(0)).isInstanceOf(SetCustomField.class);
    }

    private CustomFieldsDraft getDraftOfCustomField(String fieldName, String fieldValue) {
        return CustomFieldsDraftBuilder.ofTypeKey(CUSTOM_TYPE_KEY)
                .addObject(fieldName, fieldValue)
                .build();
    }

    private TypeService mockTypeService() {
        final TypeService typeService = mock(TypeService.class);
        when(typeService.getCachedTypeKeyById(CUSTOM_TYPE_ID)).thenReturn(CUSTOM_TYPE_KEY);
        return typeService;
    }
}
