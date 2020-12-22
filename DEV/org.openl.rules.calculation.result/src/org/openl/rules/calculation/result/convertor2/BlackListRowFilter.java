package org.openl.rules.calculation.result.convertor2;

/*-
 * #%L
 * OpenL - DEV - Rules - Calculation Result
 * %%
 * Copyright (C) 2015 - 2017 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public final class BlackListRowFilter implements RowFilter {

    private final Set<String> blackList;

    private BlackListRowFilter(Set<String> blackList) {
        this.blackList = blackList;
    }

    @Override
    public boolean excludeRow(String rowName) {
        return blackList.contains(rowName);
    }

    public static BlackListRowFilter buildBlackListRowFilter(Set<String> blackList) {
        Objects.requireNonNull(blackList, "blackList cannot be null");
        return new BlackListRowFilter(Collections.unmodifiableSet(blackList));
    }

}
