// Copyright 2022, The Android Open Source Project
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

//! The core event loop for Rust modules. Here Rust modules are started in
//! dependency order.

use gatt::{channel::AttTransport, GattCallbacks};
use log::{error, info, warn};
use tokio::task::LocalSet;

use std::{rc::Rc, sync::Mutex};
use tokio::runtime::Builder;

use tokio::sync::mpsc;

pub mod core;
pub mod gatt;
pub mod packets;
pub mod utils;

/// The Rust Modules runner. Starts and processes messages from Java / C++
/// while the Rust thread is running.  Starts in an idle state.
#[derive(Default, Debug)]
enum RustModuleRunner {
    /// Not started yet
    #[default]
    NotStarted,
    /// Main event loop is running and messages can be processed.
    /// Use [`RustModuleRunner::send`] to queue a callback to be sent.
    Running { tx: mpsc::UnboundedSender<BoxedMainThreadCallback> },
    /// The event loop has been asked to stop from the FFI interface and will stop when all
    /// messages in the queue are processed. No further messages can be sent.
    Stopping,
    /// The event loop has ended.  `result` holds an error if the thread ended not gracefully.
    Ended { result: Result<(), String> },
}

/// The ModuleViews lets us access all publicly accessible Rust modules from
/// Java / C++ while the stack is running. If a module should not be exposed
/// outside of Rust GD, there is no need to include it here.
pub struct ModuleViews<'a> {
    /// Lets us call out into C++
    pub gatt_outgoing_callbacks: Rc<dyn GattCallbacks>,
    /// Receives synchronous callbacks from JNI
    pub gatt_incoming_callbacks: Rc<gatt::callbacks::CallbackTransactionManager>,
    /// Proxies calls into GATT server
    pub gatt_module: &'a mut gatt::server::GattModule,
}

static GLOBAL_MODULE_RUNNER: Mutex<RustModuleRunner> = Mutex::new(RustModuleRunner::new());

impl RustModuleRunner {
    const fn new() -> Self {
        Self::NotStarted
    }

    /// Handles bringup of all Rust modules. This occurs after GD C++ modules
    /// have started, but before the legacy stack has initialized.
    /// Must be invoked from the Rust thread after JNI initializes it and passes
    /// in JNI modules.
    ///
    /// This function can only be run once, if it is run more than once it will panic.
    pub fn run(
        gatt_callbacks: Rc<dyn GattCallbacks>,
        att_transport: Rc<dyn AttTransport>,
        on_started: impl FnOnce(),
    ) {
        info!("starting Rust modules");
        let mut main_thread_rx = match GLOBAL_MODULE_RUNNER.lock().unwrap().start() {
            Ok(main_thread_rx) => main_thread_rx,
            Err(reason) => {
                error!("Cannot start rust modules: {reason}");
                panic!("Bluetooth Rust modules: {reason}");
            }
        };
        let rt = Builder::new_current_thread()
            .enable_all()
            .build()
            .expect("failed to start tokio runtime");
        let local = LocalSet::new();

        // Setup FFI and C++ modules
        let arbiter = gatt::arbiter::initialize_arbiter();

        // Now enter the runtime
        let result = local.block_on(&rt, async move {
            // Then follow the pure-Rust modules
            let gatt_incoming_callbacks =
                Rc::new(gatt::callbacks::CallbackTransactionManager::new(gatt_callbacks.clone()));
            let gatt_module = &mut gatt::server::GattModule::new(att_transport.clone(), arbiter);

            // All modules that are visible from incoming JNI / top-level interfaces should
            // be exposed here
            let mut modules = ModuleViews {
                gatt_outgoing_callbacks: gatt_callbacks,
                gatt_incoming_callbacks,
                gatt_module,
            };

            // notify upper layer that we are ready to receive messages
            on_started();

            // This is the core event loop that serializes incoming requests into the Rust
            // thread do_in_rust_thread lets us post into here from foreign
            // threads
            info!("starting event loop");
            while let Some(f) = main_thread_rx.recv().await {
                f(&mut modules);
            }
            Ok(())
        });
        warn!("RustModuleRunner has stopped, shutting down executor thread");

        if let Err(e) = GLOBAL_MODULE_RUNNER.lock().unwrap().finished(result) {
            warn!("failed to record runner finish: {e:?}");
        }

        gatt::arbiter::clean_arbiter();
    }

    /// Externally stop the global runner.
    pub fn stop() {
        GLOBAL_MODULE_RUNNER.lock().unwrap().shutdown();
    }

    #[allow(dead_code)]
    fn send(&self, f: BoxedMainThreadCallback) -> Result<(), (String, BoxedMainThreadCallback)> {
        match self {
            Self::NotStarted => Err(("Not started yet".to_string(), f)),
            Self::Ended { .. } | Self::Stopping => Err(("Runner ended".to_string(), f)),
            Self::Running { tx } => tx.send(f).map_err(|e| ("Failed to send".to_string(), e.0)),
        }
    }

    fn start(&mut self) -> Result<mpsc::UnboundedReceiver<BoxedMainThreadCallback>, String> {
        match self {
            Self::Running { .. } => {
                return Err("Already started".to_string());
            }
            Self::Ended { result } => {
                return Err(format!("Already finished: {result:?}"));
            }
            Self::Stopping => {
                return Err("Can't start, finishing".to_string());
            }
            Self::NotStarted => {}
        };

        let (tx, rx) = mpsc::unbounded_channel();

        *self = Self::Running { tx };
        Ok(rx)
    }

    fn shutdown(&mut self) {
        match std::mem::replace(self, Self::Stopping) {
            Self::NotStarted => {
                warn!("Runner being stopped when it hasn't been started");
                self.finished(Err("Never started".to_string())).unwrap();
            }
            Self::Stopping => {
                warn!("Asked to shutdown twice before stopped");
            }
            Self::Ended { .. } | Self::Running { .. } => {}
        }
    }

    fn finished(&mut self, result: Result<(), String>) -> Result<(), String> {
        match self {
            Self::NotStarted => return Err("Not started".to_string()),
            Self::Ended { result } => return Err(format!("Already finished with {result:?}")),
            Self::Running { .. } | Self::Stopping => {}
        }

        *self = Self::Ended { result };
        Ok(())
    }
}

type BoxedMainThreadCallback = Box<dyn for<'a> FnOnce(&'a mut ModuleViews) + Send + 'static>;

/// Posts a callback to the Rust thread and gives it access to public Rust
/// modules, used from JNI.
///
/// Do not call this from Rust modules / the Rust thread! Instead, Rust modules
/// should receive references to their dependent modules at startup. If passing
/// callbacks into C++, don't use this method either - instead, acquire a clone
/// of MAIN_THREAD_TX when the callback is created. This ensures that there
/// never are "invalid" callbacks that may still work depending on when the
/// GLOBAL_MODULE_REGISTRY is initialized.
pub fn do_in_rust_thread<F>(f: F)
where
    F: for<'a> FnOnce(&'a mut ModuleViews) + Send + 'static,
{
    if let Err((s, _f)) = GLOBAL_MODULE_RUNNER.lock().expect("lock not poisoned").send(Box::new(f))
    {
        error!("Failed to do_in_rust_thread, panicking: {s}");
        panic!("Rust call failed");
    }
}
