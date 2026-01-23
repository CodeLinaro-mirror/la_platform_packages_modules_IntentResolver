/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.intentresolver;

import android.annotation.Nullable;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Trace;
import android.os.UserHandle;

import androidx.annotation.WorkerThread;

import com.android.intentresolver.data.model.ChooserRequest;
import com.android.intentresolver.model.AbstractResolverComparator;

import java.util.List;

public class ChooserListController extends ResolverListController {
    private final SharedPreferences mPinnedComponents;
    private final int mRankedGroupSize;
    private final ChooserRequest mRequest;
    @Nullable
    private final CharSequence mChooserTitle;

    public ChooserListController(
            Context context,
            PackageManager pm,
            ChooserRequest chooserRequest,
            @Nullable CharSequence chooserTitle,
            int launchedFromUid,
            AbstractResolverComparator resolverComparator,
            UserHandle queryIntentsAsUser,
            SharedPreferences pinnedComponents,
            int rankedGroupSize) {
        super(
                context,
                pm,
                chooserRequest.getTargetIntent(),
                chooserRequest.getReferrerPackage(),
                launchedFromUid,
                resolverComparator,
                queryIntentsAsUser,
                /*shouldGetActivityMetadata =*/ true);
        mRequest = chooserRequest;
        mChooserTitle = chooserTitle;
        mPinnedComponents = pinnedComponents;
        mRankedGroupSize = rankedGroupSize;
    }

    @Override
    public boolean isComponentFiltered(ComponentName name) {
        return mRequest.getFilteredComponentNames().contains(name);
    }

    @Override
    public boolean isComponentPinned(ComponentName name) {
        return mPinnedComponents.getBoolean(name.flattenToString(), false);
    }

    /**
     * Rather than fully sorting the input list, this sorting task will put the top k elements
     * in the head of input list and fill the tail with other elements in undetermined order.
     */
    @Override
    @WorkerThread
    public void sort(List<ResolvedComponentInfo> inputList) {
        Trace.beginSection("ChooserListController#sort");
        try {
            topK(inputList, mRankedGroupSize);
        } finally {
            Trace.endSection();
        }
    }

    // TODO: investigate a feasibility of making this logic being a part of the target resolution
    //  logic (e.g. implemented in getResolversForIntentAsUser method).
    @Override
    public Intent getReplacementIntent(ActivityInfo aInfo, Intent defIntent) {
        Intent result = defIntent;
        if (mRequest.getReplacementExtras() != null) {
            final Bundle replExtras =
                    mRequest.getReplacementExtras().getBundle(aInfo.packageName);
            if (replExtras != null) {
                result = new Intent(defIntent);
                result.putExtras(replExtras);
            }
        }
        if (aInfo.name.equals(IntentForwarderActivity.FORWARD_INTENT_TO_PARENT)
                || aInfo.name.equals(IntentForwarderActivity.FORWARD_INTENT_TO_MANAGED_PROFILE)) {
            result = Intent.createChooser(result, mChooserTitle);

            // Don't auto-launch single intents if the intent is being forwarded. This is done
            // because automatically launching a resolving application as a response to the user
            // action of switching accounts is pretty unexpected.
            result.putExtra(Intent.EXTRA_AUTO_LAUNCH_SINGLE_CHOICE, false);
        }
        return result;
    }
}
