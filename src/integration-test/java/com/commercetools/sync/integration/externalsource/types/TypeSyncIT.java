package com.commercetools.sync.integration.externalsource.types;

import com.commercetools.sync.types.TypeSync;
import com.commercetools.sync.types.TypeSyncOptions;
import com.commercetools.sync.types.TypeSyncOptionsBuilder;
import com.commercetools.sync.types.helpers.TypeSyncStatistics;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.ConcurrentModificationException;
import io.sphere.sdk.client.ErrorResponseException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.EnumValue;
import io.sphere.sdk.models.LocalizedEnumValue;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.TextInputHint;
import io.sphere.sdk.types.EnumFieldType;
import io.sphere.sdk.types.FieldDefinition;
import io.sphere.sdk.types.LocalizedEnumFieldType;
import io.sphere.sdk.types.ResourceTypeIdsSetBuilder;
import io.sphere.sdk.types.StringFieldType;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.TypeDraft;
import io.sphere.sdk.types.TypeDraftBuilder;
import io.sphere.sdk.types.commands.TypeUpdateCommand;
import io.sphere.sdk.types.queries.TypeQuery;
import io.sphere.sdk.utils.CompletableFutureUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypes;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.integration.commons.utils.TypeITUtils.FIELD_DEFINITION_1;
import static com.commercetools.sync.integration.commons.utils.TypeITUtils.FIELD_DEFINITION_2;
import static com.commercetools.sync.integration.commons.utils.TypeITUtils.FIELD_DEFINITION_3;
import static com.commercetools.sync.integration.commons.utils.TypeITUtils.FIELD_DEFINITION_LABEL_1;
import static com.commercetools.sync.integration.commons.utils.TypeITUtils.FIELD_DEFINITION_NAME_1;
import static com.commercetools.sync.integration.commons.utils.TypeITUtils.TYPE_DESCRIPTION_1;
import static com.commercetools.sync.integration.commons.utils.TypeITUtils.TYPE_DESCRIPTION_2;
import static com.commercetools.sync.integration.commons.utils.TypeITUtils.TYPE_KEY_1;
import static com.commercetools.sync.integration.commons.utils.TypeITUtils.TYPE_KEY_2;
import static com.commercetools.sync.integration.commons.utils.TypeITUtils.TYPE_NAME_1;
import static com.commercetools.sync.integration.commons.utils.TypeITUtils.TYPE_NAME_2;
import static com.commercetools.sync.integration.commons.utils.TypeITUtils.getTypeByKey;
import static com.commercetools.sync.integration.commons.utils.TypeITUtils.populateSourceProject;
import static com.commercetools.sync.integration.commons.utils.TypeITUtils.populateTargetProject;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;


public class TypeSyncIT {

    /**
     * Deletes types from source and target CTP projects.
     * Populates source and target CTP projects with test data.
     */
    @Before
    public void setup() {
        deleteTypes(CTP_TARGET_CLIENT);
        populateSourceProject();
        populateTargetProject();
    }

    /**
     * Deletes all the test data from the {@code CTP_SOURCE_CLIENT} and the {@code CTP_SOURCE_CLIENT} projects that
     * were set up in this test class.
     */
    @AfterClass
    public static void tearDown() {
        deleteTypes(CTP_TARGET_CLIENT);
    }


