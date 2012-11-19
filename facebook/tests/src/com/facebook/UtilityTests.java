/**
 * Copyright 2010 Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

public final class UtilityTests extends AndroidTestCase {

    @SmallTest @MediumTest @LargeTest
    public void testCamelCaseToLowercase() {
        assertEquals("hello_world", Utility.convertCamelCaseToLowercaseWithUnderscores("HelloWorld"));
        assertEquals("hello_world", Utility.convertCamelCaseToLowercaseWithUnderscores("helloWorld"));
    }

    @SmallTest @MediumTest @LargeTest
    public void testJsonObjectClear() throws JSONException {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("hello", "world");
        jsonObject.put("hocus", "pocus");

        Utility.jsonObjectClear(jsonObject);
        assertEquals(0, jsonObject.length());
    }

    @SmallTest @MediumTest @LargeTest
    public void testJsonObjectContainsValue() throws JSONException {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("hello", "world");
        jsonObject.put("hocus", "pocus");

        assertTrue(Utility.jsonObjectContainsValue(jsonObject, "pocus"));
        assertFalse(Utility.jsonObjectContainsValue(jsonObject, "Fred"));
    }

    @SmallTest @MediumTest @LargeTest
    public void testJsonObjectEntrySet() throws JSONException {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("hello", "world");
        jsonObject.put("hocus", "pocus");

        Set<Entry<String, Object>> entrySet = Utility.jsonObjectEntrySet(jsonObject);
        assertEquals(2, entrySet.size());
    }

    @SmallTest @MediumTest @LargeTest
    public void testJsonObjectKeySet() throws JSONException {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("hello", "world");
        jsonObject.put("hocus", "pocus");

        Set<String> keySet = Utility.jsonObjectKeySet(jsonObject);
        assertEquals(2, keySet.size());
        assertTrue(keySet.contains("hello"));
        assertFalse(keySet.contains("world"));
    }

    @SmallTest @MediumTest @LargeTest
    public void testJsonObjectPutAll() throws JSONException {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("hello", "world");
        map.put("hocus", "pocus");

        JSONObject jsonObject = new JSONObject();
        Utility.jsonObjectPutAll(jsonObject, map);

        assertEquals("pocus", jsonObject.get("hocus"));
        assertEquals(2, jsonObject.length());
    }

    @SmallTest @MediumTest @LargeTest
    public void testJsonObjectValues() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("hello", "world");
        jsonObject.put("hocus", "pocus");

        Collection<Object> values = Utility.jsonObjectValues(jsonObject);

        assertEquals(2, values.size());
        assertTrue(values.contains("world"));
    }
}
