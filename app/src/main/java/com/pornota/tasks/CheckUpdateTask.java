/*
 * Copyright (C) 2018 Chandra Poerwanto
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

package com.pornota.tasks;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NotificationCompat;

import com.pornota.MainActivity;
import com.pornota.R;
import com.pornota.configs.AppConfig;
import com.pornota.configs.OTAConfig;
import com.pornota.configs.OTAVersion;
import com.pornota.dialogs.WaitDialogHandler;
import com.pornota.utils.OTAUtils;
import com.pornota.xml.OTADevice;
import com.pornota.xml.OTAParser;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

public class CheckUpdateTask extends AsyncTask<Void, Void, OTADevice> {

    private static CheckUpdateTask mInstance = null;
    private final Handler mHandler = new WaitDialogHandler();
    private boolean mIsBackgroundThread;
    private UpdateCheckerCallback callBack;

    public interface UpdateCheckerCallback {
        void processOTACheckResult(OTADevice device, boolean updateAvailable);
        Context getContext();
    }

    private CheckUpdateTask(boolean isBackgroundThread) {
        this.mIsBackgroundThread = isBackgroundThread;
    }

    public static CheckUpdateTask getInstance(boolean isBackgroundThread, UpdateCheckerCallback callBack) {
        if (mInstance == null) {
            mInstance = new CheckUpdateTask(isBackgroundThread);
        }
        mInstance.callBack = callBack;
        return mInstance;
    }

    private static boolean isConnectivityAvailable(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = connMgr.getActiveNetworkInfo();
        return (netInfo != null && netInfo.isConnected());
    }

    @Override
    protected OTADevice doInBackground(Void... params) {
        if (callBack == null || callBack.getContext() == null) {OTAUtils.logError("CheckUpdateTask - Context is null. Aborting...");return null;}

        if (!isConnectivityAvailable(callBack.getContext())) {
            return null;
        }

        showWaitDialog();

        OTADevice professional = null;
        OTADevice teen = null;
        OTADevice final_ota = null;
        String deviceName = OTAUtils.getDeviceName(callBack.getContext());
        OTAUtils.logInfo("deviceName: " + deviceName);
        if (!deviceName.isEmpty()) {
            String preferredType = AppConfig.getPreferredType(callBack.getContext());
            if("Professional".equals(preferredType) || "Latest".equals(preferredType)) professional = fetchURL(OTAConfig.getInstance(callBack.getContext()).getProfessionalOtaUrl(),deviceName);
            if("Teen".equals(preferredType) || "Latest".equals(preferredType)) teen = fetchURL(OTAConfig.getInstance(callBack.getContext()).getTeenOtaUrl(),deviceName);
            if(professional==null && teen!=null){
                final_ota = teen;
            }else if(professional!=null && teen==null){
                final_ota = professional;
            }else if(professional!=null){
                boolean result = OTAVersion.checkVersions(professional.getLatestVersion(), teen.getLatestVersion(), professional.getBuildTime(), teen.getBuildTime(), callBack.getContext());
                if(result){
                    final_ota = professional;
                }else{
                    final_ota = teen;
                }
            }else{final_ota = null;}
        }

        if (final_ota != null) final_ota.setROMSize(OTAUtils.getSizeFromURL(callBack.getContext(), final_ota.getROMURL()));

        return final_ota;
    }

    @Override
    protected void onPostExecute(OTADevice device) {
        super.onPostExecute(device);
        boolean updateAvailable = false;
        if (device == null) {
            showToast(R.string.check_update_failed);
        } else {
            updateAvailable = OTAVersion.checkServerVersion(device.getLatestVersion(), device.getBuildTime(), callBack.getContext());
            if (updateAvailable) {
                showNotification(callBack.getContext(), device);
                showToast(R.string.update_available);
            } else {
                showToast(R.string.no_update_available);
            }
        }

        if(callBack!=null){callBack.processOTACheckResult(device,updateAvailable);}

        hideWaitDialog();

        mInstance = null;
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        mInstance = null;
    }

    private OTADevice fetchURL(String otaUrl, String deviceName){
        OTADevice device = null;
        try {
            InputStream is = OTAUtils.downloadURL(otaUrl);
            if (is != null) {
                final String releaseType = OTAConfig.getInstance(callBack.getContext()).getReleaseType();
                device = OTAParser.getInstance().parse(is, deviceName, releaseType);
                is.close();
            }
        } catch (IOException | XmlPullParserException e) {
            OTAUtils.logError(e);
        }
        return device;
    }

    private void showWaitDialog() {
        if (!mIsBackgroundThread) {
            Message msg = mHandler.obtainMessage(WaitDialogHandler.MSG_SHOW_DIALOG);
            msg.obj = callBack.getContext();
            msg.arg1 = R.string.dialog_message;
            mHandler.sendMessage(msg);
        }
    }

    private void hideWaitDialog() {
        if (!mIsBackgroundThread) {
            Message msg = mHandler.obtainMessage(WaitDialogHandler.MSG_CLOSE_DIALOG);
            mHandler.sendMessage(msg);
        }
    }

    private void showToast(int messageId) {
        if (!mIsBackgroundThread) {
            OTAUtils.toast(messageId, callBack.getContext());
        }
    }

    private void showNotification(Context context, OTADevice device) {
        if (mIsBackgroundThread) {
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            int notifyID = 1;
            String id = "pornota_channel";
            CharSequence name = context.getString(R.string.pornota_channel);
            String description = context.getString(R.string.pornota_channel_description);
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel mChannel = new NotificationChannel(id, name, importance);
            mChannel.setDescription(description);
            notificationManager.createNotificationChannel(mChannel);

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context.getApplicationContext(), "PornOTA")
                    .setSmallIcon(R.drawable.ic_notification_pornota)
                    .setContentTitle(context.getString(R.string.notification_title))
                    .setContentText(context.getString(R.string.notification_message))
                    .setOnlyAlertOnce(true)
                    .setAutoCancel(true)
                    .setChannelId(id);

            Intent intent = new Intent(context, MainActivity.class);
            intent.setAction(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra("OTADevice", device);
            final PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.setContentIntent(pendingIntent);
            notificationManager.notify(notifyID, mBuilder.build());
        }
    }
}
