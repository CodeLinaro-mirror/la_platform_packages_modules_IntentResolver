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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Trace;
import android.os.UserHandle;

import androidx.annotation.WorkerThread;

import com.android.intentresolver.model.AbstractResolverComparator;

import java.util.List;

public class ChooserListController extends ResolverListController {
    private final List<ComponentName> mFilteredComponents;
    private final SharedPreferences mPinnedComponents;
    private final int mRankedGroupSize;

    public ChooserListController(
            Context context,
            PackageManager pm,
            Intent targetIntent,
            String referrerPackageName,
            int launchedFromUid,
            AbstractResolverComparator resolverComparator,
            UserHandle queryIntentsAsUser,
            List<ComponentName> filteredComponents,
            SharedPreferences pinnedComponents,
            int rankedGroupSize) {
        super(
                context,
                pm,
                targetIntent,
                referrerPackageName,
                launchedFromUid,
                resolverComparator,
                queryIntentsAsUser);
        mFilteredComponents = filteredComponents;
        mPinnedComponents = pinnedComponents;
        mRankedGroupSize = rankedGroupSize;
    }

    @Override
    public boolean isComponentFiltered(ComponentName name) {
        return mFilteredComponents.contains(name);
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
}
