/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.bluetooth.mcp;

import static com.android.bluetooth.TestUtils.MockitoRule;
import static com.android.bluetooth.TestUtils.getTestDevice;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.*;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothUuid;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.media.session.PlaybackState;
import android.os.ParcelUuid;

import androidx.test.filters.MediumTest;
import androidx.test.runner.AndroidJUnit4;

import com.android.bluetooth.TestUtils;
import com.android.bluetooth.audio_util.MediaData;
import com.android.bluetooth.audio_util.MediaPlayerList;
import com.android.bluetooth.audio_util.MediaPlayerWrapper;
import com.android.bluetooth.audio_util.Metadata;
import com.android.bluetooth.btservice.AdapterService;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.UUID;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class MediaControlProfileTest {
    private final String mFlagDexmarker = System.getProperty("dexmaker.share_classloader", "false");

    private MediaControlProfile mMediaControlProfile;

    private String packageName = "TestPackage";

    private String name = "TestPlayer";
    private CharSequence charSequence = "TestPlayer";
    private MediaControlServiceCallbacks mMcpServiceCallbacks;

    @Rule public final MockitoRule mMockitoRule = new MockitoRule();

    @Mock private AdapterService mAdapterService;
    @Mock private MediaData mMockMediaData;
    @Mock private MediaPlayerList mMockMediaPlayerList;
    @Mock private Metadata mMockMetadata;
    @Mock private MediaPlayerWrapper mMockMediaPlayerWrapper;
    @Mock private PackageManager mMockPackageManager;
    @Mock private ApplicationInfo mMockApplicationInfo;
    @Mock private MediaControlGattServiceInterface mMockGMcsService;
    @Mock private McpService mMockMcpService;

    @Captor private ArgumentCaptor<HashMap> stateMapCaptor;
    @Captor private ArgumentCaptor<Long> positionCaptor;
    @Captor private ArgumentCaptor<MediaControlProfile.ListCallback> listCallbackCaptor;

    @Before
    public void setUp() throws Exception {
        if (!mFlagDexmarker.equals("true")) {
            System.setProperty("dexmaker.share_classloader", "true");
        }

        MediaControlProfile.ListCallback listCallback;

        TestUtils.setAdapterService(mAdapterService);

        mMockMediaData.metadata = mMockMetadata;

        mMockMediaData.state = null;
        mMockMetadata.duration = Long.toString(0);
        mMockMetadata.title = null;
        doReturn(mMockMediaPlayerWrapper).when(mMockMediaPlayerList).getActivePlayer();
        doReturn(mMockMcpService).when(mMockMcpService).getApplicationContext();
        doReturn(mMockPackageManager).when(mMockMcpService).getPackageManager();
        doReturn(packageName).when(mMockMcpService).getPackageName();
        doReturn(name).when(mMockMediaPlayerWrapper).getPackageName();
        doReturn(charSequence).when(mMockApplicationInfo).loadLabel(any(PackageManager.class));
        doReturn(mMockApplicationInfo)
                .when(mMockPackageManager)
                .getApplicationInfo(anyString(), anyInt());

        mMediaControlProfile = new MediaControlProfile(mMockMcpService, mMockMediaPlayerList);

        // this is equivalent of what usually happens inside init class
        mMediaControlProfile.injectGattServiceForTesting(packageName, mMockGMcsService);
        mMediaControlProfile.onServiceInstanceRegistered(ServiceStatus.OK, mMockGMcsService);
        mMcpServiceCallbacks = mMediaControlProfile;

        // Make sure callbacks are not called before it's fully initialized
        verify(mMockMediaPlayerList, times(0)).init(any());
        mMediaControlProfile.init();
        verify(mMockMediaPlayerList).init(listCallbackCaptor.capture());

        listCallback = listCallbackCaptor.getValue();
        listCallback.run(mMockMediaData);
        // Give some time to verify if post function finishes on update player state method call
        // TODO: Is there a possibility to get rid of this timeout?
        verify(mMockGMcsService, timeout(100).times(1)).updatePlayerState(any(HashMap.class));
    }

    @After
    public void tearDown() throws Exception {
        TestUtils.clearAdapterService(mAdapterService);

        if (mMediaControlProfile == null) {
            return;
        }

        mMediaControlProfile.cleanup();
        mMediaControlProfile = null;
        reset(mMockMediaPlayerList);

        if (!mFlagDexmarker.equals("true")) {
            System.setProperty("dexmaker.share_classloader", mFlagDexmarker);
        }
    }

    @Test
    public void testGetCurrentTrackDuration() {
        long duration = 10;

        // Some duration
        mMockMetadata.duration = Long.toString(duration);
        assertThat(mMediaControlProfile.getCurrentTrackDuration()).isEqualTo(duration);

        // No metadata equals no track duration
        mMockMediaData.metadata = null;
        assertThat(mMediaControlProfile.getCurrentTrackDuration())
                .isEqualTo(MediaControlGattServiceInterface.TRACK_DURATION_UNAVAILABLE);
    }

    @Test
    public void testPlayerState2McsState() {
        assertThat(mMediaControlProfile.playerState2McsState(PlaybackState.STATE_PLAYING))
                .isEqualTo(MediaState.PLAYING);
        assertThat(mMediaControlProfile.playerState2McsState(PlaybackState.STATE_NONE))
                .isEqualTo(MediaState.INACTIVE);
        assertThat(mMediaControlProfile.playerState2McsState(PlaybackState.STATE_STOPPED))
                .isEqualTo(MediaState.PAUSED);
        assertThat(mMediaControlProfile.playerState2McsState(PlaybackState.STATE_PAUSED))
                .isEqualTo(MediaState.PAUSED);
        assertThat(mMediaControlProfile.playerState2McsState(PlaybackState.STATE_PLAYING))
                .isEqualTo(MediaState.PLAYING);
        assertThat(mMediaControlProfile.playerState2McsState(PlaybackState.STATE_FAST_FORWARDING))
                .isEqualTo(MediaState.SEEKING);
        assertThat(mMediaControlProfile.playerState2McsState(PlaybackState.STATE_REWINDING))
                .isEqualTo(MediaState.SEEKING);
        assertThat(mMediaControlProfile.playerState2McsState(PlaybackState.STATE_BUFFERING))
                .isEqualTo(MediaState.PAUSED);
        assertThat(mMediaControlProfile.playerState2McsState(PlaybackState.STATE_ERROR))
                .isEqualTo(MediaState.INACTIVE);
        assertThat(mMediaControlProfile.playerState2McsState(PlaybackState.STATE_CONNECTING))
                .isEqualTo(MediaState.INACTIVE);
        assertThat(
                        mMediaControlProfile.playerState2McsState(
                                PlaybackState.STATE_SKIPPING_TO_PREVIOUS))
                .isEqualTo(MediaState.PAUSED);
        assertThat(mMediaControlProfile.playerState2McsState(PlaybackState.STATE_SKIPPING_TO_NEXT))
                .isEqualTo(MediaState.PAUSED);
        assertThat(
                        mMediaControlProfile.playerState2McsState(
                                PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM))
                .isEqualTo(MediaState.PAUSED);
    }

    @Test
    public void testGetLatestTrackPosition() {
        int state = PlaybackState.STATE_PLAYING;
        long position = 10;
        float playback_speed = 1.5f;

        assertThat(mMcpServiceCallbacks.onGetCurrentTrackPosition())
                .isEqualTo(MediaControlGattServiceInterface.TRACK_POSITION_UNAVAILABLE);

        PlaybackState.Builder bob = new PlaybackState.Builder(mMockMediaData.state);
        bob.setState(state, position, playback_speed);
        mMockMediaData.state = bob.build();
        doReturn(mMockMediaData.state).when(mMockMediaPlayerWrapper).getPlaybackState();

        assertThat(mMcpServiceCallbacks.onGetCurrentTrackPosition())
                .isNotEqualTo(MediaControlGattServiceInterface.TRACK_POSITION_UNAVAILABLE);
    }

    @Test
    public void testOnCurrentPlayerStateUpdate() {
        HashMap stateMap;
        int state = PlaybackState.STATE_PLAYING;
        long position = 10;
        float playback_speed = 1.5f;
        long update_time = 77;
        long duration = 10;
        String title = "TestTrackTitle";

        mMockMetadata.duration = Long.toString(duration);
        mMockMetadata.title = title;

        PlaybackState.Builder bob = new PlaybackState.Builder(mMockMediaData.state);
        bob.setState(state, position, playback_speed, update_time);
        mMockMediaData.state = bob.build();

        mMediaControlProfile.onCurrentPlayerStateUpdated(true, true);
        // First time called from ListCallback. Give some time to verify if post function
        // finishes on update player state method call
        // TODO: Is there a possibility to get rid of this timeout?
        verify(mMockGMcsService, timeout(100).times(2)).updatePlayerState(stateMapCaptor.capture());
        stateMap = stateMapCaptor.getValue();

        assertThat(stateMap).containsKey(PlayerStateField.PLAYER_NAME);

        // state changed
        assertThat(stateMap).containsKey(PlayerStateField.PLAYBACK_STATE);
        assertThat(stateMap).containsKey(PlayerStateField.OPCODES_SUPPORTED);
        assertThat(stateMap).containsKey(PlayerStateField.SEEKING_SPEED);
        assertThat(stateMap).containsKey(PlayerStateField.PLAYBACK_SPEED);
        assertThat(stateMap).containsKey(PlayerStateField.TRACK_POSITION);

        // metadata changed
        assertThat(stateMap).containsKey(PlayerStateField.TRACK_DURATION);
        assertThat(stateMap).containsKey(PlayerStateField.TRACK_TITLE);
    }

    private void testHandleTrackPositionSetRequest(long position, long duration, int times) {
        mMcpServiceCallbacks.onTrackPositionSetRequest(position);
        verify(mMockMediaPlayerWrapper, timeout(100).times(times)).seekTo(positionCaptor.capture());

        // position cannot be negative and bigger than track duration
        if (position < 0) assertThat(positionCaptor.getValue().longValue()).isEqualTo(0);
        else if (position > duration) {
            assertThat(positionCaptor.getValue().longValue()).isEqualTo(duration);
        } else {
            assertThat(positionCaptor.getValue().longValue()).isEqualTo(position);
        }
    }

    @Test
    public void testHandleTrackPositionsSetRequest() {
        long duration = 50;
        long actions = PlaybackState.ACTION_SEEK_TO;
        int times = 1;

        mMockMetadata.duration = Long.toString(duration);

        PlaybackState.Builder bob = new PlaybackState.Builder(mMockMediaData.state);
        bob.setActions(actions);
        mMockMediaData.state = bob.build();

        testHandleTrackPositionSetRequest(-duration, duration, times++);
        testHandleTrackPositionSetRequest(duration + duration, duration, times++);
        testHandleTrackPositionSetRequest(duration / 2, duration, times++);

        actions = 0;
        bob.setActions(actions);
        mMockMediaData.state = bob.build();

        mMcpServiceCallbacks.onTrackPositionSetRequest(duration);
        // First time called from ListCallback. Give some time to verify if post function
        // finishes on update player state method call
        // TODO: Is there a possibility to get rid of this timeout?
        verify(mMockGMcsService, timeout(100).times(2)).updatePlayerState(any(HashMap.class));
    }

    @Test
    public void testHandlePlaybackSpeedSetRequest() {
        float speed = 1.5f;
        int times = 1;

        mMcpServiceCallbacks.onPlaybackSpeedSetRequest(speed);
        verify(mMockMediaPlayerWrapper, timeout(100).times(times)).setPlaybackSpeed(anyFloat());

        // Playback speed wouldn't be set if no active player
        doReturn(null).when(mMockMediaPlayerList).getActivePlayer();
        mMcpServiceCallbacks.onPlaybackSpeedSetRequest(speed);
        verify(mMockMediaPlayerWrapper, timeout(100).times(times)).setPlaybackSpeed(anyFloat());
    }

    @Test
    public void testHandleMediaControlRequest() {
        long actions =
                PlaybackState.ACTION_PLAY
                        | PlaybackState.ACTION_PAUSE
                        | PlaybackState.ACTION_STOP
                        | PlaybackState.ACTION_SKIP_TO_PREVIOUS
                        | PlaybackState.ACTION_SKIP_TO_NEXT
                        | PlaybackState.ACTION_REWIND
                        | PlaybackState.ACTION_FAST_FORWARD
                        | PlaybackState.ACTION_SEEK_TO;
        long duration = 10;

        PlaybackState.Builder bob = new PlaybackState.Builder(mMockMediaData.state);
        bob.setActions(actions);
        mMockMediaData.state = bob.build();

        Request request = new Request(Request.Opcodes.PLAY, 0);
        mMcpServiceCallbacks.onMediaControlRequest(request);
        verify(mMockMediaPlayerWrapper, timeout(100)).playCurrent();
        request = new Request(Request.Opcodes.PAUSE, 0);
        mMcpServiceCallbacks.onMediaControlRequest(request);
        verify(mMockMediaPlayerWrapper, timeout(100)).pauseCurrent();
        request = new Request(Request.Opcodes.STOP, 0);
        mMcpServiceCallbacks.onMediaControlRequest(request);
        verify(mMockMediaPlayerWrapper, timeout(100)).seekTo(0);
        verify(mMockMediaPlayerWrapper).stopCurrent();
        request = new Request(Request.Opcodes.PREVIOUS_TRACK, 0);
        mMcpServiceCallbacks.onMediaControlRequest(request);
        verify(mMockMediaPlayerWrapper, timeout(100)).skipToPrevious();
        request = new Request(Request.Opcodes.NEXT_TRACK, 0);
        mMcpServiceCallbacks.onMediaControlRequest(request);
        verify(mMockMediaPlayerWrapper, timeout(100)).skipToNext();
        request = new Request(Request.Opcodes.FAST_REWIND, 0);
        mMcpServiceCallbacks.onMediaControlRequest(request);
        verify(mMockMediaPlayerWrapper, timeout(100)).rewind();
        request = new Request(Request.Opcodes.FAST_FORWARD, 0);
        mMcpServiceCallbacks.onMediaControlRequest(request);
        verify(mMockMediaPlayerWrapper, timeout(100)).fastForward();

        mMockMetadata.duration = Long.toString(duration);
        assertThat(mMediaControlProfile.getCurrentTrackDuration()).isEqualTo(duration);
        request = new Request(Request.Opcodes.MOVE_RELATIVE, 100);
        mMcpServiceCallbacks.onMediaControlRequest(request);
        verify(mMockMediaPlayerWrapper, timeout(100)).seekTo(duration);

        // Verify toggle-style play/pause control support
        clearInvocations(mMockMediaPlayerWrapper);
        actions = PlaybackState.ACTION_PLAY_PAUSE;
        bob = new PlaybackState.Builder(mMockMediaData.state);
        bob.setActions(actions);
        mMockMediaData.state = bob.build();

        request = new Request(Request.Opcodes.PLAY, 0);
        mMcpServiceCallbacks.onMediaControlRequest(request);
        verify(mMockMediaPlayerWrapper, timeout(100)).playCurrent();
        request = new Request(Request.Opcodes.PAUSE, 0);
        mMcpServiceCallbacks.onMediaControlRequest(request);
        verify(mMockMediaPlayerWrapper, timeout(100)).pauseCurrent();
    }

    @Test
    public void testAvrcpCompatibleActionSet() {
        long actions = PlaybackState.ACTION_SET_RATING;
        PlaybackState.Builder bob = new PlaybackState.Builder(mMockMediaData.state);
        bob.setActions(actions);
        mMockMediaData.state = bob.build();

        // Same base feature set as the player item features defined in `avrcp/get_foder_items.cc`
        final long baseFeatures =
                PlaybackState.ACTION_PLAY
                        | PlaybackState.ACTION_STOP
                        | PlaybackState.ACTION_PAUSE
                        | PlaybackState.ACTION_REWIND
                        | PlaybackState.ACTION_FAST_FORWARD
                        | PlaybackState.ACTION_SKIP_TO_NEXT
                        | PlaybackState.ACTION_SKIP_TO_PREVIOUS;
        assertThat(mMediaControlProfile.getCurrentPlayerSupportedActions())
                .isEqualTo(actions | baseFeatures);
    }

    @Test
    public void testPlayerActions2McsSupportedOpcodes() {
        long actions =
                PlaybackState.ACTION_PLAY
                        | PlaybackState.ACTION_PAUSE
                        | PlaybackState.ACTION_STOP
                        | PlaybackState.ACTION_SKIP_TO_PREVIOUS
                        | PlaybackState.ACTION_SKIP_TO_NEXT
                        | PlaybackState.ACTION_REWIND
                        | PlaybackState.ACTION_FAST_FORWARD
                        | PlaybackState.ACTION_SEEK_TO;
        int opcodes_supported =
                Request.SupportedOpcodes.STOP
                        | Request.SupportedOpcodes.PAUSE
                        | Request.SupportedOpcodes.PLAY
                        | Request.SupportedOpcodes.FAST_REWIND
                        | Request.SupportedOpcodes.PREVIOUS_TRACK
                        | Request.SupportedOpcodes.NEXT_TRACK
                        | Request.SupportedOpcodes.FAST_FORWARD
                        | Request.SupportedOpcodes.MOVE_RELATIVE;

        assertThat(mMediaControlProfile.playerActions2McsSupportedOpcodes(actions))
                .isEqualTo(opcodes_supported);

        // Verify toggle-style play/pause control support
        actions = PlaybackState.ACTION_PLAY_PAUSE;
        opcodes_supported = Request.SupportedOpcodes.PAUSE | Request.SupportedOpcodes.PLAY;

        assertThat(mMediaControlProfile.playerActions2McsSupportedOpcodes(actions))
                .isEqualTo(opcodes_supported);
    }

    @Test
    public void testProcessPendingPlayerStateRequest() {
        HashMap stateMap;
        int state = PlaybackState.STATE_PLAYING;
        long position = 10;
        float playback_speed = 1.5f;

        PlaybackState.Builder bob = new PlaybackState.Builder(mMockMediaData.state);
        bob.setState(state, position, playback_speed);
        mMockMediaData.state = bob.build();
        doReturn(mMockMediaData.state).when(mMockMediaPlayerWrapper).getPlaybackState();

        PlayerStateField[] state_fields =
                new PlayerStateField[] {
                    PlayerStateField.PLAYBACK_STATE,
                    PlayerStateField.TRACK_DURATION,
                    PlayerStateField.PLAYBACK_SPEED,
                    PlayerStateField.SEEKING_SPEED,
                    PlayerStateField.PLAYING_ORDER,
                    PlayerStateField.TRACK_POSITION,
                    PlayerStateField.PLAYER_NAME,
                    PlayerStateField.PLAYING_ORDER_SUPPORTED,
                    PlayerStateField.OPCODES_SUPPORTED
                };

        mMcpServiceCallbacks.onPlayerStateRequest(state_fields);
        // First time called from ListCallback. Give some time to verify if post function
        // finishes on update player state method call
        // TODO: Is there a possibility to get rid of this timeout?
        verify(mMockGMcsService, timeout(100).times(2)).updatePlayerState(stateMapCaptor.capture());
        stateMap = stateMapCaptor.getValue();

        assertThat(stateMap).containsKey(PlayerStateField.PLAYBACK_STATE);
        assertThat(stateMap).containsKey(PlayerStateField.TRACK_DURATION);
        assertThat(stateMap).containsKey(PlayerStateField.PLAYBACK_SPEED);
        assertThat(stateMap).containsKey(PlayerStateField.SEEKING_SPEED);
        assertThat(stateMap).containsKey(PlayerStateField.PLAYING_ORDER);
        assertThat(stateMap).containsKey(PlayerStateField.TRACK_POSITION);
        assertThat(stateMap).containsKey(PlayerStateField.PLAYER_NAME);
        assertThat(stateMap).containsKey(PlayerStateField.PLAYING_ORDER_SUPPORTED);
        assertThat(stateMap).containsKey(PlayerStateField.OPCODES_SUPPORTED);
    }

    private void testGetCurrentPlayerPlayingOrder(
            PlayingOrder expected_value, boolean is_shuffle_set, boolean is_repeat_set) {
        doReturn(is_shuffle_set).when(mMockMediaPlayerWrapper).isShuffleSet();
        doReturn(is_repeat_set).when(mMockMediaPlayerWrapper).isRepeatSet();
        assertThat(mMediaControlProfile.getCurrentPlayerPlayingOrder()).isEqualTo(expected_value);
    }

    @Test
    public void testGetCurrentPlayerPlayingOrders() {
        testGetCurrentPlayerPlayingOrder(PlayingOrder.SHUFFLE_REPEAT, true, true);
        testGetCurrentPlayerPlayingOrder(PlayingOrder.SHUFFLE_ONCE, true, false);
        testGetCurrentPlayerPlayingOrder(PlayingOrder.IN_ORDER_REPEAT, false, true);
        testGetCurrentPlayerPlayingOrder(PlayingOrder.IN_ORDER_ONCE, false, false);
    }

    private void testGetSupportedPlayingOrder(boolean is_shuffle_set, boolean is_repeat_set) {
        int expected_value = SupportedPlayingOrder.IN_ORDER_ONCE;

        if (is_repeat_set) expected_value |= SupportedPlayingOrder.IN_ORDER_REPEAT;
        if (is_shuffle_set) {
            if (is_repeat_set) expected_value |= SupportedPlayingOrder.SHUFFLE_REPEAT;
            else expected_value |= SupportedPlayingOrder.SHUFFLE_ONCE;
        }

        doReturn(is_shuffle_set).when(mMockMediaPlayerWrapper).isShuffleSupported();
        doReturn(is_repeat_set).when(mMockMediaPlayerWrapper).isRepeatSupported();
        assertThat(mMediaControlProfile.getSupportedPlayingOrder().intValue())
                .isEqualTo(expected_value);
    }

    @Test
    public void testGetSupportedPlayingOrders() {
        testGetSupportedPlayingOrder(true, true);
        testGetSupportedPlayingOrder(true, false);
        testGetSupportedPlayingOrder(false, true);
        testGetSupportedPlayingOrder(false, false);
    }

    @Test
    public void testGmcsSetGetNotificationSubscriptionDoesNotCrash() {
        final ParcelUuid charUuid1 = new ParcelUuid(UUID.randomUUID());
        final int ccid1 = BluetoothDevice.METADATA_GMCS_CCCD;

        doReturn(ccid1).when(mMockGMcsService).getContentControlId();
        doReturn(BluetoothUuid.GENERIC_MEDIA_CONTROL.getUuid())
                .when(mMockGMcsService)
                .getServiceUuid();

        // BluetoothDevice class is not mockable
        BluetoothDevice bluetoothDevice = getTestDevice(0);
        mMediaControlProfile.setNotificationSubscription(ccid1, bluetoothDevice, charUuid1, true);
        assertThat(mMediaControlProfile.getNotificationSubscriptions(ccid1, bluetoothDevice))
                .isNotNull();
    }

    @Test
    public void testDumpDoesNotCrash() {
        mMediaControlProfile.dump(new StringBuilder());
    }
}
