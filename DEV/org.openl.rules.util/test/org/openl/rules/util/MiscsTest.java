package org.openl.rules.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.openl.rules.util.Miscs.isEmpty;
import static org.openl.rules.util.Miscs.isInfinite;
import static org.openl.rules.util.Miscs.isNaN;
import static org.openl.rules.util.Miscs.isNotEmpty;
import static org.openl.rules.util.Miscs.length;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;

import org.junit.Test;

public class MiscsTest {
    @Test
    public void testIsEmpty() {
        assertTrue(isEmpty(null));
        assertTrue(isEmpty(new Object[0]));
        assertTrue(isEmpty(""));
        assertTrue(isEmpty(" \t\n"));
        assertFalse(isEmpty(new Object[] { 0 }));
        assertFalse(isEmpty(0));
        assertFalse(isEmpty(0.0));
        assertFalse(isEmpty(Double.NaN));
        assertFalse(isEmpty("-"));

        assertTrue(isEmpty(new byte[0]));
        assertFalse(isEmpty(new byte[] { 0 }));
        assertTrue(isEmpty(new ArrayList()));
        assertFalse(isEmpty(new ArrayList() {
            {
                add(1);
            }
        }));
        assertTrue(isEmpty(new HashMap<>()));
        assertFalse(isEmpty(new HashMap() {
            {
                put(1, 1);
            }
        }));

        assertTrue(isEmpty((Iterable) Collections::emptyIterator));
        assertFalse(isEmpty((Iterable) () -> new Scanner("NotEmptyString")));
    }

    @Test
    public void testIsNotEmpty() {
        assertFalse(isNotEmpty(null));
        assertFalse(isNotEmpty(new Object[0]));
        assertFalse(isNotEmpty(""));
        assertFalse(isNotEmpty(" \t\n"));
        assertTrue(isNotEmpty(new Object[] { 0 }));
        assertTrue(isNotEmpty(0));
        assertTrue(isNotEmpty(0.0));
        assertTrue(isNotEmpty(Double.NaN));
        assertTrue(isNotEmpty("-"));

        assertFalse(isNotEmpty(new byte[0]));
        assertTrue(isNotEmpty(new byte[] { 0 }));
        assertFalse(isNotEmpty(new ArrayList()));
        assertTrue(isNotEmpty(new ArrayList() {
            {
                add(1);
            }
        }));
        assertFalse(isNotEmpty(new HashMap<>()));
        assertTrue(isNotEmpty(new HashMap() {
            {
                put(1, 1);
            }
        }));

        assertFalse(isNotEmpty((Iterable) Collections::emptyIterator));
        assertTrue(isNotEmpty((Iterable) () -> new Scanner("NotEmptyString")));
    }

    @Test
    public void testLength() {
        assertEquals(0, length((HashSet<?>) null));
        assertEquals(0, length(new HashSet()));
        assertEquals(1, length(new HashSet() {
            {
                add(false);
            }
        }));

        assertEquals(0, length((Map<?, ?>) null));
        assertEquals(0, length(new HashMap<>()));
        assertEquals(1, length(new HashMap() {
            {
                put(false, true);
            }
        }));
    }

    @Test
    public void testIsNaN() {
        assertFalse(isNaN((Double) null));
        assertTrue(isNaN(Double.NaN));
        assertFalse(isNaN(0.0));

        assertFalse(isNaN((Float) null));
        assertTrue(isNaN(Float.NaN));
        assertFalse(isNaN(0f));
    }

    @Test
    public void testIsInfinite() {
        assertFalse(isInfinite((Double) null));
        assertTrue(isInfinite(Double.NEGATIVE_INFINITY));
        assertTrue(isInfinite(Double.POSITIVE_INFINITY));
        assertFalse(isInfinite(Double.NaN));
        assertFalse(isInfinite(0.0));

        assertFalse(isInfinite((Float) null));
        assertTrue(isInfinite(Float.NEGATIVE_INFINITY));
        assertTrue(isInfinite(Float.POSITIVE_INFINITY));
        assertFalse(isInfinite(Float.NaN));
        assertFalse(isInfinite(0f));

        assertFalse(isInfinite((Long) null));
        assertTrue(isInfinite(Long.MAX_VALUE));
        assertTrue(isInfinite(Long.MIN_VALUE));
        assertFalse(isInfinite(1L));

        assertFalse(isInfinite((Integer) null));
        assertTrue(isInfinite(Integer.MAX_VALUE));
        assertTrue(isInfinite(Integer.MIN_VALUE));
        assertFalse(isInfinite(0));
    }
}
