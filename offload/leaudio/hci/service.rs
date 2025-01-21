// Copyright 2024, The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

use android_hardware_bluetooth_offload_leaudio::{aidl, binder};

use crate::arbiter::Arbiter;
use aidl::android::hardware::bluetooth::offload::leaudio::{
    IHciProxy::{BnHciProxy, BpHciProxy, IHciProxy},
    IHciProxyCallbacks::IHciProxyCallbacks,
};
use binder::{
    BinderFeatures, DeathRecipient, ExceptionCode, Interface, Result as BinderResult, Strong,
};
use bluetooth_offload_hci::IsoData;
use std::collections::HashMap;
use std::sync::{Arc, Mutex, Weak};

pub(crate) use aidl::android::hardware::bluetooth::offload::leaudio::StreamConfiguration::StreamConfiguration;

pub(crate) struct Service {
    state: Arc<Mutex<State>>,
}

#[derive(Default)]
struct State {
    arbiter: Weak<Arbiter>,
    callbacks: Option<Strong<dyn IHciProxyCallbacks>>,
    streams: HashMap<u16, StreamConfiguration>,
}

impl Service {
    pub(crate) fn new() -> Self {
        let state = Arc::new(Mutex::new(State::default()));
        HciProxy::register(state.clone());
        Self { state }
    }

    pub(crate) fn reset(&self, arbiter: Weak<Arbiter>) {
        let mut state = self.state.lock().unwrap();
        *state = State { arbiter, ..Default::default() }
    }

    pub(crate) fn start_stream(&self, handle: u16, config: StreamConfiguration) {
        let mut state = self.state.lock().unwrap();
        if let Some(callbacks) = &state.callbacks {
            let _ = callbacks.startStream(handle.into(), &config);
        } else {
            log::warn!("Stream started without registered client");
        };
        state.streams.insert(handle, config);
    }

    pub(crate) fn stop_stream(&self, handle: u16) {
        let mut state = self.state.lock().unwrap();
        state.streams.remove(&handle);
        if let Some(callbacks) = &state.callbacks {
            let _ = callbacks.stopStream(handle.into());
        };
    }
}

struct HciProxy {
    state: Arc<Mutex<State>>,
    _death_recipient: DeathRecipient,
}

impl Interface for HciProxy {}

impl HciProxy {
    fn register(state: Arc<Mutex<State>>) {
        let death_recipient = {
            let state = state.clone();
            DeathRecipient::new(move || {
                log::info!("Client has died");
                state.lock().unwrap().callbacks = None;
            })
        };

        binder::add_service(
            &format!("{}/default", BpHciProxy::get_descriptor()),
            BnHciProxy::new_binder(
                Self { state, _death_recipient: death_recipient },
                BinderFeatures::default(),
            )
            .as_binder(),
        )
        .expect("Failed to register service");
    }
}

impl IHciProxy for HciProxy {
    fn registerCallbacks(&self, callbacks: &Strong<dyn IHciProxyCallbacks>) -> BinderResult<()> {
        let mut state = self.state.lock().unwrap();
        state.callbacks = Some(callbacks.clone());
        for (handle, config) in &state.streams {
            let _ = callbacks.startStream((*handle).into(), config);
        }

        Ok(())
    }

    fn sendPacket(&self, handle: i32, seqnum: i32, data: &[u8]) -> BinderResult<()> {
        let handle: u16 = handle.try_into().map_err(|_| ExceptionCode::ILLEGAL_ARGUMENT)?;
        let seqnum: u16 = seqnum.try_into().map_err(|_| ExceptionCode::ILLEGAL_ARGUMENT)?;

        let state = self.state.lock().unwrap();
        if let Some(arbiter) = state.arbiter.upgrade() {
            arbiter.push_audio(&IsoData::new(handle, seqnum, data));
        } else {
            log::warn!("Trashing packet received in bad state");
        }

        Ok(())
    }
}
