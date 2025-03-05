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

use bluetooth_offload_hci as hci;

use crate::arbiter::Arbiter;
use crate::service::{Service, StreamConfiguration};
use hci::{Command, Event, EventToBytes, IsoData, Module, ModuleBuilder, ReturnParameters, Status};
use std::collections::HashMap;
use std::sync::{Arc, Mutex};

const DATA_PATH_ID_SOFTWARE: u8 = 0x19; // TODO

/// LE Audio HCI-Proxy module builder
pub struct LeAudioModuleBuilder {}

pub(crate) struct LeAudioModule {
    next_module: Arc<dyn Module>,
    state: Mutex<State>,
}

#[derive(Default)]
struct State {
    big: HashMap<u8, BigParameters>,
    cig: HashMap<u8, CigParameters>,
    stream: HashMap<u16, Stream>,
    arbiter: Option<Arc<Arbiter>>,
}

struct BigParameters {
    bis_handles: Vec<u16>,
    sdu_interval: u32,
}

struct CigParameters {
    cis_handles: Vec<u16>,
    sdu_interval_c_to_p: u32,
    sdu_interval_p_to_c: u32,
}

#[derive(Debug, Clone)]
struct Stream {
    state: StreamState,
    iso_type: IsoType,
    iso_interval_us: u32,
}

#[derive(Debug, PartialEq, Clone)]
enum StreamState {
    Disabled,
    Enabling,
    Enabled,
}

#[derive(Debug, Clone)]
enum IsoType {
    Cis { c_to_p: IsoInDirection, _p_to_c: IsoInDirection },
    Bis { c_to_p: IsoInDirection },
}

#[derive(Debug, Clone)]
struct IsoInDirection {
    sdu_interval_us: u32,
    max_sdu_size: u16,
    burst_number: u8,
    flush_timeout: u8,
}

impl Stream {
    fn new_cis(cig: &CigParameters, e: &hci::LeCisEstablished) -> Self {
        let iso_interval_us = (e.iso_interval as u32) * 1250;

        if cig.sdu_interval_c_to_p != 0 {
            assert_eq!(iso_interval_us % cig.sdu_interval_c_to_p, 0, "Framing mode not supported");
            assert_eq!(
                iso_interval_us / cig.sdu_interval_c_to_p,
                e.bn_c_to_p.into(),
                "SDU fragmentation not supported"
            );
        }
        if cig.sdu_interval_p_to_c != 0 {
            assert_eq!(iso_interval_us % cig.sdu_interval_p_to_c, 0, "Framing mode not supported");
            assert_eq!(
                iso_interval_us / cig.sdu_interval_p_to_c,
                e.bn_p_to_c.into(),
                "SDU fragmentation not supported"
            );
        }

        Self {
            state: StreamState::Disabled,
            iso_interval_us,
            iso_type: IsoType::Cis {
                c_to_p: IsoInDirection {
                    sdu_interval_us: cig.sdu_interval_c_to_p,
                    max_sdu_size: e.max_pdu_c_to_p,
                    burst_number: e.bn_c_to_p,
                    flush_timeout: e.ft_c_to_p,
                },
                _p_to_c: IsoInDirection {
                    sdu_interval_us: cig.sdu_interval_p_to_c,
                    max_sdu_size: e.max_pdu_p_to_c,
                    burst_number: e.bn_p_to_c,
                    flush_timeout: e.ft_p_to_c,
                },
            },
        }
    }

    fn new_bis(big: &BigParameters, e: &hci::LeCreateBigComplete) -> Self {
        let iso_interval_us = (e.iso_interval as u32) * 1250;

        assert_eq!(iso_interval_us % big.sdu_interval, 0, "Framing mode not supported");
        assert_eq!(
            iso_interval_us / big.sdu_interval,
            e.bn.into(),
            "SDU fragmentation not supported"
        );

        Self {
            state: StreamState::Disabled,
            iso_interval_us,
            iso_type: IsoType::Bis {
                c_to_p: IsoInDirection {
                    sdu_interval_us: big.sdu_interval,
                    max_sdu_size: e.max_pdu,
                    burst_number: e.bn,
                    flush_timeout: e.irc,
                },
            },
        }
    }
}

