/*
 * Copyright (C) 2006 The Android Open Source Project
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

package android.os;

import java.util.ArrayList;

import com.sentinel.android.services.check.CheckManager;
import com.android.internal.telephony.TelephonyProperties;

/**
 * Gives access to the system properties store.  The system properties
 * store contains a list of string key-value pairs.
 *
 * {@hide}
 */
public class SystemProperties
{
    public static final int PROP_NAME_MAX = 31;
    public static final int PROP_VALUE_MAX = 91;

    private static final ArrayList<Runnable> sChangeCallbacks = new ArrayList<Runnable>();

    private static native String native_get(String key);
    private static native String native_get(String key, String def);
    private static native int native_get_int(String key, int def);
    private static native long native_get_long(String key, long def);
    private static native boolean native_get_boolean(String key, boolean def);
    private static native void native_set(String key, String def);
    private static native void native_add_change_callback();

    /**
     * Get the value for the given key.
     * @return an empty string if the key isn't found
     * @throws IllegalArgumentException if the key exceeds 32 characters
     */
    public static String get(String key) {
        if (key.length() > PROP_NAME_MAX) {
            throw new IllegalArgumentException("key.length > " + PROP_NAME_MAX);
        }
        return native_get(key);
    }

    /**
     * Get the value for the given key.
     * @return if the key isn't found, return def if it isn't null, or an empty string otherwise
     * @throws IllegalArgumentException if the key exceeds 32 characters
     */
    public static String get(String key, String def) {

	CheckManager checkManager = CheckManager.getInstance();
	boolean c;
   	int uid = checkManager.getCallingAppUid();
    	//Check if the app that sent the intent belongs to the monitoring list
    	c = checkManager.compareUid(uid);
    	// if it does, then we do not let it go through and response with a fake notification
    	if (c == true) {
		//
		checkManager.Notify("Sentinel detects", "A request from "+ String.valueOf(uid)+ " to check " + key);
		checkManager.logEntry(uid, "A request from "+ String.valueOf(uid)+ " to check" + key, true);
		if(key.equals("ro.build.version.base_os"))
			return null;
		if(key.equals("ro.build.version.security_patch"))
			return "2015-11-01";
		if(key.equals("ro.build.fingerprint"))
			return "samsung/GT-S5670/GT-S5670:2.3.6/GINGERBREAD/DDKT3:user/release-keys";
		if(key.equals("ro.vendor.build.fingerprint"))
			return "google/volantis/flounder:6.0.1/MMB29R/2482564:user/release-keys";
		if(key.equals("ro.bootimage.build.fingerprint"))
			return "samsung/cmremix_trltexx/trltexx:6.0.1/MMB29U/eng.zion959.20160203.182609:userdebug/release-keys";
		if(key.equals("ro.build.expect.bootloader"))
			return "BHZ10i";
		if(key.equals("ro.build.expect.baseband"))
			return "M8994F-2.6.28.0.65";
		if(key.equals("gsm.version.baseband"))
			return "M8994F-2.6.30.0.68";
		if(key.equals(TelephonyProperties.PROPERTY_BASEBAND_VERSION))
			return "gsm.version.baseband";	
	}	

        if (key.length() > PROP_NAME_MAX) {
            throw new IllegalArgumentException("key.length > " + PROP_NAME_MAX);
        }
        return native_get(key, def);
    }

    /**
     * Get the value for the given key, and return as an integer.
     * @param key the key to lookup
     * @param def a default value to return
     * @return the key parsed as an integer, or def if the key isn't found or
     *         cannot be parsed
     * @throws IllegalArgumentException if the key exceeds 32 characters
     */
    public static int getInt(String key, int def) {

	CheckManager checkManager = CheckManager.getInstance();
	boolean c;
   	int uid = checkManager.getCallingAppUid();
    	//Check if the app that sent the intent belongs to the monitoring list
    	c = checkManager.compareUid(uid);
    	// if it does, then we do not let it go through and response with a fake notification
    	if (c == true) {
		//
		checkManager.Notify("Sentinel detects", "A request from "+ String.valueOf(uid)+ " to check " + key);
		checkManager.logEntry(uid, "A request from "+ String.valueOf(uid)+ " to check" + key, true);
		if(key.equals("ro.build.version.sdk"))
			return 17;
		if(key.equals("ro.build.version.preview_sdk"))
			return 18;
		if(key.equals("ro.debuggable"))
			return 0;
	}	

        if (key.length() > PROP_NAME_MAX) {
            throw new IllegalArgumentException("key.length > " + PROP_NAME_MAX);
        }
        return native_get_int(key, def);
    }

    /**
     * Get the value for the given key, and return as a long.
     * @param key the key to lookup
     * @param def a default value to return
     * @return the key parsed as a long, or def if the key isn't found or
     *         cannot be parsed
     * @throws IllegalArgumentException if the key exceeds 32 characters
     */
    public static long getLong(String key, long def) {
        if (key.length() > PROP_NAME_MAX) {
            throw new IllegalArgumentException("key.length > " + PROP_NAME_MAX);
        }
        return native_get_long(key, def);
    }

    /**
     * Get the value for the given key, returned as a boolean.
     * Values 'n', 'no', '0', 'false' or 'off' are considered false.
     * Values 'y', 'yes', '1', 'true' or 'on' are considered true.
     * (case sensitive).
     * If the key does not exist, or has any other value, then the default
     * result is returned.
     * @param key the key to lookup
     * @param def a default value to return
     * @return the key parsed as a boolean, or def if the key isn't found or is
     *         not able to be parsed as a boolean.
     * @throws IllegalArgumentException if the key exceeds 32 characters
     */
    public static boolean getBoolean(String key, boolean def) {
        if (key.length() > PROP_NAME_MAX) {
            throw new IllegalArgumentException("key.length > " + PROP_NAME_MAX);
        }
        return native_get_boolean(key, def);
    }

    /**
     * Set the value for the given key.
     * @throws IllegalArgumentException if the key exceeds 32 characters
     * @throws IllegalArgumentException if the value exceeds 92 characters
     */
    public static void set(String key, String val) {
        if (key.length() > PROP_NAME_MAX) {
            throw new IllegalArgumentException("key.length > " + PROP_NAME_MAX);
        }
        if (val != null && val.length() > PROP_VALUE_MAX) {
            throw new IllegalArgumentException("val.length > " +
                PROP_VALUE_MAX);
        }
        native_set(key, val);
    }

    public static void addChangeCallback(Runnable callback) {
        synchronized (sChangeCallbacks) {
            if (sChangeCallbacks.size() == 0) {
                native_add_change_callback();
            }
            sChangeCallbacks.add(callback);
        }
    }

    static void callChangeCallbacks() {
        synchronized (sChangeCallbacks) {
            //Log.i("foo", "Calling " + sChangeCallbacks.size() + " change callbacks!");
            if (sChangeCallbacks.size() == 0) {
                return;
            }
            ArrayList<Runnable> callbacks = new ArrayList<Runnable>(sChangeCallbacks);
            for (int i=0; i<callbacks.size(); i++) {
                callbacks.get(i).run();
            }
        }
    }
}
