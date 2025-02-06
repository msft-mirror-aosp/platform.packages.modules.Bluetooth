/*
 * Copyright (C) 2025 The Android Open Source Project
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

package android.bluetooth.pairing.utils;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import static java.util.Objects.requireNonNull;

import android.annotation.NonNull;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.google.common.collect.Iterators;
import org.hamcrest.Matcher;
import org.hamcrest.core.AllOf;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.hamcrest.MockitoHamcrest;

import java.time.Duration;
import java.util.Arrays;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * IntentReceiver helps in managing the Intents received through the Broadcast
 *  receiver, with specific intent actions registered.
 *  It uses Builder pattern for instance creation, and also allows setting up
 *  a custom listener's onReceive().
 *
 * Use the following way to create an instance of the IntentReceiver.
 *      IntentReceiver intentReceiver = new IntentReceiver.Builder(sTargetContext,
 *          BluetoothDevice.ACTION_1,
 *          BluetoothDevice.ACTION_2)
 *          .setIntentListener(--) // optional
 *          .setIntentTimeout(--)  // optional
 *          .build();
 *
 * Ordered and unordered verification mechanisms are also provided through public methods.
 */

public class IntentReceiver {
    private static final String TAG = IntentReceiver.class.getSimpleName();

    /** Interface for listening & processing the received intents */
    public interface IntentListener {
        /**
         * Callback for receiving intents
         *
         * @param intent Received intent
         */
        void onReceive(Intent intent);
    }

    @Mock private BroadcastReceiver mReceiver;

    /** Intent timeout value, can be configured through constructor, or setter method */
    private final Duration mIntentTimeout;

    /** To verify the received intents in-order */
    private final InOrder mInOrder;
    private final Context mContext;
    private final String[] mIntentStrings;
    private final Deque<IntentFilter> mDqIntentFilter;
    /*
     * Note: Since we are using Builder pattern, also add new variables added
     *  to the Builder class
     */

    /** Listener for the received intents */
    private final IntentListener mIntentListener;

    /**
     * Creates an Intent receiver from the builder instance
     * Note: This is a private constructor, so always prepare IntentReceiver's
     *  instance through Builder().
     *
     * @param builder Pre-built builder instance
     */
    private IntentReceiver(Builder builder) {
        this.mIntentTimeout = builder.mIntentTimeout;
        this.mContext = builder.mContext;
        this.mIntentStrings = builder.mIntentStrings;
        this.mIntentListener = builder.mIntentListener;

        /* Perform other calls required for instantiation */
        MockitoAnnotations.initMocks(this);
        mInOrder = inOrder(mReceiver);
        mDqIntentFilter = new ArrayDeque<>();
        mDqIntentFilter.addFirst(prepareIntentFilter(mIntentStrings));

        setupListener();
        registerReceiver();
    }

    /** Private constructor to avoid creation of IntentReceiver instance directly */
    private IntentReceiver() {
        mIntentTimeout = null;
        mInOrder = null;
        mContext = null;
        mIntentStrings = null;
        mDqIntentFilter = null;
        mIntentListener = null;
    }

    /**
     * Builder class which helps in avoiding overloading constructors (as the class grows)
     * Usage:
     *      new IntentReceiver.Builder(ARGS)
     *      .setterMethods() **Optional calls, as these are default params
     *      .build();
     */
    public static class Builder {
        /**
         * Add all the instance variables from IntentReceiver,
         *  which needs to be initiated from the constructor,
         *  with either default, or user provided value.
         */
        private final Context mContext;
        private final String[] mIntentStrings;

        /** Non-final variables as there are setters available */
        private Duration mIntentTimeout;
        private IntentListener mIntentListener;

        /**
         * Private default constructor to avoid creation of Builder default
         *  instance directly as we need some instance variables to be initiated
         *  with user defined values.
         */
        private Builder() {
            mContext = null;
            mIntentStrings = null;
        }

