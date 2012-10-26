/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wuman.twolevellrucache;

import java.io.File;

import junit.framework.TestCase;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class TwoLevelLruCacheTest extends TestCase {

    private final int appVersion = 100;
    private String javaTmpDir;
    private File cacheDir;
    private TwoLevelLruCache<String> cache;
    private Gson gson;
    private GsonConverter<String> converter;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        javaTmpDir = System.getProperty("java.io.tmpdir");
        cacheDir = new File(javaTmpDir, "TwoLevelLruCacheTest");
        cacheDir.mkdir();
        for (File file : cacheDir.listFiles()) {
            file.delete();
        }
        gson = new GsonBuilder().create();
        converter = new GsonConverter<String>(gson, String.class);
        cache = new TwoLevelLruCache<String>(cacheDir, appVersion,
                Integer.MAX_VALUE, Long.MAX_VALUE, converter);
    }

    @Override
    protected void tearDown() throws Exception {
        cache.close();
        super.tearDown();
    }

    public void testWriteAndReadEntry() throws Exception {
        cache.put("k1", "ABC");
        String value = cache.get("k1");
        assertEquals("ABC", value);
    }

    public void testReadAndWriteEntryAcrossCacheOpenAndClose() throws Exception {
        cache.put("k1", "A");
        cache.close();

        cache = new TwoLevelLruCache<String>(cacheDir, appVersion,
                Integer.MAX_VALUE, Long.MAX_VALUE, converter);
        String value = cache.get("k1");
        assertEquals("A", value);
    }

}