    @Test
    public void sync_WithUpdatedType_ShouldUpdateType() {
        // preparation
        final Optional<Type> oldTypeBefore = getTypeByKey(CTP_TARGET_CLIENT, TYPE_KEY_1);
        assertThat(oldTypeBefore).isNotEmpty();

        final TypeDraft newTypeDraft = TypeDraftBuilder.of(
                TYPE_KEY_1,
                TYPE_NAME_2,
                ResourceTypeIdsSetBuilder.of().addCategories().build())
                .description(TYPE_DESCRIPTION_2)
                .fieldDefinitions(singletonList(FIELD_DEFINITION_1))
                .build();

        final TypeSyncOptions typeSyncOptions = TypeSyncOptionsBuilder
                .of(CTP_TARGET_CLIENT)
                .build();

        final TypeSync typeSync = new TypeSync(typeSyncOptions);

        // test
        final TypeSyncStatistics typeSyncStatistics = typeSync
                .sync(singletonList(newTypeDraft))
                .toCompletableFuture()
                .join();

        //assertions
        assertThat(typeSyncStatistics).hasValues(1, 0, 1, 0);


        final Optional<Type> oldTypeAfter = getTypeByKey(CTP_TARGET_CLIENT, TYPE_KEY_1);

        assertThat(oldTypeAfter).isNotEmpty();
        assertThat(oldTypeAfter).hasValueSatisfying(type -> {
            assertThat(type.getName()).isEqualTo(TYPE_NAME_2);
            assertThat(type.getDescription()).isEqualTo(TYPE_DESCRIPTION_2);
            assertFieldDefinitionsAreEqual(type.getFieldDefinitions(), singletonList(FIELD_DEFINITION_1));
        });
    }

    @Test
    public void sync_WithNewType_ShouldCreateType() {
        //preparation
        final TypeDraft newTypeDraft = TypeDraftBuilder.of(
                TYPE_KEY_2,
                TYPE_NAME_2,
                ResourceTypeIdsSetBuilder.of().addCategories().build())
                .description(TYPE_DESCRIPTION_2)
                .fieldDefinitions(singletonList(FIELD_DEFINITION_1))
                .build();

        final TypeSyncOptions typeSyncOptions = TypeSyncOptionsBuilder
                .of(CTP_TARGET_CLIENT)
                .build();

        final TypeSync typeSync = new TypeSync(typeSyncOptions);

        //test
        final TypeSyncStatistics typeSyncStatistics = typeSync
                .sync(singletonList(newTypeDraft))
                .toCompletableFuture()
                .join();

        //assertions
        assertThat(typeSyncStatistics).hasValues(1, 1, 0, 0);

        final Optional<Type> oldTypeAfter = getTypeByKey(CTP_TARGET_CLIENT, TYPE_KEY_2);

        assertThat(oldTypeAfter).isNotEmpty();
        assertThat(oldTypeAfter).hasValueSatisfying(type -> {
            assertThat(type.getName()).isEqualTo(TYPE_NAME_2);
            assertThat(type.getDescription()).isEqualTo(TYPE_DESCRIPTION_2);
            assertFieldDefinitionsAreEqual(type.getFieldDefinitions(), singletonList(FIELD_DEFINITION_1));
        });
    }

    @Test
    public void sync_WithUpdatedType_WithNewFieldDefinitions_ShouldUpdateTypeAddingFieldDefinition() {
        //preparation
        final TypeDraft newTypeDraft = TypeDraftBuilder.of(
                TYPE_KEY_1,
                TYPE_NAME_1,
                ResourceTypeIdsSetBuilder.of().addCategories().build())
                .description(TYPE_DESCRIPTION_1)
                .fieldDefinitions(asList(FIELD_DEFINITION_1, FIELD_DEFINITION_2, FIELD_DEFINITION_3))
                .build();


        final TypeSyncOptions typeSyncOptions = TypeSyncOptionsBuilder
                .of(CTP_TARGET_CLIENT)
                .build();

        final TypeSync typeSync = new TypeSync(typeSyncOptions);

        //test
        final TypeSyncStatistics typeSyncStatistics = typeSync
                .sync(singletonList(newTypeDraft))
                .toCompletableFuture()
                .join();

        //assertions
        assertThat(typeSyncStatistics).hasValues(1, 0, 1, 0);

        final Optional<Type> oldTypeAfter = getTypeByKey(CTP_TARGET_CLIENT, TYPE_KEY_1);

        assertThat(oldTypeAfter).hasValueSatisfying(type -> {
            assertFieldDefinitionsAreEqual(type.getFieldDefinitions(),
                asList(
                    FIELD_DEFINITION_1,
                    FIELD_DEFINITION_2,
                    FIELD_DEFINITION_3
                ));
        });
    }

