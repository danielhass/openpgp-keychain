package org.sufficientlysecure.keychain.util;

import android.os.Bundle;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(emulateSdk = 18) // Robolectric doesn't yet support 19
public class FileImportCacheTest {

    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;
    }

    @Test
    public void testInputOutput() throws Exception {

        FileImportCache<Bundle> cache = new FileImportCache<Bundle>(Robolectric.application);

        ArrayList<Bundle> list = new ArrayList<Bundle>();

        for (int i = 0; i < 50; i++) {
            Bundle b = new Bundle();
            b.putInt("key1", i);
            b.putString("key2", Integer.toString(i));
            list.add(b);
        }

        // write to cache file
        cache.writeCache(list);

        // read back
        List<Bundle> last = cache.readCacheIntoList();

        for (int i = 0; i < list.size(); i++) {
            Assert.assertEquals("input values should be equal to output values",
                    list.get(i).getInt("key1"), last.get(i).getInt("key1"));
            Assert.assertEquals("input values should be equal to output values",
                    list.get(i).getString("key2"), last.get(i).getString("key2"));
        }

    }

}
