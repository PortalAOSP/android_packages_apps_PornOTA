/*
 * Copyright (C) 2018 PornAOSP
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
 *
 * Created by Devil7DK for PornAOSP
 *
 */

package com.pornota.configs;

import android.content.Context;
import android.preference.PreferenceManager;

import java.util.Properties;

public class MagiskConfig extends Properties {

    private static final String MAGISK_VARIANT = "magisk_variant";

    private String url;
    private String filename;
    private String version;
    private String size;

    public MagiskConfig(String url, String filename, String version, String size) {
        this.url = url;
        this.filename = filename;
        this.version = version;
        this.size = size;
    }

    public String getUrl() {return url;}

    public String getVersion() {return version;}

    public String getFilename() {return filename;}

    public String getSize() {return size;}

    public static String getVariant(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(MAGISK_VARIANT, "latest");
    }

    public static void setVariant(String variant, Context context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(MAGISK_VARIANT, variant).apply();
    }
}