        /**
         * Creates a Builder instance with following required params
         *
         * @param context Context
         * @param intentStrings Array of intents to filter and register
         */
        public Builder(@NonNull Context context, String... intentStrings) {
            mContext = context;
            mIntentStrings = requireNonNull(intentStrings,
                "IntentReceiver.Builder(): Intent string cannot be null");

            if (mIntentStrings.length == 0) {
                throw new RuntimeException("IntentReceiver.Builder(): No intents to register");
            }

            /* Default values for remaining vars */
            mIntentTimeout = Duration.ofSeconds(10);
            mIntentListener = null;
        }

        public Builder setIntentListener(IntentListener intentListener) {
            mIntentListener = intentListener;
            return this;
        }

        public Builder setIntentTimeout(Duration intentTimeout) {
            mIntentTimeout = intentTimeout;
            return this;
        }

        /**
         * Builds and returns the IntentReceiver object with all the passed,
         *  and default params supplied to Builder().
         */
        public IntentReceiver build() {
            return new IntentReceiver(this);
        }
    }

    /**
     * Verifies if the intent is received in order
     *
     * @param matchers Matchers
     */
    public void verifyReceivedOrdered(Matcher<Intent>... matchers) {
        mInOrder.verify(mReceiver, timeout(mIntentTimeout.toMillis()))
                .onReceive(any(Context.class), MockitoHamcrest.argThat(AllOf.allOf(matchers)));
    }

    /**
     * Verifies if requested number of intents are received (unordered)
     *
     * @param num Number of intents
     * @param matchers Matchers
     */
    public void verifyReceived(int num, Matcher<Intent>... matchers) {
        verify(mReceiver, timeout(mIntentTimeout.toMillis()).times(num))
                .onReceive(any(Context.class), MockitoHamcrest.argThat(AllOf.allOf(matchers)));
    }

    /**
     * Verifies if the intent is received (unordered)
     *
     * @param matchers Matchers
     */
    public void verifyReceived(Matcher<Intent>... matchers) {
        verifyReceived(1, matchers);
    }

    /**
     * This function will make sure that the instance is properly cleared
     *  based on the registered actions.
     * Note: This function MUST be called before returning from the caller function,
     *  as this either unregisters the latest registered actions, or free resources.
     */
    public void close() {
        Log.d(TAG, "close(): " + mDqIntentFilter.size());

        /* More than 1 IntentFilters are present */
        if(mDqIntentFilter.size() > 1) {
            /*
             * It represents there are IntentFilters present to be rolled back.
             * So, unregister and roll back to previous IntentFilter.
             */
            unregisterRecentAllIntentActions();
        }
        else {
            /*
             * It represents that this close() is called in the scope of creation of
             *  the object, and hence there is only 1 IntentFilter which is present.
             *  So, we can safely close this instance.
             */
            verifyNoMoreInteractions();
            unregisterReceiver();
        }
    }

    /**
     * Registers the new actions passed as argument.
     * 1. Unregister the receiver, and in turn old IntentFilter.
     * 2. Creates a new IntentFilter from the String[], and treat that as latest.
     * 3. Registers the new IntentFilter with the receiver to the current context.
     */
    public void registerIntentActions(String... intentStrings) {
        IntentFilter intentFilter = prepareIntentFilter(intentStrings);

        unregisterReceiver();
        /* Pushes the new intentFilter to top to make it the latest registered */
        mDqIntentFilter.addFirst(intentFilter);
        registerReceiver();
    }

