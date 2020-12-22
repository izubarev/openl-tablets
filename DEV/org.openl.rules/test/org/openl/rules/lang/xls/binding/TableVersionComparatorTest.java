package org.openl.rules.lang.xls.binding;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.openl.rules.table.properties.TableProperties;

public class TableVersionComparatorTest {

    private TableVersionComparator comparator;
    private TableProperties first;
    private TableProperties second;

    @Before
    public void setUp() {
        comparator = TableVersionComparator.getInstance();
        first = new TableProperties();
        second = new TableProperties();
    }

    @Test
    public void testCompareEqualProperties() {
        // two different instances
        first.setActive(Boolean.TRUE);
        second.setActive(Boolean.TRUE);
        assertEquals(0, comparator.compare(first, second));

        first.setActive(Boolean.TRUE);
        second.setActive(Boolean.TRUE);
        assertEquals(0, comparator.compare(first, second));

        first.setActive(Boolean.FALSE);
        second.setActive(Boolean.FALSE);
        assertEquals(0, comparator.compare(first, second));
    }

    @Test
    public void testLess() {
        first.setActive(true);
        second.setActive(false);
        assertEquals(-1, comparator.compare(first, second));
    }

    @Test
    public void testMore() {
        first.setActive(false);
        second.setActive(true);
        assertEquals(1, comparator.compare(first, second));
    }

    @Test
    public void testCompareNullProperties() {
        first.setActive(null);
        second.setActive(Boolean.TRUE);
        assertEquals(0, comparator.compare(first, second));

        first.setActive(Boolean.TRUE);
        second.setActive(null);
        assertEquals(0, comparator.compare(first, second));

        first.setActive(null);
        second.setActive(null);
        assertEquals(0, comparator.compare(first, second));

        first.setActive(null);
        second.setActive(Boolean.FALSE);
        assertEquals(-1, comparator.compare(first, second));

        first.setActive(Boolean.FALSE);
        second.setActive(null);
        assertEquals(1, comparator.compare(first, second));
    }

    @Test
    public void testLessByActive() {
        first.setActive(true);
        second.setActive(false);
        assertEquals(-1, comparator.compare(first, second));
    }

    @Test
    public void testMoreByActive() {
        first.setActive(false);
        second.setActive(true);
        assertEquals(1, comparator.compare(first, second));
    }
}
