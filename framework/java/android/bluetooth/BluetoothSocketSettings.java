/*
 * Copyright 2024 The Android Open Source Project
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

package android.bluetooth;

import static android.bluetooth.BluetoothSocket.SocketType;

import android.annotation.FlaggedApi;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresNoPermission;

import com.android.bluetooth.flags.Flags;

import java.util.UUID;

/**
 * Defines parameters for creating Bluetooth server and client socket channels.
 *
 * <p>Used with {@link BluetoothAdapter#listenUsingSocketSettings} to create a server socket and
 * {@link BluetoothDevice#createUsingSocketSettings} to create a client socket.
 *
 * @see BluetoothAdapter#listenUsingSocketSettings
 * @see BluetoothDevice#createUsingSocketSettings
 */
@FlaggedApi(Flags.FLAG_SOCKET_SETTINGS_API)
public final class BluetoothSocketSettings {

    private static final int L2CAP_PSM_UNSPECIFIED = -1;

    /** Type of the Bluetooth socket */
    @SocketType private int mSocketType;

    /** Encryption requirement for the Bluetooth socket. */
    private boolean mEncryptionRequired;

    /** Authentication requirement for the Bluetooth socket. */
    private boolean mAuthenticationRequired;

    /** L2CAP Protocol/Service Multiplexer (PSM) for the Bluetooth Socket. */
    private int mL2capPsm;

    /** RFCOMM service name associated with the Bluetooth socket. */
    private String mRfcommServiceName;

    /** RFCOMM service UUID associated with the Bluetooth socket. */
    private UUID mRfcommUuid;

    /**
     * Returns the type of the Bluetooth socket.
     *
     * <p>Defaults to {@code BluetoothSocket#TYPE_RFCOMM}.
     */
    @RequiresNoPermission
    @SocketType
    public int getSocketType() {
        return mSocketType;
    }

    /** Returns the L2CAP PSM value used for a BluetoothSocket#TYPE_LE socket. */
    @RequiresNoPermission
    public @IntRange(from = 128, to = 255) int getL2capPsm() {
        return mL2capPsm;
    }

    /**
     * Returns the RFCOMM service name used for a BluetoothSocket#TYPE_RFCOMM socket.
     *
     * <p>Defaults to {@code null}.
     */
    @Nullable
    @RequiresNoPermission
    public String getRfcommServiceName() {
        return mRfcommServiceName;
    }

    /**
     * Returns the RFCOMM service UUID used for a BluetoothSocket#TYPE_RFCOMM socket.
     *
     * <p>Defaults to {@code null}.
     */
    @Nullable
    @RequiresNoPermission
    public UUID getRfcommUuid() {
        return mRfcommUuid;
    }

    /**
     * Checks if encryption is enabled for the Bluetooth socket.
     *
     * <p>Defaults to {@code false}.
     */
    @RequiresNoPermission
    public boolean isEncryptionRequired() {
        return mEncryptionRequired;
    }

    /**
     * Checks if authentication is enabled for the Bluetooth socket.
     *
     * <p>Defaults to {@code false}.
     */
    @RequiresNoPermission
    public boolean isAuthenticationRequired() {
        return mAuthenticationRequired;
    }

    /**
     * Returns a {@link String} that describes each BluetoothSocketSettings parameter current value.
     */
    @Override
    public String toString() {
        if (mSocketType == BluetoothSocket.TYPE_RFCOMM) {
            return "BluetoothSocketSettings{"
                    + "mSocketType="
                    + mSocketType
                    + ", mEncryptionRequired="
                    + mEncryptionRequired
                    + ", mAuthenticationRequired="
                    + mAuthenticationRequired
                    + ", mRfcommServiceName="
                    + mRfcommServiceName
                    + ", mRfcommUuid="
                    + mRfcommUuid
                    + "}";
        } else {
            return "BluetoothSocketSettings{"
                    + "mSocketType="
                    + mSocketType
                    + ", mL2capPsm="
                    + mL2capPsm
                    + ", mEncryptionRequired="
                    + mEncryptionRequired
                    + ", mAuthenticationRequired="
                    + mAuthenticationRequired
                    + "}";
        }
    }

    private BluetoothSocketSettings(
            int socketType,
            int l2capPsm,
            boolean encryptionRequired,
            boolean authenticationRequired,
            String rfcommServiceName,
            UUID rfcommUuid) {
        mSocketType = socketType;
        mL2capPsm = l2capPsm;
        mEncryptionRequired = encryptionRequired;
        mAuthenticationRequired = authenticationRequired;
        mRfcommUuid = rfcommUuid;
        mRfcommServiceName = rfcommServiceName;
    }

    /** Builder for {@link BluetoothSocketSettings}. */
    @FlaggedApi(Flags.FLAG_SOCKET_SETTINGS_API)
    public static final class Builder {
        private int mSocketType = BluetoothSocket.TYPE_RFCOMM;
        private int mL2capPsm = L2CAP_PSM_UNSPECIFIED;
        private boolean mEncryptionRequired = false;
        private boolean mAuthenticationRequired = false;
        private String mRfcommServiceName = null;
        private UUID mRfcommUuid = null;

        public Builder() {}

