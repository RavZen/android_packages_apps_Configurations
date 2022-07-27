/*
 * Copyright (C) 2017 AospExtended ROM Project
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

package org.derpfest.derpspace.fragments;

import android.app.ActivityManagerNative;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.IWindowManager;
import android.view.View;
import android.view.WindowManagerGlobal;

import androidx.preference.ListPreference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.util.derp.udfps.UdfpsUtils;

import com.android.settings.fuelgauge.PowerUsageSummary;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.search.BaseSearchIndexProvider;

import com.android.settingslib.search.Indexable;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.derp.support.preferences.SystemSettingSwitchPreference;
import com.derp.support.preferences.CustomSystemSeekBarPreference;
import com.derp.support.colorpicker.ColorPickerPreference;
import com.derp.support.preferences.SystemSettingListPreference;

import org.json.JSONException;
import org.json.JSONObject;
import static android.os.UserHandle.USER_SYSTEM;
import android.os.RemoteException;
import android.os.ServiceManager;
import static android.os.UserHandle.USER_CURRENT;

@SearchIndexable
public class LockscreenUI extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String FINGERPRINT_SUCCESS_VIB = "fingerprint_success_vib";
    private static final String FINGERPRINT_ERROR_VIB = "fingerprint_error_vib";
    private static final String SCREEN_OFF_FOD = "screen_off_fod";
    private static final String ENABLE_UDFPS_START_HAPTIC_FEEDBACK = "enable_udfps_start_haptic_feedback";
    private static final String AMBIENT_ICONS_LOCKSCREEN = "ambient_icons_lockscreen";
    private static final String AMBIENT_ICONS_SIZE = "ambient_icons_size";
    private static final String LOCKSCREEN_BATTERY_INFO_TEMP_UNIT = "lockscreen_charge_temp_unit";
    private static final String CUSTOM_CLOCK_FACE = Settings.Secure.LOCK_SCREEN_CUSTOM_CLOCK_FACE;
    private static final String DEFAULT_CLOCK = "com.android.keyguard.clock.DefaultClockController";

    private FingerprintManager mFingerprintManager;
    private SystemSettingSwitchPreference mFingerprintSuccessVib;
    private SystemSettingSwitchPreference mFingerprintErrorVib;
    private SystemSettingSwitchPreference mScreenOffFOD;
    private SystemSettingSwitchPreference mUdfpsHapticFeedback;
    private SystemSettingSwitchPreference mAmbientIconsLockscreen;
    private CustomSystemSeekBarPreference mAmbientIconsSize;
    private ColorPickerPreference mAmbientIconsColor;
    private SystemSettingListPreference mBatteryTempUnit;

    private Context mContext;
    private ListPreference mLockClockStyles;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.lockscreen_ui);

        final ContentResolver resolver = getActivity().getContentResolver();
        final PreferenceScreen prefSet = getPreferenceScreen();
        final PackageManager mPm = getActivity().getPackageManager();
        final PreferenceCategory fpCategory = (PreferenceCategory)
                findPreference("lockscreen_ui_finterprint_category");

        int unitMode = Settings.System.getIntForUser(resolver,
                Settings.System.LOCKSCREEN_BATTERY_INFO_TEMP_UNIT, 0, UserHandle.USER_CURRENT);
        mBatteryTempUnit = (SystemSettingListPreference) findPreference(
                "lockscreen_charge_temp_unit");
        mBatteryTempUnit.setValue(String.valueOf(unitMode));
        mBatteryTempUnit.setSummary(mBatteryTempUnit.getEntry());
        mBatteryTempUnit.setOnPreferenceChangeListener(this);

        mFingerprintManager = (FingerprintManager)
                getActivity().getSystemService(Context.FINGERPRINT_SERVICE);
        mFingerprintSuccessVib = findPreference(FINGERPRINT_SUCCESS_VIB);
        mFingerprintErrorVib = findPreference(FINGERPRINT_ERROR_VIB);
        mScreenOffFOD = findPreference(SCREEN_OFF_FOD);
        mUdfpsHapticFeedback = findPreference(ENABLE_UDFPS_START_HAPTIC_FEEDBACK);


        if (mPm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT) &&
                 mFingerprintManager != null) {
            if (!mFingerprintManager.isHardwareDetected()){
                prefSet.removePreference(fpCategory);
            } else {
                mFingerprintSuccessVib.setChecked((Settings.System.getInt(getContentResolver(),
                        Settings.System.FP_SUCCESS_VIBRATE, 1) == 1));
                mFingerprintSuccessVib.setOnPreferenceChangeListener(this);
                mFingerprintErrorVib.setChecked((Settings.System.getInt(getContentResolver(),
                        Settings.System.FP_ERROR_VIBRATE, 1) == 1));
                mFingerprintErrorVib.setOnPreferenceChangeListener(this);
                if (UdfpsUtils.hasUdfpsSupport(getActivity())) {
                    mScreenOffFOD.setChecked((Settings.System.getInt(getContentResolver(),
                            Settings.System.SCREEN_OFF_FOD, 1) == 1));
                    mScreenOffFOD.setOnPreferenceChangeListener(this);
                    mUdfpsHapticFeedback.setChecked((Settings.System.getInt(getContentResolver(),
                            Settings.System.ENABLE_UDFPS_START_HAPTIC_FEEDBACK, 1) == 1));
                    mUdfpsHapticFeedback.setOnPreferenceChangeListener(this);
                } else {
                    fpCategory.removePreference(mScreenOffFOD);
                    fpCategory.removePreference(mUdfpsHapticFeedback);
                }
            }
        } else {
            prefSet.removePreference(fpCategory);
        }

        mAmbientIconsLockscreen = (SystemSettingSwitchPreference) findPreference(AMBIENT_ICONS_LOCKSCREEN);
        mAmbientIconsLockscreen.setChecked((Settings.System.getInt(resolver,
                Settings.System.AMBIENT_ICONS_LOCKSCREEN, 0) == 1));
        mAmbientIconsLockscreen.setOnPreferenceChangeListener(this);

        mAmbientIconsSize = (CustomSystemSeekBarPreference) findPreference(AMBIENT_ICONS_SIZE);
        int intSize = Settings.System.getInt(getContentResolver(),
                Settings.System.AMBIENT_ICONS_SIZE, 80);
        mAmbientIconsSize.setValue(intSize / 1);
        mAmbientIconsSize.setOnPreferenceChangeListener(this);

        mLockClockStyles = (ListPreference) findPreference(CUSTOM_CLOCK_FACE);
        String mLockClockStylesValue = getLockScreenCustomClockFace();
        mLockClockStyles.setValue(mLockClockStylesValue);
        mLockClockStyles.setSummary(mLockClockStyles.getEntry());
        mLockClockStyles.setOnPreferenceChangeListener(this);

    }

    private String getLockScreenCustomClockFace() {
        mContext = getActivity();
        String value = Settings.Secure.getStringForUser(mContext.getContentResolver(),
                CUSTOM_CLOCK_FACE, USER_CURRENT);

        if (value == null || value.isEmpty()) value = DEFAULT_CLOCK;

        try {
            JSONObject json = new JSONObject(value);
            return json.getString("clock");
        } catch (JSONException ex) {
        }
        return value;
    }

    private void setLockScreenCustomClockFace(String value) {
        try {
            JSONObject json = new JSONObject();
            json.put("clock", value);
            Settings.Secure.putStringForUser(mContext.getContentResolver(), CUSTOM_CLOCK_FACE,
                    json.toString(), USER_CURRENT);
        } catch (JSONException ex) {
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.DERP;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mBatteryTempUnit) {
            int value = Integer.parseInt((String) objValue);
            Settings.System.putIntForUser(getActivity().getContentResolver(),
                    Settings.System.LOCKSCREEN_BATTERY_INFO_TEMP_UNIT, value,
                    UserHandle.USER_CURRENT);
            int index = mBatteryTempUnit.findIndexOfValue((String) objValue);
            mBatteryTempUnit.setSummary(
            mBatteryTempUnit.getEntries()[index]);
            return true;
        } else if (preference == mFingerprintSuccessVib) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.FP_SUCCESS_VIBRATE, value ? 1 : 0);
            return true;
        } else if (preference == mFingerprintErrorVib) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.FP_ERROR_VIBRATE, value ? 1 : 0);
            return true;
        } else if (preference == mScreenOffFOD) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.SCREEN_OFF_FOD, value ? 1 : 0);
            return true;
        } else if (preference == mUdfpsHapticFeedback) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.ENABLE_UDFPS_START_HAPTIC_FEEDBACK, value ? 1 : 0);
            return true;
        } else if (preference == mAmbientIconsLockscreen) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.AMBIENT_ICONS_LOCKSCREEN, value ? 0 : 0);
            return true;
        } else if (preference == mAmbientIconsSize) {
            int width = ((Integer)objValue).intValue();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.AMBIENT_ICONS_SIZE, width);
            return true;
        } else if (preference == mLockClockStyles) {
            setLockScreenCustomClockFace((String) objValue);
            int index = mLockClockStyles.findIndexOfValue((String) objValue);
            mLockClockStyles.setSummary(mLockClockStyles.getEntries()[index]);
            return true;
        }
        return false;
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                    boolean enabled) {
                final ArrayList<SearchIndexableResource> result = new ArrayList<>();
                final SearchIndexableResource sir = new SearchIndexableResource(context);
                sir.xmlResId = R.xml.lockscreen_ui;
                result.add(sir);
                return result;
            }

            @Override
            public List<String> getNonIndexableKeys(Context context) {
                final List<String> keys = super.getNonIndexableKeys(context);
                return keys;
            }
    };
}