    @Test
    public void sync_WithUpdatedType_WithoutOldFieldDefinition_ShouldUpdateTypeRemovingFieldDefinition() {
        final Optional<Type> oldTypeBefore = getTypeByKey(CTP_TARGET_CLIENT, TYPE_KEY_1);
        assertThat(oldTypeBefore).isNotEmpty();

        final TypeDraft newTypeDraft = TypeDraftBuilder.of(
                TYPE_KEY_1,
                TYPE_NAME_1,
                ResourceTypeIdsSetBuilder.of().addCategories().build())
                .description(TYPE_DESCRIPTION_1)
                .fieldDefinitions(singletonList(FIELD_DEFINITION_1))
                .build();

        final TypeSyncOptions typeSyncOptions = TypeSyncOptionsBuilder
                .of(CTP_TARGET_CLIENT)
                .build();

        final TypeSync typeSync = new TypeSync(typeSyncOptions);

        final TypeSyncStatistics typeSyncStatistics = typeSync
                .sync(singletonList(newTypeDraft))
                .toCompletableFuture().join();

        assertThat(typeSyncStatistics).hasValues(1, 0, 1, 0);

        final Optional<Type> oldTypeAfter = getTypeByKey(CTP_TARGET_CLIENT, TYPE_KEY_1);

        assertThat(oldTypeAfter).isNotEmpty();
        assertFieldDefinitionsAreEqual(oldTypeAfter.get().getFieldDefinitions(),
                singletonList(FIELD_DEFINITION_1));
    }

    @Test
    public void sync_WithUpdatedType_ChangingFieldDefinitionOrder_ShouldUpdateTypeChangingFieldDefinitionOrder() {
        //preparation
        final TypeDraft newTypeDraft = TypeDraftBuilder.of(
                TYPE_KEY_1,
                TYPE_NAME_1,
                ResourceTypeIdsSetBuilder.of().addCategories().build())
                .description(TYPE_DESCRIPTION_1)
                .fieldDefinitions(asList(FIELD_DEFINITION_2, FIELD_DEFINITION_1))
                .build();

        final TypeSyncOptions typeSyncOptions = TypeSyncOptionsBuilder
                .of(CTP_TARGET_CLIENT)
                .build();

        final TypeSync typeSync = new TypeSync(typeSyncOptions);

        //test
        final TypeSyncStatistics typeSyncStatistics = typeSync
                .sync(singletonList(newTypeDraft))
                .toCompletableFuture().join();

        //assertions
        assertThat(typeSyncStatistics).hasValues(1, 0, 1, 0);

        final Optional<Type> oldTypeAfter = getTypeByKey(CTP_TARGET_CLIENT, TYPE_KEY_1);

        assertThat(oldTypeAfter).hasValueSatisfying(type ->
            assertFieldDefinitionsAreEqual(type.getFieldDefinitions(),
                asList(FIELD_DEFINITION_2, FIELD_DEFINITION_1)));
    }

    @Test
    public void sync_WithUpdatedType_WithUpdatedFieldDefinition_ShouldUpdateTypeUpdatingFieldDefinition() {
        //preparation
        final FieldDefinition fieldDefinitionUpdated = FieldDefinition.of(
                StringFieldType.of(),
                FIELD_DEFINITION_NAME_1,
                LocalizedString.ofEnglish("label_1_updated"),
                true,
                TextInputHint.MULTI_LINE);

        final TypeDraft newTypeDraft = TypeDraftBuilder.of(
                TYPE_KEY_1,
                TYPE_NAME_1,
                ResourceTypeIdsSetBuilder.of().addCategories().build())
                .description(TYPE_DESCRIPTION_1)
                .fieldDefinitions(singletonList(fieldDefinitionUpdated))
                .build();

        final TypeSyncOptions typeSyncOptions = TypeSyncOptionsBuilder
                .of(CTP_TARGET_CLIENT)
                .build();

        final TypeSync typeSync = new TypeSync(typeSyncOptions);

        //test
        final TypeSyncStatistics typeSyncStatistics = typeSync
                .sync(singletonList(newTypeDraft))
                .toCompletableFuture().join();

        //assertions
        assertThat(typeSyncStatistics).hasValues(1, 0, 1, 0);

        final Optional<Type> oldTypeAfter = getTypeByKey(CTP_TARGET_CLIENT, TYPE_KEY_1);

        assertThat(oldTypeAfter).hasValueSatisfying(type ->
            assertFieldDefinitionsAreEqual(type.getFieldDefinitions(),
                singletonList(fieldDefinitionUpdated)));
    }