impl ModuleBuilder for LeAudioModuleBuilder {
    /// Build the HCI-Proxy module from the next module in the chain
    fn build(&self, next_module: Arc<dyn Module>) -> Arc<dyn Module> {
        Service::register();
        Arc::new(LeAudioModule::new(next_module))
    }
}

impl LeAudioModule {
    pub(crate) fn new(next_module: Arc<dyn Module>) -> Self {
        Self { next_module, state: Mutex::new(Default::default()) }
    }

    #[cfg(test)]
    pub(crate) fn arbiter(&self) -> Option<Arc<Arbiter>> {
        let state = self.state.lock().unwrap();
        state.arbiter.clone()
    }
}

impl Module for LeAudioModule {
    fn next(&self) -> &dyn Module {
        &*self.next_module
    }

    fn out_cmd(&self, data: &[u8]) {
        match Command::from_bytes(data) {
            Ok(Command::LeSetCigParameters(ref c)) => {
                let mut state = self.state.lock().unwrap();
                state.cig.insert(
                    c.cig_id,
                    CigParameters {
                        cis_handles: vec![],
                        sdu_interval_c_to_p: c.sdu_interval_c_to_p,
                        sdu_interval_p_to_c: c.sdu_interval_p_to_c,
                    },
                );
            }

            Ok(Command::LeCreateBig(ref c)) => {
                let mut state = self.state.lock().unwrap();
                state.big.insert(
                    c.big_handle,
                    BigParameters { bis_handles: vec![], sdu_interval: c.sdu_interval },
                );
            }

            Ok(Command::LeSetupIsoDataPath(ref c)) if c.data_path_id == DATA_PATH_ID_SOFTWARE => {
                assert_eq!(c.data_path_direction, hci::LeDataPathDirection::Input);
                let mut state = self.state.lock().unwrap();
                let stream = state.stream.get_mut(&c.connection_handle).unwrap();
                stream.state = StreamState::Enabling;
            }

            _ => (),
        }

        self.next().out_cmd(data);
    }

