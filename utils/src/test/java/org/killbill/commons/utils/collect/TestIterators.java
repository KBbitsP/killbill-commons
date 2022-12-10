/*
 * Copyright 2020-2022 Equinix, Inc
 * Copyright 2014-2022 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.commons.utils.collect;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class TestIterators {

    @Test(groups = "fast")
    public void testGetLast() {
        final List<KeyValue> keyValues = List.of(new KeyValue("a", "1"), new KeyValue("b", "2"), new KeyValue("c", "3"));
        final KeyValue last = Iterators.getLast(keyValues.iterator());

        assertNotNull(last);
        assertEquals(last.getKey(), "c");
        assertEquals(last.getValue(), "3");
    }

    @Test(groups = "fast")
    public void testGetLastWithEmptyList() {
        final List<KeyValue> keyValues = Collections.emptyList();
        try {
            Iterators.getLast(keyValues.iterator());
            fail("iterator is empty");
        } catch (final NoSuchElementException ignored) {}
    }

    @Test(groups = "fast")
    public void testTransform() {
        final List<KeyValue> list = List.of(new KeyValue("a", "1"), new KeyValue("b", "2"), new KeyValue("c", "3"));
        final Iterator<String> keyOnly = Iterators.transform(list.iterator(), KeyValue::getKey);
        assertEquals(keyOnly.next(), "a");
        assertEquals(keyOnly.next(), "b");
        assertEquals(keyOnly.next(), "c");
    }

    @Test(groups = "fast")
    public void testToUnmodifiableList() {
        final Collection<KeyValue> set = new HashSet<>();
        set.add(new KeyValue("a", "1"));
        set.add(new KeyValue("b", "2"));
        set.add(new KeyValue("b", "2")); // Duplicate. Note that Set.of cannot add duplicate element (IllegalArgumentException: duplicate element)

        final List<KeyValue> list = Iterators.toUnmodifiableList(set.iterator());

        assertNotNull(list);
        assertEquals(list.size(), 2);
        assertTrue(list.contains(new KeyValue("a", "1")));
        assertTrue(list.contains(new KeyValue("b", "2")));
    }

    @Test(groups = "fast")
    public void testSize() {
        final List<String> strings = List.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j");
        final int size = Iterators.size(strings.iterator());
        assertEquals(size, 10);
    }

    @Test(groups = "fast")
    public void testContains() {
        final Iterator<String> strings = List.of("a", "b", "c").iterator();
        final Iterator<String> empty = Collections.emptyIterator();
        final Iterator<KeyValue> keyValues = List.of(new KeyValue("a", "1"),
                                                     new KeyValue("b", "2"))
                                                 .iterator();

        assertTrue(Iterators.contains(strings, "a"));
        assertFalse(Iterators.contains(strings, "d"));

        assertFalse(Iterators.contains(empty, "a"));

        assertTrue(Iterators.contains(keyValues, new KeyValue("b", "2")));
        assertFalse(Iterators.contains(keyValues, new KeyValue("a", "2")));
    }
}
