/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.bluetooth.gatt;

public class FilterParams {
    private final int mClientIf;
    private final int mFiltIndex;
    private final int mFeatSeln;
    private final int mListLogicType;
    private final int mFiltLogicType;
    private final int mRssiHighValue;
    private final int mRssiLowValue;
    private final int mDelyMode;
    private final int mFoundTimeOut;
    private final int mLostTimeOut;
    private final int mFoundTimeOutCnt;
    private final int mNumOfTrackEntries;

    public FilterParams(
            int clientIf,
            int filtIndex,
            int featSeln,
            int listLogicType,
            int filtLogicType,
            int rssiHighThres,
            int rssiLowThres,
            int delyMode,
            int foundTimeout,
            int lostTimeout,
            int foundTimeoutCnt,
            int numOfTrackingEntries) {

        mClientIf = clientIf;
        mFiltIndex = filtIndex;
        mFeatSeln = featSeln;
        mListLogicType = listLogicType;
        mFiltLogicType = filtLogicType;
        mRssiHighValue = rssiHighThres;
        mRssiLowValue = rssiLowThres;
        mDelyMode = delyMode;
        mFoundTimeOut = foundTimeout;
        mLostTimeOut = lostTimeout;
        mFoundTimeOutCnt = foundTimeoutCnt;
        mNumOfTrackEntries = numOfTrackingEntries;
    }

    public int getClientIf() {
        return mClientIf;
    }

    public int getFiltIndex() {
        return mFiltIndex;
    }

    public int getFeatSeln() {
        return mFeatSeln;
    }

    public int getDelyMode() {
        return mDelyMode;
    }

    public int getListLogicType() {
        return mListLogicType;
    }

    public int getFiltLogicType() {
        return mFiltLogicType;
    }

    public int getRSSIHighValue() {
        return mRssiHighValue;
    }

    public int getRSSILowValue() {
        return mRssiLowValue;
    }

    public int getFoundTimeout() {
        return mFoundTimeOut;
    }

    public int getFoundTimeOutCnt() {
        return mFoundTimeOutCnt;
    }

    public int getLostTimeout() {
        return mLostTimeOut;
    }

    public int getNumOfTrackEntries() {
        return mNumOfTrackEntries;
    }
}