        /**
         * Sets the socket type.
         *
         * <p>Must be one of:
         *
         * <ul>
         *   <li>{@link BluetoothSocket#TYPE_RFCOMM}
         *   <li>{@link BluetoothSocket#TYPE_LE}
         * </ul>
         *
         * <p>Defaults to {@code BluetoothSocket#TYPE_RFCOMM}.
         *
         * @param socketType The type of socket.
         * @return This builder.
         * @throws IllegalArgumentException If {@code socketType} is invalid.
         */
        @NonNull
        @RequiresNoPermission
        public Builder setSocketType(@SocketType int socketType) {
            if (socketType != BluetoothSocket.TYPE_RFCOMM
                    && socketType != BluetoothSocket.TYPE_LE) {
                throw new IllegalArgumentException("invalid socketType - " + socketType);
            }
            mSocketType = socketType;
            return this;
        }

        /**
         * Sets the L2CAP PSM (Protocol/Service Multiplexer) for the Bluetooth socket.
         *
         * <p>This is only used for {@link BluetoothSocket#TYPE_LE} sockets.
         *
         * <p>Valid PSM values for {@link BluetoothSocket#TYPE_LE} sockets is ranging from 128
         * (0x80) to 255 (0xFF).
         *
         * <p>Application using this API is responsible for obtaining protocol/service multiplexer
         * (PSM) value from remote device.
         *
         * @param l2capPsm The L2CAP PSM value.
         * @return This builder.
         * @throws IllegalArgumentException If l2cap PSM is not in given range.
         */
        @NonNull
        @RequiresNoPermission
        public Builder setL2capPsm(@IntRange(from = 128, to = 255) int l2capPsm) {
            if (l2capPsm < 128 || l2capPsm > 255) {
                throw new IllegalArgumentException("invalid L2cap PSM - " + l2capPsm);
            }
            mL2capPsm = l2capPsm;
            return this;
        }

        /**
         * Sets the encryption requirement for the Bluetooth socket.
         *
         * <p>Defaults to {@code false}.
         *
         * @param encryptionRequired {@code true} if encryption is required for this socket, {@code
         *     false} otherwise.
         * @return This builder.
         */
        @NonNull
        @RequiresNoPermission
        public Builder setEncryptionRequired(boolean encryptionRequired) {
            mEncryptionRequired = encryptionRequired;
            return this;
        }

        /**
         * Sets the authentication requirement for the Bluetooth socket.
         *
         * <p>Defaults to {@code false}.
         *
         * @param authenticationRequired {@code true} if authentication is required for this socket,
         *     {@code false} otherwise.
         * @return This builder.
         */
        @NonNull
        @RequiresNoPermission
        public Builder setAuthenticationRequired(boolean authenticationRequired) {
            mAuthenticationRequired = authenticationRequired;
            return this;
        }

        /**
         * Sets the RFCOMM service name associated with the Bluetooth socket.
         *
         * <p>This name is used to identify the service when a remote device searches for it using
         * SDP.
         *
         * <p>This is only used for {@link BluetoothSocket#TYPE_RFCOMM} sockets.
         *
         * <p>Defaults to {@code null}.
         *
         * @param rfcommServiceName The RFCOMM service name.
         * @return This builder.
         */
        @NonNull
        @RequiresNoPermission
        public Builder setRfcommServiceName(@NonNull String rfcommServiceName) {
            mRfcommServiceName = rfcommServiceName;
            return this;
        }

        /**
         * Sets the RFCOMM service UUID associated with the Bluetooth socket.
         *
         * <p>This UUID is used to uniquely identify the service when a remote device searches for
         * it using SDP.
         *
         * <p>This is only used for {@link BluetoothSocket#TYPE_RFCOMM} sockets.
         *
         * <p>Defaults to {@code null}.
         *
         * @param rfcommUuid The RFCOMM service UUID.
         * @return This builder.
         */
        @NonNull
        @RequiresNoPermission
        public Builder setRfcommUuid(@NonNull UUID rfcommUuid) {
            mRfcommUuid = rfcommUuid;
            return this;
        }

        /**
         * Builds a {@link BluetoothSocketSettings} object.
         *
         * @return A new {@link BluetoothSocketSettings} object with the configured parameters.
         * @throws IllegalArgumentException on invalid parameters
         */
        @NonNull
        @RequiresNoPermission
        public BluetoothSocketSettings build() {
            if (mSocketType == BluetoothSocket.TYPE_RFCOMM) {
                if (mRfcommUuid == null) {
                    throw new IllegalArgumentException("RFCOMM socket with missing uuid");
                }
                if (mL2capPsm != L2CAP_PSM_UNSPECIFIED) {
                    throw new IllegalArgumentException(
                            "Invalid Socket config: "
                                    + " Socket type: "
                                    + mSocketType
                                    + " L2cap PSM: "
                                    + mL2capPsm);
                }
            }
            if (mSocketType == BluetoothSocket.TYPE_LE) {
                if (mRfcommUuid != null) {
                    throw new IllegalArgumentException(
                            "Invalid Socket config: "
                                    + "Socket type: "
                                    + mSocketType
                                    + " Rfcomm Service Name: "
                                    + mRfcommServiceName
                                    + " Rfcomm Uuid: "
                                    + mRfcommUuid);
                }
            }

            return new BluetoothSocketSettings(
                    mSocketType,
                    mL2capPsm,
                    mEncryptionRequired,
                    mAuthenticationRequired,
                    mRfcommServiceName,
                    mRfcommUuid);
        }
    }
}