    @Test
    public void sync_WithoutKey_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
        final TypeDraft newTypeDraft = TypeDraftBuilder.of(
                null,
                TYPE_NAME_1,
                ResourceTypeIdsSetBuilder.of().addCategories().build())
                .description(TYPE_DESCRIPTION_1)
                .fieldDefinitions(asList(FIELD_DEFINITION_1, FIELD_DEFINITION_2))
                .build();

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final TypeSyncOptions typeSyncOptions = TypeSyncOptionsBuilder
                .of(CTP_TARGET_CLIENT)
                .errorCallback((errorMessage, exception) -> {
                    errorMessages.add(errorMessage);
                    exceptions.add(exception);
                })
                .build();

        final TypeSync typeSync = new TypeSync(typeSyncOptions);

        //test
        final TypeSyncStatistics typeSyncStatistics = typeSync
                .sync(singletonList(newTypeDraft))
                .toCompletableFuture()
                .join();

        // assertions
        assertThat(errorMessages)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(message ->
                assertThat(message).isEqualTo("Failed to process type draft without key.")
            );

        assertThat(exceptions)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(throwable -> assertThat(throwable).isNull());

        assertThat(typeSyncStatistics).hasValues(1, 0, 0, 1);
    }

    @Test
    public void sync_WithNullDraft_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
        //preparation
        final TypeDraft newTypeDraft = null;
        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final TypeSyncOptions typeSyncOptions = TypeSyncOptionsBuilder
                .of(CTP_TARGET_CLIENT)
                .errorCallback((errorMessage, exception) -> {
                    errorMessages.add(errorMessage);
                    exceptions.add(exception);
                })
                .build();

        final TypeSync typeSync = new TypeSync(typeSyncOptions);

        //test
        final TypeSyncStatistics typeSyncStatistics = typeSync
                .sync(singletonList(newTypeDraft))
                .toCompletableFuture()
                .join();

        //assertions
        assertThat(errorMessages)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(message ->
                assertThat(message).isEqualTo("Failed to process null type draft.")
            );

        assertThat(exceptions)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(throwable -> assertThat(throwable).isNull());

        assertThat(typeSyncStatistics).hasValues(1, 0, 0, 1);
    }

    @Test
    public void sync_WithoutName_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
        // preparation
        // Draft without "name" throws a commercetools exception because "name" is a required value
        final TypeDraft newTypeDraft = TypeDraftBuilder.of(
                TYPE_KEY_1,
                null,
                ResourceTypeIdsSetBuilder.of().addCategories().build())
                .description(TYPE_DESCRIPTION_1)
                .fieldDefinitions(asList(FIELD_DEFINITION_1, FIELD_DEFINITION_2))
                .build();

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final TypeSyncOptions typeSyncOptions = TypeSyncOptionsBuilder
                .of(CTP_TARGET_CLIENT)
                .errorCallback((errorMessage, exception) -> {
                    errorMessages.add(errorMessage);
                    exceptions.add(exception);
                })
                .build();

        final TypeSync typeSync = new TypeSync(typeSyncOptions);

        // test
        final TypeSyncStatistics typeSyncStatistics = typeSync
                .sync(singletonList(newTypeDraft))
                .toCompletableFuture()
                .join();

        // assertions
        assertThat(errorMessages)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(message ->
                assertThat(message).contains("Failed to update type with key 'key_1'.")
            );

        assertThat(exceptions)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(throwable -> {
                assertThat(throwable).isExactlyInstanceOf(CompletionException.class);
                assertThat(throwable).hasCauseExactlyInstanceOf(ErrorResponseException.class);
                assertThat(throwable).hasMessageContaining("Missing required value");
            });

        assertThat(typeSyncStatistics).hasValues(1, 0, 0, 1);
    }

    @Test
    public void sync_WithoutFieldDefinitionType_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
        //preparation
        final FieldDefinition fieldDefinition = FieldDefinition.of(
                null,
                FIELD_DEFINITION_NAME_1,
                FIELD_DEFINITION_LABEL_1,
                true,
                TextInputHint.SINGLE_LINE);

        final TypeDraft newTypeDraft = TypeDraftBuilder.of(
                TYPE_KEY_1,
                null,
                ResourceTypeIdsSetBuilder.of().addCategories().build())
                .description(TYPE_DESCRIPTION_1)
                .fieldDefinitions(singletonList(fieldDefinition))
                .build();

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final TypeSyncOptions typeSyncOptions = TypeSyncOptionsBuilder
                .of(CTP_TARGET_CLIENT)
                .errorCallback((errorMessage, exception) -> {
                    errorMessages.add(errorMessage);
                    exceptions.add(exception);
                })
                .build();

        final TypeSync typeSync = new TypeSync(typeSyncOptions);

        //test
        final TypeSyncStatistics typeSyncStatistics = typeSync
                .sync(singletonList(newTypeDraft))
                .toCompletableFuture()
                .join();

        // assertions
        assertThat(errorMessages)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(message ->
                assertThat(message).contains("Failed to update type with key 'key_1'.")
            );

        assertThat(exceptions)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(throwable -> {
                assertThat(throwable).isExactlyInstanceOf(CompletionException.class);
                assertThat(throwable).hasCauseExactlyInstanceOf(ErrorResponseException.class);
                assertThat(throwable).hasMessageContaining("Missing required value");
            });

        assertThat(typeSyncStatistics).hasValues(1, 0, 0, 1);
    }

    @Test
    public void sync_WithSeveralBatches_ShouldReturnProperStatistics() {
        // preparation
        // Default batch size is 50 (check TypeSyncOptionsBuilder) so we have 2 batches of 50
        final List<TypeDraft> typeDrafts = IntStream
                .range(0, 100)
                .mapToObj(i -> TypeDraftBuilder.of(
                        "key__" + Integer.toString(i),
                        LocalizedString.ofEnglish("name__" + Integer.toString(i)),
                        ResourceTypeIdsSetBuilder.of().addCategories().build())
                        .description(LocalizedString.ofEnglish("description__" + Integer.toString(i)))
                        .fieldDefinitions(singletonList(FIELD_DEFINITION_1))
                        .build())
                .collect(Collectors.toList());

        final TypeSyncOptions typeSyncOptions = TypeSyncOptionsBuilder
                .of(CTP_TARGET_CLIENT)
                .build();

        final TypeSync typeSync = new TypeSync(typeSyncOptions);

        //test
        final TypeSyncStatistics typeSyncStatistics = typeSync
                .sync(typeDrafts)
                .toCompletableFuture()
                .join();

        //assertion
        assertThat(typeSyncStatistics)
                .hasValues(100, 100, 0, 0);
    }

    private static void assertFieldDefinitionsAreEqual(@Nonnull final List<FieldDefinition> oldFields,
                                                       @Nonnull final List<FieldDefinition> newFields) {

        IntStream.range(0, newFields.size())
                .forEach(index -> {
                    final FieldDefinition oldFieldDefinition = oldFields.get(index);
                    final FieldDefinition newFieldDefinition = newFields.get(index);

                    assertThat(oldFieldDefinition.getName()).isEqualTo(newFieldDefinition.getName());
                    assertThat(oldFieldDefinition.getLabel()).isEqualTo(newFieldDefinition.getLabel());
                    assertThat(oldFieldDefinition.getType()).isEqualTo(newFieldDefinition.getType());
                    // no update action exists for the input hint
                    //assertThat(oldFieldDefinition.getInputHint()).isEqualTo(newFieldDefinition.getInputHint());
                    assertThat(oldFieldDefinition.isRequired()).isEqualTo(newFieldDefinition.isRequired());

                    if (oldFieldDefinition.getType().getClass() == EnumFieldType.class) {
                        assertPlainEnumsValuesAreEqual(
                                ((EnumFieldType) oldFieldDefinition.getType()).getValues(),
                                ((EnumFieldType) newFieldDefinition.getType()).getValues()
                        );
                    } else if (oldFieldDefinition.getType().getClass() == LocalizedEnumFieldType.class) {
                        assertLocalizedEnumsValuesAreEqual(
                                ((LocalizedEnumFieldType) oldFieldDefinition.getType()).getValues(),
                                ((LocalizedEnumFieldType) newFieldDefinition.getType()).getValues()
                        );
                    }
                });
    }

    private static void assertPlainEnumsValuesAreEqual(@Nonnull final List<EnumValue> enumValues,
                                                       @Nonnull final List<EnumValue> enumValuesDrafts) {

        IntStream.range(0, enumValuesDrafts.size())
                .forEach(index -> {
                    final EnumValue enumValue = enumValues.get(index);
                    final EnumValue enumValueDraft = enumValuesDrafts.get(index);

                    assertThat(enumValue.getKey()).isEqualTo(enumValueDraft.getKey());
                    assertThat(enumValue.getLabel()).isEqualTo(enumValueDraft.getLabel());
                });
    }

    private static void assertLocalizedEnumsValuesAreEqual(@Nonnull final List<LocalizedEnumValue> enumValues,
                                                           @Nonnull final List<LocalizedEnumValue> enumValuesDrafts) {

        IntStream.range(0, enumValuesDrafts.size())
                .forEach(index -> {
                    final LocalizedEnumValue enumValue = enumValues.get(index);
                    final LocalizedEnumValue enumValueDraft = enumValuesDrafts.get(index);

                    assertThat(enumValue.getKey()).isEqualTo(enumValueDraft.getKey());
                    assertThat(enumValue.getLabel()).isEqualTo(enumValueDraft.getLabel());
                });
    }

    @Test
    public void sync_WithChangedTypeButBadGatewayException_ShouldFailUpdateType() {
        // Mock sphere client to return BadGatewayException on the first update request.
        final SphereClient spyClient = spy(CTP_TARGET_CLIENT);
        when(spyClient.execute(any(TypeUpdateCommand.class)))
            .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture( new BadGatewayException()));

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> errors = new ArrayList<>();

        final TypeSyncOptions typeSyncOptions =
            TypeSyncOptionsBuilder.of(spyClient)
                                  .errorCallback((errorMessage, error) -> {
                                      errorMessages.add(errorMessage);
                                      errors.add(error);
                                  })
                                  .build();

        final TypeDraft typeDraft = TypeDraftBuilder.of(
            TYPE_KEY_1,
            TYPE_NAME_2,
            ResourceTypeIdsSetBuilder.of().addCategories().build())
                                                    .description(TYPE_DESCRIPTION_2)
                                                    .fieldDefinitions(singletonList(FIELD_DEFINITION_1))
                                                    .build();

        final TypeSync typeSync = new TypeSync(typeSyncOptions);
        final TypeSyncStatistics statistics = typeSync.sync(Collections.singletonList(typeDraft))
                                                      .toCompletableFuture()
                                                      .join();

        // Test and assertion
        assertThat(statistics).hasValues(1, 0, 0, 1);
        assertThat(errorMessages).hasSize(1);
        assertThat(errors).hasSize(1);

        assertThat(errors.get(0).getCause()).isExactlyInstanceOf(BadGatewayException.class);

        assertThat(errorMessages.get(0))
            .contains(format("Failed to update type with key: '%s'. Reason: %s",
                typeDraft.getKey(), errors.get(0)));
    }

    @Test
    public void sync_WithChangedTypeButConcurrentModificationException_ShouldRetryToUpdateNewTypeWithSuccess() {

        // Mock sphere client to return ConcurrentModification on the first update request.
        final SphereClient spyClient = spy(CTP_TARGET_CLIENT);
        when(spyClient.execute(any(TypeUpdateCommand.class)))
            .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(new ConcurrentModificationException()))
            .thenCallRealMethod();

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> errors = new ArrayList<>();

        final TypeSyncOptions typeSyncOptions =
            TypeSyncOptionsBuilder.of(spyClient)
                                      .errorCallback((errorMessage, error) -> {
                                          errorMessages.add(errorMessage);
                                          errors.add(error);
                                      })
                                      .build();

        final TypeDraft typeDraft = TypeDraftBuilder.of(
            TYPE_KEY_1,
            TYPE_NAME_2,
            ResourceTypeIdsSetBuilder.of().addCategories().build())
                                                    .description(TYPE_DESCRIPTION_2)
                                                    .fieldDefinitions(singletonList(FIELD_DEFINITION_1))
                                                    .build();

        final TypeSync typeSync = new TypeSync(typeSyncOptions);
        final TypeSyncStatistics statistics = typeSync.sync(Collections.singletonList(typeDraft))
                                                              .toCompletableFuture()
                                                              .join();

        // Test and assertion
        assertThat(statistics).hasValues(1, 0, 1, 0);
        assertThat(errorMessages).isEmpty();
        assertThat(errors).isEmpty();
    }

    @Test
    public void sync_WithChangedTypeButBadGatewayException_ShouldFailToReFetchAndUpdate() {
        // Mock sphere client to return ConcurrentModification on the first update request.
        final SphereClient spyClient = spy(CTP_TARGET_CLIENT);
        when(spyClient.execute(any(TypeUpdateCommand.class)))
            .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture( new ConcurrentModificationException()))
            .thenCallRealMethod();

        when(spyClient.execute(any(TypeQuery.class)))
            .thenCallRealMethod() // fetchMatchingTypesByKeys
            .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(new BadGatewayException()));


        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> errors = new ArrayList<>();

        final TypeSyncOptions typeSyncOptions =
            TypeSyncOptionsBuilder.of(spyClient)
                                  .errorCallback((errorMessage, error) -> {
                                      errorMessages.add(errorMessage);
                                      errors.add(error);
                                  })
                                  .build();

        final TypeDraft typeDraft = TypeDraftBuilder.of(
            TYPE_KEY_1,
            TYPE_NAME_2,
            ResourceTypeIdsSetBuilder.of().addCategories().build())
                                                    .description(TYPE_DESCRIPTION_2)
                                                    .fieldDefinitions(singletonList(FIELD_DEFINITION_1))
                                                    .build();

        final TypeSync typeSync = new TypeSync(typeSyncOptions);
        final TypeSyncStatistics statistics = typeSync.sync(Collections.singletonList(typeDraft))
                                                      .toCompletableFuture()
                                                      .join();

        // Test and assertion
        assertThat(statistics).hasValues(1, 0, 0, 1);
        assertThat(errorMessages).hasSize(2);
        assertThat(errors).hasSize(2);

        assertThat(errors.get(0).getCause()).isExactlyInstanceOf(BadGatewayException.class);
        assertThat(errorMessages.get(0))
            .contains(format("Failed to fetch types with keys: '%s'. Reason: %s",
                typeDraft.getKey(), errors.get(0)));

        assertThat(errorMessages.get(1))
            .contains(format("Failed to update type with key: '%s'. Reason: Failed to fetch type on retry.",
                typeDraft.getKey()));
    }

}
