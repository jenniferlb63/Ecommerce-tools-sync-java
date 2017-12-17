package com.commercetools.sync.categories.helpers;


import com.commercetools.sync.commons.helpers.BaseSyncStatistics;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.lang.String.format;

public class CategorySyncStatistics extends BaseSyncStatistics {
    /**
     * Map that represents categories with missing parents; the keys of the map are the keys of the missing parent
     * categories and the value of each is a list of the children category keys.
     */
    private Map<String, List<String>> categoryKeysWithMissingParents = new HashMap<>();

    public CategorySyncStatistics() {
        super();
    }

    /**
     * Builds a summary of the category sync statistics instance that looks like the following example:
     *
     * <p>"Summary: 2 categories were processed in total (0 created, 0 updated and 0 categories failed to sync)."
     *
     * @return a summary message of the category sync statistics instance.
     */
    @Override
    public String getReportMessage() {
        reportMessage = format("Summary: %s categories were processed in total "
                + "(%s created, %s updated, %s failed to sync and %s categories with a missing parent).",
            getProcessed(), getCreated(), getUpdated(), getFailed(),
            getNumberOfCategoriesWithMissingParents(categoryKeysWithMissingParents));
        return reportMessage;
    }

    static int getNumberOfCategoriesWithMissingParents(
        @Nonnull final Map<String, ArrayList<String>> categoryKeysWithMissingParents) {
        return categoryKeysWithMissingParents.values()
                                             .stream()
                                             .filter(Objects::nonNull)
                                             .map(List::size)
                                             .reduce(0, Integer::sum);
    }

    public Map<String, List<String>> getCategoryKeysWithMissingParents() {
        return categoryKeysWithMissingParents;
    }

    public void setCategoryKeysWithMissingParents(@Nonnull final
                                                  Map<String, List<String>> categoryKeysWithMissingParents) {
        this.categoryKeysWithMissingParents = categoryKeysWithMissingParents;
    }
}