    /**
     * Helper function to register intent actions, and get the IntentReceiver
     *  instance.
     *
     * @param parentIntentReceiver IntentReceiver instance from the parent test caller
     *  This should be `null` if there is no parent IntentReceiver instance.
     * @param targetContext Context instance
     * @param intentStrings Intent actions string array
     *
     * This should be used to register new intent actions in a testStep
     *  function always.
     */
    public static IntentReceiver updateNewIntentActionsInParentReceiver(
        IntentReceiver parentIntentReceiver, Context targetContext, String... intentStrings) {
        /*
         * If parentIntentReceiver is NULL, it indicates that the caller
         *  is a fresh test/testStep and a new IntentReceiver will be returned.
         *  else, update the intent actions and return the same instance.
         */
        // Create a new instance for the current test/testStep function.
        if(parentIntentReceiver == null)
            return new IntentReceiver.Builder(targetContext, intentStrings)
                .build();

        /* Update the intent actions in the parent IntentReceiver instance */
        parentIntentReceiver.registerIntentActions(intentStrings);
        return parentIntentReceiver;
    }

    /** Helper functions are added below, usually private */

    /** Registers the listener for the received intents, and perform a custom logic as required */
    private void setupListener() {
        doAnswer(
                inv -> {
                    Log.d(
                            TAG,
                            "onReceive(): intent=" +
                                Arrays.toString(inv.getArguments()));

                    if (mIntentListener == null) return null;

                    Intent intent = inv.getArgument(1);

                    /* Custom `onReceive` will be provided by the caller */
                    mIntentListener.onReceive(intent);
                    return null;
                })
            .when(mReceiver)
            .onReceive(any(), any());
    }

    private IntentFilter prepareIntentFilter(String... intentStrings) {
        IntentFilter intentFilter = new IntentFilter();
        for (String intentString : intentStrings) {
          intentFilter.addAction(intentString);
        }

        return intentFilter;
    }

    /**
     * Registers the latest intent filter which is at the deque.peekFirst()
     * Note: The mDqIntentFilter must not be empty here.
     */
    private void registerReceiver() {
        Log.d(TAG, "registerReceiver(): Registering for intents: " +
            getActionsFromIntentFilter(mDqIntentFilter.peekFirst()));

        /* ArrayDeque should not be empty at all while registering a receiver */
        assertThat(mDqIntentFilter.isEmpty()).isFalse();
        mContext.registerReceiver(mReceiver,
            (IntentFilter)mDqIntentFilter.peekFirst());
    }

    /**
     * Unregisters the receiver from the list of active receivers.
     * Also, we can now re-use the same receiver, or register a new
     *  receiver with the same or different intent filter, the old
     *  registration is no longer valid.
     *  Source: Intents and intent filters (Android Developers)
     */
    private void unregisterReceiver() {
        Log.d(TAG, "unregisterReceiver()");
        mContext.unregisterReceiver(mReceiver);
    }

    /** Verifies that no more intents are received */
    private void verifyNoMoreInteractions() {
        Log.d(TAG, "verifyNoMoreInteractions()");
        Mockito.verifyNoMoreInteractions(mReceiver);
    }

    /**
     * Registers the new actions passed as argument.
     * 1. Unregister the receiver, and in turn new IntentFilter.
     * 2. Pops the new IntentFilter to roll-back to the old one.
     * 3. Registers the old IntentFilter with the receiver to the current context.
     */
    private void unregisterRecentAllIntentActions() {
        assertThat(mDqIntentFilter.isEmpty()).isFalse();

        unregisterReceiver();
        /* Restores the previous intent filter, and discard the latest */
        mDqIntentFilter.removeFirst();
        registerReceiver();
    }

    /**
     * Helper function to get the actions from the IntentFilter
     *
     * @param intentFilter IntentFilter instance
     *
     * This is a helper function to get the actions from the IntentFilter,
     *  and return as a String.
     */
    private String getActionsFromIntentFilter(
        IntentFilter intentFilter) {
        Iterator<String> iterator = intentFilter.actionsIterator();
        StringBuilder allIntentActions = new StringBuilder();
        while (iterator.hasNext()) {
            allIntentActions.append(iterator.next() + ", ");
        }

        return allIntentActions.toString();
    }
}