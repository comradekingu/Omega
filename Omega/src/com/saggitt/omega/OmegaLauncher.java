/*
 *  Copyright (c) 2020 Omega Launcher
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.saggitt.omega;

import android.animation.AnimatorSet;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.android.launcher3.AppInfo;
import com.android.launcher3.FolderInfo;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.WorkspaceItemInfo;
import com.android.launcher3.util.ComponentKey;
import com.saggitt.omega.gestures.GestureController;
import com.saggitt.omega.iconpack.EditIconActivity;
import com.saggitt.omega.iconpack.IconPackManager;
import com.saggitt.omega.override.CustomInfoProvider;
import com.saggitt.omega.util.ContextUtils;
import com.saggitt.omega.util.CustomLauncherClient;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static com.saggitt.omega.util.Config.REQUEST_PERMISSION_STORAGE_ACCESS;

public class OmegaLauncher extends Launcher {
    public Context mContext;
    private boolean paused = false;
    private boolean sRestart = false;
    private OmegaPreferences mOmegaPrefs;
    private OmegaPreferencesChangeCallback prefCallback = new OmegaPreferencesChangeCallback(this);
    private OmegaLauncherCallbacks launcherCallbacks;
    private GestureController mGestureController;
    public static boolean showFolderNotificationCount;
    public static Drawable currentEditIcon = null;
    public static ItemInfo currentEditInfo = null;
    public final int CODE_EDIT_ICON = 100;

    public static OmegaLauncher getLauncher(Context context) {
        if (context instanceof OmegaLauncher) {
            return (OmegaLauncher) context;
        } else {
            return (OmegaLauncher) LauncherAppState.getInstance(context).getLauncher();
        }
    }

    public OmegaLauncher() {
        launcherCallbacks = new OmegaLauncherCallbacks(this);
        setLauncherCallbacks(launcherCallbacks);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 && !Utilities.hasStoragePermission(this)) {
            Utilities.requestStoragePermission(this);
        }

        super.onCreate(savedInstanceState);
        mContext = this;
        mOmegaPrefs = Utilities.getOmegaPrefs(mContext);
        mOmegaPrefs.registerCallback(prefCallback);
        ContextUtils contextUtils = new ContextUtils(this);
        contextUtils.setAppLanguage(mOmegaPrefs.getLanguage());
        showFolderNotificationCount = mOmegaPrefs.getFolderBadgeCount();
    }

    public GestureController getGestureController() {
        if (mGestureController == null)
            mGestureController = new GestureController(this);

        return mGestureController;
    }

    @Override
    public void onResume() {
        super.onResume();
        restartIfPending();
        paused = false;
    }

    @Override
    public void onPause() {
        super.onPause();
        paused = true;
    }

    public void onDestroy() {
        super.onDestroy();
        Utilities.getOmegaPrefs(this).unregisterCallback();

        if (sRestart) {
            sRestart = false;
            OmegaPreferences.Companion.destroyInstance();
        }
    }

    public void startEditIcon(ItemInfo itemInfo, CustomInfoProvider<ItemInfo> infoProvider) {
        ComponentKey component;

        currentEditInfo = itemInfo;

        if (itemInfo instanceof AppInfo) {
            component = ((AppInfo) itemInfo).toComponentKey();
            currentEditIcon = Objects.requireNonNull(IconPackManager.Companion.getInstance(this).getEntryForComponent(component)).getDrawable();
        } else if (itemInfo instanceof WorkspaceItemInfo) {
            component = new ComponentKey(itemInfo.getTargetComponent(), itemInfo.user);
            currentEditIcon = new BitmapDrawable(mContext.getResources(), ((WorkspaceItemInfo) itemInfo).iconBitmap);
        } else if (itemInfo instanceof FolderInfo) {
            component = ((FolderInfo) itemInfo).toComponentKey();
            currentEditIcon = ((FolderInfo) itemInfo).getDefaultIcon(this);
        } else {
            component = null;
            currentEditIcon = null;
        }

        boolean folderInfo = itemInfo instanceof FolderInfo;
        int flags = Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS | Intent.FLAG_ACTIVITY_CLEAR_TASK;
        Intent intent = EditIconActivity.Companion.newIntent(this, infoProvider.getTitle(itemInfo), folderInfo, component);

        BlankActivity.Companion
                .startActivityForResult(this, intent, CODE_EDIT_ICON, flags, (resultCode, data) -> {
                    handleEditIconResult(resultCode, data);
                    return null;
                });

    }

    private void handleEditIconResult(int resultCode, @NotNull Bundle data) {
        if (resultCode == Activity.RESULT_OK) {
            if (currentEditInfo == null) {
                return;
            }
            ItemInfo itemInfo = currentEditInfo;

            String entryString = Objects.requireNonNull(data).getString(EditIconActivity.EXTRA_ENTRY);

            IconPackManager.CustomIconEntry customIconEntry = IconPackManager.CustomIconEntry.Companion.fromString(entryString);
            Log.d(TAG, "Entry Icon:  Item: " + itemInfo + " Entry: " + customIconEntry);
            (CustomInfoProvider.Companion.forItem(this, itemInfo)).setIcon(itemInfo, customIconEntry);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_STORAGE_ACCESS) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle(R.string.title_storage_permission_required)
                        .setMessage(R.string.content_storage_permission_required)
                        .setCancelable(false)
                        .setNegativeButton(android.R.string.no, null)
                        .setPositiveButton(android.R.string.yes, (dialog, which) -> Utilities.requestStoragePermission(this))
                        .show();
            }
        }
        //if (requestCode == REQUEST_PERMISSION_LOCATION_ACCESS) {
        //OmegaAppKt.getOmegaApp(this).getSmartspace().updateWeatherData();
        //}
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public boolean shouldRecreate() {
        return !sRestart;
    }

    public void scheduleRestart() {
        if (paused) {
            sRestart = true;
        } else {
            Utilities.restartLauncher(mContext);
        }
    }

    public void restartIfPending() {
        if (sRestart) {
            OmegaAppKt.getOmegaApp(this).restart(false);
        }
    }

    @Nullable
    public CustomLauncherClient getGoogleNow() {
        return launcherCallbacks.getClient();
    }

    public void playQsbAnimation() {
        launcherCallbacks.mQsbAnimationController.dZ();
    }

    public AnimatorSet openQsb() {
        return launcherCallbacks.mQsbAnimationController.openQsb();
    }
}
