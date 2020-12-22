package org.openl.rules.testmethod.result;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ComparableComparatorTest {
    @Test
    public void test() {
        ComparableComparator comp = (ComparableComparator) ComparableComparator.getInstance();

        assertTrue(comp.isEqual(null, null));

        assertFalse(comp.isEqual(10, null));

        assertFalse(comp.isEqual(null, 10));

        assertTrue(comp.isEqual(10, 10));
    }
}