    fn in_evt(&self, data: &[u8]) {
        match Event::from_bytes(data) {
            Ok(Event::CommandComplete(ref e)) => match e.return_parameters {
                ReturnParameters::Reset(ref ret) if ret.status == Status::Success => {
                    let mut state = self.state.lock().unwrap();
                    *state = Default::default();
                }

                ReturnParameters::LeReadBufferSizeV2(ref ret) if ret.status == Status::Success => {
                    let mut state = self.state.lock().unwrap();
                    state.arbiter = Some(Arc::new(Arbiter::new(
                        self.next_module.clone(),
                        ret.iso_data_packet_length.into(),
                        ret.total_num_iso_data_packets.into(),
                    )));
                    Service::reset(Arc::downgrade(state.arbiter.as_ref().unwrap()));
                }

                ReturnParameters::LeSetCigParameters(ref ret) if ret.status == Status::Success => {
                    let mut state = self.state.lock().unwrap();
                    let cig = state.cig.get_mut(&ret.cig_id).unwrap();
                    cig.cis_handles = ret.connection_handles.clone();
                }

                ReturnParameters::LeRemoveCig(ref ret) if ret.status == Status::Success => {
                    let mut state = self.state.lock().unwrap();
                    state.cig.remove(&ret.cig_id);
                }

                ReturnParameters::LeSetupIsoDataPath(ref ret) => 'event: {
                    let mut state = self.state.lock().unwrap();
                    let stream = state.stream.get_mut(&ret.connection_handle).unwrap();
                    stream.state =
                        if stream.state == StreamState::Enabling && ret.status == Status::Success {
                            StreamState::Enabled
                        } else {
                            StreamState::Disabled
                        };

                    if stream.state != StreamState::Enabled {
                        break 'event;
                    }

                    let c_to_p = match stream.iso_type {
                        IsoType::Cis { ref c_to_p, .. } => c_to_p,
                        IsoType::Bis { ref c_to_p } => c_to_p,
                    };

                    Service::start_stream(
                        ret.connection_handle,
                        StreamConfiguration {
                            isoIntervalUs: stream.iso_interval_us as i32,
                            sduIntervalUs: c_to_p.sdu_interval_us as i32,
                            maxSduSize: c_to_p.max_sdu_size as i32,
                            burstNumber: c_to_p.burst_number as i32,
                            flushTimeout: c_to_p.flush_timeout as i32,
                        },
                    );
                }

                ReturnParameters::LeRemoveIsoDataPath(ref ret) if ret.status == Status::Success => {
                    let mut state = self.state.lock().unwrap();
                    let stream = state.stream.get_mut(&ret.connection_handle).unwrap();
                    if stream.state == StreamState::Enabled {
                        Service::stop_stream(ret.connection_handle);
                    }
                    stream.state = StreamState::Disabled;
                }

                _ => (),
            },

            Ok(Event::LeCisEstablished(ref e)) if e.status == Status::Success => {
                let mut state = self.state.lock().unwrap();
                let mut cig_values = state.cig.values();
                let Some(cig) =
                    cig_values.find(|&g| g.cis_handles.iter().any(|&h| h == e.connection_handle))
                else {
                    panic!("CIG not set-up for CIS 0x{:03x}", e.connection_handle);
                };

                let cis = Stream::new_cis(cig, e);
                if state.stream.insert(e.connection_handle, cis).is_some() {
                    log::error!("CIS already established");
                } else {
                    let arbiter = state.arbiter.as_ref().unwrap();
                    arbiter.add_connection(e.connection_handle);
                }
            }

            Ok(Event::DisconnectionComplete(ref e)) if e.status == Status::Success => {
                let mut state = self.state.lock().unwrap();
                if state.stream.remove(&e.connection_handle).is_some() {
                    let arbiter = state.arbiter.as_ref().unwrap();
                    arbiter.remove_connection(e.connection_handle);
                }
            }

            Ok(Event::LeCreateBigComplete(ref e)) if e.status == Status::Success => {
                let mut state_guard = self.state.lock().unwrap();
                let state = &mut *state_guard;

                let big = state.big.get_mut(&e.big_handle).unwrap();
                big.bis_handles = e.bis_handles.clone();

                let bis = Stream::new_bis(big, e);
                for h in &big.bis_handles {
                    if state.stream.insert(*h, bis.clone()).is_some() {
                        log::error!("BIS already established");
                    } else {
                        let arbiter = state.arbiter.as_ref().unwrap();
                        arbiter.add_connection(*h);
                    }
                }
            }

            Ok(Event::LeTerminateBigComplete(ref e)) => {
                let mut state = self.state.lock().unwrap();
                let big = state.big.remove(&e.big_handle).unwrap();
                for h in big.bis_handles {
                    state.stream.remove(&h);

                    let arbiter = state.arbiter.as_ref().unwrap();
                    arbiter.remove_connection(h);
                }
            }

            Ok(Event::NumberOfCompletedPackets(ref e)) => 'event: {
                let state = self.state.lock().unwrap();
                let Some(arbiter) = state.arbiter.as_ref() else {
                    break 'event;
                };

                let (stack_event, _) = {
                    let mut stack_event = hci::NumberOfCompletedPackets {
                        handles: Vec::with_capacity(e.handles.len()),
                    };
                    let mut audio_event = hci::NumberOfCompletedPackets {
                        handles: Vec::with_capacity(e.handles.len()),
                    };
                    for item in &e.handles {
                        let handle = item.connection_handle;
                        arbiter.set_completed(handle, item.num_completed_packets.into());

                        if match state.stream.get(&handle) {
                            Some(stream) => stream.state == StreamState::Enabled,
                            None => false,
                        } {
                            audio_event.handles.push(*item);
                        } else {
                            stack_event.handles.push(*item);
                        }
                    }
                    (stack_event, audio_event)
                };

                if !stack_event.handles.is_empty() {
                    self.next().in_evt(&stack_event.to_bytes());
                }
                return;
            }

            Ok(..) => (),

            Err(code) => {
                log::error!("Malformed event with code: {:?}", code);
            }
        }

        self.next().in_evt(data);
    }

    fn out_iso(&self, data: &[u8]) {
        let state = self.state.lock().unwrap();
        let arbiter = state.arbiter.as_ref().unwrap();
        arbiter.push_incoming(&IsoData::from_bytes(data).unwrap());
    }
}
