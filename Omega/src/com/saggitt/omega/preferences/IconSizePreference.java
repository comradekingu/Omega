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

package com.saggitt.omega.preferences;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.DialogPreference;

import com.android.launcher3.R;

public class IconSizePreference extends DialogPreference {
    private Context mContext;
    public IconSizePreference(Context context) {
        this(context, null, 0);
    }

    public IconSizePreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IconSizePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        updateSummary();
        mContext = context;
    }

    private void updateSummary() {
        setSummary("Icon Size: ");
    }

    @Override
    public int getDialogLayoutResource() {
        return R.layout.preference_icon_size;
    }
}
