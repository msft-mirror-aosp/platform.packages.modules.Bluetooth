#include <server_configurable_flags/get_flags.h>

#include <vector>

#include "com_android_bluetooth_flags.h"

namespace com::android::bluetooth::flags {

class flag_provider : public flag_provider_interface {
public:
  virtual bool a2dp_aidl_encoding_interval() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.a2dp_aidl_encoding_interval", "false") == "true";
  }

  virtual bool a2dp_async_allow_low_latency() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.a2dp_async_allow_low_latency", "false") == "true";
  }

  virtual bool a2dp_check_lea_iso_channel() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.a2dp_check_lea_iso_channel", "false") == "true";
  }

  virtual bool a2dp_concurrent_source_sink() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.a2dp_concurrent_source_sink", "false") == "true";
  }

  virtual bool a2dp_ignore_started_when_responder() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.a2dp_ignore_started_when_responder",
                   "false") == "true";
  }

  virtual bool a2dp_offload_codec_extensibility() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.a2dp_offload_codec_extensibility",
                   "true") == "true";
  }

  virtual bool a2dp_service_looper() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth", "com.android.bluetooth.flags.a2dp_service_looper",
                   "false") == "true";
  }

  virtual bool abs_volume_sdp_conflict() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth", "com.android.bluetooth.flags.abs_volume_sdp_conflict",
                   "false") == "true";
  }

  virtual bool airplane_mode_x_ble_on() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth", "com.android.bluetooth.flags.airplane_mode_x_ble_on",
                   "false") == "true";
  }

  virtual bool allow_switching_hid_and_hogp() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.allow_switching_hid_and_hogp", "false") == "true";
  }

  virtual bool always_fallback_to_available_device() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.always_fallback_to_available_device",
                   "false") == "true";
  }

  virtual bool android_headtracker_service() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.android_headtracker_service", "false") == "true";
  }

  virtual bool api_get_connection_state_using_identity_address() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.api_get_connection_state_using_identity_address",
                   "false") == "true";
  }

  virtual bool asha_asrc() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth", "com.android.bluetooth.flags.asha_asrc", "false") ==
           "true";
  }

  virtual bool asha_encrypted_l2c_coc() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth", "com.android.bluetooth.flags.asha_encrypted_l2c_coc",
                   "false") == "true";
  }

  virtual bool asymmetric_phy_for_unidirectional_cis() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.asymmetric_phy_for_unidirectional_cis",
                   "false") == "true";
  }

  virtual bool audio_port_binder_inherit_rt() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.audio_port_binder_inherit_rt", "false") == "true";
  }

  virtual bool audio_routing_centralization() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.audio_routing_centralization", "false") == "true";
  }

  virtual bool auto_connect_on_multiple_hfp_when_no_a2dp_device() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.auto_connect_on_multiple_hfp_when_no_a2dp_device",
                   "false") == "true";
  }

  virtual bool auto_on_feature() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth", "com.android.bluetooth.flags.auto_on_feature",
                   "true") == "true";
  }

  virtual bool av_stream_reconfigure_fix() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.av_stream_reconfigure_fix", "false") == "true";
  }

  virtual bool avdt_discover_seps_as_acceptor() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.avdt_discover_seps_as_acceptor", "false") == "true";
  }

  virtual bool avdtp_error_codes() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth", "com.android.bluetooth.flags.avdtp_error_codes",
                   "false") == "true";
  }

  virtual bool avoid_static_loading_of_native() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.avoid_static_loading_of_native", "false") == "true";
  }

  virtual bool avrcp_connect_a2dp_delayed() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.avrcp_connect_a2dp_delayed", "false") == "true";
  }

  virtual bool avrcp_sdp_records() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth", "com.android.bluetooth.flags.avrcp_sdp_records",
                   "false") == "true";
  }

  virtual bool ble_check_data_length_on_legacy_advertising() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.ble_check_data_length_on_legacy_advertising",
                   "false") == "true";
  }

  virtual bool ble_context_map_remove_fix() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.ble_context_map_remove_fix", "false") == "true";
  }

  virtual bool ble_gatt_server_use_address_type_in_connection() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.ble_gatt_server_use_address_type_in_connection",
                   "false") == "true";
  }

  virtual bool ble_scan_adv_metrics_redesign() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.ble_scan_adv_metrics_redesign", "false") == "true";
  }

  virtual bool bluetooth_power_telemetry() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.bluetooth_power_telemetry", "false") == "true";
  }

  virtual bool bond_transport_after_bond_cancel_fix() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.bond_transport_after_bond_cancel_fix",
                   "false") == "true";
  }

  virtual bool break_uhid_polling_early() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.break_uhid_polling_early", "false") == "true";
  }

  virtual bool browsing_refactor() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth", "com.android.bluetooth.flags.browsing_refactor",
                   "false") == "true";
  }

  virtual bool bt_socket_api_l2cap_cid() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth", "com.android.bluetooth.flags.bt_socket_api_l2cap_cid",
                   "false") == "true";
  }

  virtual bool bt_system_context_report() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.bt_system_context_report", "true") == "true";
  }

  virtual bool bta_ag_cmd_brsf_allow_uint32() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.bta_ag_cmd_brsf_allow_uint32", "false") == "true";
  }

  virtual bool bta_av_use_peer_codec() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth", "com.android.bluetooth.flags.bta_av_use_peer_codec",
                   "false") == "true";
  }

  virtual bool bta_av_setconfig_rej_type_confusion() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.bta_av_setconfig_rej_type_confusion",
                   "false") == "true";
  }

  virtual bool bta_dm_defer_device_discovery_state_change_until_rnr_complete() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.bta_dm_defer_device_discovery_state_change_until_"
                   "rnr_complete",
                   "false") == "true";
  }

  virtual bool bta_dm_discover_both() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth", "com.android.bluetooth.flags.bta_dm_discover_both",
                   "false") == "true";
  }

  virtual bool cancel_pairing_only_on_disconnected_transport() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.cancel_pairing_only_on_disconnected_transport",
                   "false") == "true";
  }

  virtual bool channel_sounding() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth", "com.android.bluetooth.flags.channel_sounding",
                   "true") == "true";
  }

  virtual bool channel_sounding_in_stack() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.channel_sounding_in_stack", "false") == "true";
  }

  virtual bool cleanup_le_only_device_type() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.cleanup_le_only_device_type", "false") == "true";
  }

  virtual bool clear_collision_state_on_pairing_complete() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.clear_collision_state_on_pairing_complete",
                   "false") == "true";
  }

  virtual bool continue_service_discovery_when_cancel_device_discovery() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.continue_service_discovery_when_cancel_device_"
                   "discovery",
                   "false") == "true";
  }

  virtual bool device_iot_config_logging() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.device_iot_config_logging", "false") == "true";
  }

  virtual bool do_not_replace_existing_cod_with_uncategorized_cod() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.do_not_replace_existing_cod_with_uncategorized_cod",
                   "false") == "true";
  }

  virtual bool dumpsys_acquire_stack_when_executing() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.dumpsys_acquire_stack_when_executing",
                   "false") == "true";
  }

  virtual bool enable_hap_by_default() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth", "com.android.bluetooth.flags.enable_hap_by_default",
                   "false") == "true";
  }

  virtual bool enable_sniff_offload() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth", "com.android.bluetooth.flags.enable_sniff_offload",
                   "false") == "true";
  }

  virtual bool encrypted_advertising_data() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.encrypted_advertising_data", "false") == "true";
  }

  virtual bool ensure_valid_adv_flag() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth", "com.android.bluetooth.flags.ensure_valid_adv_flag",
                   "false") == "true";
  }

  virtual bool enumerate_gatt_errors() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth", "com.android.bluetooth.flags.enumerate_gatt_errors",
                   "true") == "true";
  }

  virtual bool explicit_kill_from_system_server() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.explicit_kill_from_system_server",
                   "false") == "true";
  }

  virtual bool fallback_when_wired_audio_disconnected() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.fallback_when_wired_audio_disconnected",
                   "false") == "true";
  }

  virtual bool fast_bind_to_app() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth", "com.android.bluetooth.flags.fast_bind_to_app",
                   "false") == "true";
  }

  virtual bool fix_hfp_qual_1_9() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth", "com.android.bluetooth.flags.fix_hfp_qual_1_9",
                   "false") == "true";
  }

  virtual bool fix_le_pairing_passkey_entry_bypass() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.fix_le_pairing_passkey_entry_bypass",
                   "true") == "true";
  }

  virtual bool fix_nonconnectable_scannable_advertisement() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.fix_nonconnectable_scannable_advertisement",
                   "false") == "true";
  }

  virtual bool floss_separate_host_privacy_and_llprivacy() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.floss_separate_host_privacy_and_llprivacy",
                   "false") == "true";
  }

  virtual bool gatt_cleanup_restricted_handles() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.gatt_cleanup_restricted_handles", "true") == "true";
  }

  virtual bool gatt_client_dynamic_allocation() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.gatt_client_dynamic_allocation", "false") == "true";
  }

  virtual bool gatt_drop_acl_on_out_of_resources_fix() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.gatt_drop_acl_on_out_of_resources_fix",
                   "false") == "true";
  }

  virtual bool gatt_fix_device_busy() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth", "com.android.bluetooth.flags.gatt_fix_device_busy",
                   "true") == "true";
  }

  virtual bool gatt_fix_multiple_direct_connect() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.gatt_fix_multiple_direct_connect",
                   "false") == "true";
  }

  virtual bool gatt_reconnect_on_bt_on_fix() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.gatt_reconnect_on_bt_on_fix", "false") == "true";
  }

  virtual bool gatt_rediscover_on_canceled() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.gatt_rediscover_on_canceled", "false") == "true";
  }

  virtual bool gdx_device_discovery() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth", "com.android.bluetooth.flags.gdx_device_discovery",
                   "false") == "true";
  }

  virtual bool gdx_service_discovery() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth", "com.android.bluetooth.flags.gdx_service_discovery",
                   "false") == "true";
  }

  virtual bool get_address_type_api() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth", "com.android.bluetooth.flags.get_address_type_api",
                   "true") == "true";
  }

  virtual bool guest_mode_bond() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth", "com.android.bluetooth.flags.guest_mode_bond",
                   "false") == "true";
  }

  virtual bool handle_delivery_sending_failure_events() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.handle_delivery_sending_failure_events",
                   "false") == "true";
  }

  virtual bool headset_client_am_hf_volume_symmetric() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.headset_client_am_hf_volume_symmetric",
                   "false") == "true";
  }

  virtual bool headtracker_codec_capability() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.headtracker_codec_capability", "false") == "true";
  }

  virtual bool headtracker_sdu_size() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth", "com.android.bluetooth.flags.headtracker_sdu_size",
                   "false") == "true";
  }

  virtual bool hfp_codec_aptx_voice() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth", "com.android.bluetooth.flags.hfp_codec_aptx_voice",
                   "true") == "true";
  }

  virtual bool hid_report_queuing() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth", "com.android.bluetooth.flags.hid_report_queuing",
                   "false") == "true";
  }

  virtual bool higher_l2cap_flush_threshold() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.higher_l2cap_flush_threshold", "false") == "true";
  }

  virtual bool identity_address_null_if_unknown() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.identity_address_null_if_unknown",
                   "false") == "true";
  }

  virtual bool ignore_notify_when_already_connected() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.ignore_notify_when_already_connected",
                   "false") == "true";
  }

  virtual bool is_sco_managed_by_audio() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth", "com.android.bluetooth.flags.is_sco_managed_by_audio",
                   "false") == "true";
  }

  virtual bool keep_hfp_active_during_leaudio_handover() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.keep_hfp_active_during_leaudio_handover",
                   "false") == "true";
  }

  virtual bool keep_stopped_media_browser_service() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.keep_stopped_media_browser_service",
                   "true") == "true";
  }

  virtual bool key_missing_as_ordered_broadcast() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.key_missing_as_ordered_broadcast",
                   "false") == "true";
  }

  virtual bool key_missing_broadcast() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth", "com.android.bluetooth.flags.key_missing_broadcast",
                   "true") == "true";
  }

  virtual bool key_missing_classic_device() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.key_missing_classic_device", "false") == "true";
  }

  virtual bool kill_instead_of_exit() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth", "com.android.bluetooth.flags.kill_instead_of_exit",
                   "false") == "true";
  }

  virtual bool l2cap_le_do_not_adjust_min_interval() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.l2cap_le_do_not_adjust_min_interval",
                   "false") == "true";
  }

  virtual bool l2cap_p_ccb_check_rewrite() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.l2cap_p_ccb_check_rewrite", "false") == "true";
  }

  virtual bool l2cap_tx_complete_cb_info() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.l2cap_tx_complete_cb_info", "false") == "true";
  }

  virtual bool l2cap_update_existing_conn_interval_with_base_interval() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.l2cap_update_existing_conn_interval_with_base_"
                   "interval",
                   "false") == "true";
  }

  virtual bool le_ase_read_multiple_variable() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.le_ase_read_multiple_variable", "false") == "true";
  }

  virtual bool le_audio_base_ecosystem_interval() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.le_audio_base_ecosystem_interval",
                   "false") == "true";
  }

  virtual bool le_audio_support_unidirectional_voice_assistant() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.le_audio_support_unidirectional_voice_assistant",
                   "false") == "true";
  }

  virtual bool le_periodic_scanning_reassembler() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.le_periodic_scanning_reassembler",
                   "true") == "true";
  }

  virtual bool le_scan_fix_remote_exception() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.le_scan_fix_remote_exception", "true") == "true";
  }

  virtual bool le_scan_use_address_type() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.le_scan_use_address_type", "false") == "true";
  }

  virtual bool le_scan_use_uid_for_importance() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.le_scan_use_uid_for_importance", "false") == "true";
  }

  virtual bool leaudio_add_sampling_frequencies() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_add_sampling_frequencies",
                   "true") == "true";
  }

  virtual bool leaudio_allow_leaudio_only_devices() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_allow_leaudio_only_devices",
                   "false") == "true";
  }

  virtual bool leaudio_allowed_context_mask() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_allowed_context_mask", "false") == "true";
  }

  virtual bool leaudio_big_depends_on_audio_state() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_big_depends_on_audio_state",
                   "false") == "true";
  }

  virtual bool leaudio_broadcast_assistant_handle_command_statuses() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_broadcast_assistant_handle_command_"
                   "statuses",
                   "false") == "true";
  }

  virtual bool leaudio_broadcast_assistant_peripheral_entrustment() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_broadcast_assistant_peripheral_entrustment",
                   "false") == "true";
  }

  virtual bool leaudio_broadcast_audio_handover_policies() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_broadcast_audio_handover_policies",
                   "true") == "true";
  }

  virtual bool leaudio_broadcast_destroy_after_timeout() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_broadcast_destroy_after_timeout",
                   "false") == "true";
  }

  virtual bool leaudio_broadcast_extract_periodic_scanner_from_state_machine() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_broadcast_extract_periodic_scanner_from_"
                   "state_machine",
                   "false") == "true";
  }

  virtual bool leaudio_broadcast_feature_support() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_broadcast_feature_support",
                   "true") == "true";
  }

  virtual bool leaudio_broadcast_monitor_source_sync_status() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_broadcast_monitor_source_sync_status",
                   "true") == "true";
  }

  virtual bool leaudio_broadcast_update_metadata_callback() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_broadcast_update_metadata_callback",
                   "false") == "true";
  }

  virtual bool leaudio_broadcast_volume_control_for_connected_devices() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_broadcast_volume_control_for_connected_"
                   "devices",
                   "true") == "true";
  }

  virtual bool leaudio_broadcast_volume_control_with_set_volume() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_broadcast_volume_control_with_set_volume",
                   "false") == "true";
  }

  virtual bool leaudio_call_start_scan_directly() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_call_start_scan_directly",
                   "false") == "true";
  }

  virtual bool leaudio_callback_on_group_stream_status() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_callback_on_group_stream_status",
                   "true") == "true";
  }

  virtual bool leaudio_codec_config_callback_order_fix() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_codec_config_callback_order_fix",
                   "false") == "true";
  }

  virtual bool leaudio_dynamic_spatial_audio() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_dynamic_spatial_audio", "false") == "true";
  }

  virtual bool leaudio_getting_active_state_support() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_getting_active_state_support",
                   "false") == "true";
  }

  virtual bool leaudio_hal_client_asrc() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth", "com.android.bluetooth.flags.leaudio_hal_client_asrc",
                   "true") == "true";
  }

  virtual bool leaudio_mono_location_errata() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_mono_location_errata", "false") == "true";
  }

  virtual bool leaudio_multicodec_aidl_support() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_multicodec_aidl_support",
                   "false") == "true";
  }

  virtual bool leaudio_multiple_vocs_instances_api() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_multiple_vocs_instances_api",
                   "true") == "true";
  }

  virtual bool leaudio_no_context_validate_streaming_request() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_no_context_validate_streaming_request",
                   "false") == "true";
  }

  virtual bool leaudio_quick_leaudio_toggle_switch_fix() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_quick_leaudio_toggle_switch_fix",
                   "false") == "true";
  }

  virtual bool leaudio_resume_active_after_hfp_handover() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_resume_active_after_hfp_handover",
                   "false") == "true";
  }

  virtual bool leaudio_speed_up_reconfiguration_between_call() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_speed_up_reconfiguration_between_call",
                   "false") == "true";
  }

  virtual bool leaudio_start_request_state_mutex_check() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_start_request_state_mutex_check",
                   "false") == "true";
  }

  virtual bool leaudio_start_stream_race_fix() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_start_stream_race_fix", "true") == "true";
  }

  virtual bool leaudio_synchronize_start() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_synchronize_start", "false") == "true";
  }

  virtual bool leaudio_use_audio_mode_listener() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.leaudio_use_audio_mode_listener",
                   "false") == "true";
  }

  virtual bool load_did_config_from_sysprops() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.load_did_config_from_sysprops", "false") == "true";
  }

  virtual bool maintain_call_index_after_conference() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.maintain_call_index_after_conference",
                   "false") == "true";
  }

  virtual bool map_limit_notification() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth", "com.android.bluetooth.flags.map_limit_notification",
                   "false") == "true";
  }

  virtual bool mcp_allow_play_without_active_player() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.mcp_allow_play_without_active_player",
                   "false") == "true";
  }

  virtual bool metadata_api_inactive_audio_device_upon_connection() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.metadata_api_inactive_audio_device_upon_connection",
                   "true") == "true";
  }

  virtual bool msft_addr_tracking_quirk() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.msft_addr_tracking_quirk", "false") == "true";
  }

  virtual bool nrpa_non_connectable_adv() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.nrpa_non_connectable_adv", "false") == "true";
  }

  virtual bool opp_fix_multiple_notifications_issues() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.opp_fix_multiple_notifications_issues",
                   "true") == "true";
  }

  virtual bool opp_ignore_content_observer_after_service_stop() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.opp_ignore_content_observer_after_service_stop",
                   "false") == "true";
  }

  virtual bool opp_start_activity_directly_from_notification() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.opp_start_activity_directly_from_notification",
                   "true") == "true";
  }

  virtual bool override_context_to_specify_device_id() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.override_context_to_specify_device_id",
                   "false") == "true";
  }

  virtual bool pairing_name_discovery_addresss_mismatch() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.pairing_name_discovery_addresss_mismatch",
                   "false") == "true";
  }

  virtual bool pairing_on_unknown_transport() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.pairing_on_unknown_transport", "false") == "true";
  }

  virtual bool pan_use_identity_address() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.pan_use_identity_address", "false") == "true";
  }

  virtual bool phy_to_native() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth", "com.android.bluetooth.flags.phy_to_native",
                   "false") == "true";
  }

  virtual bool pretend_network_service() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth", "com.android.bluetooth.flags.pretend_network_service",
                   "true") == "true";
  }

  virtual bool prevent_hogp_reconnect_when_connected() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.prevent_hogp_reconnect_when_connected",
                   "false") == "true";
  }

  virtual bool randomize_device_level_media_ids() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.randomize_device_level_media_ids",
                   "false") == "true";
  }

  virtual bool read_model_num_fix() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth", "com.android.bluetooth.flags.read_model_num_fix",
                   "false") == "true";
  }

  virtual bool remove_address_map_on_unbond() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.remove_address_map_on_unbond", "false") == "true";
  }

  virtual bool remove_dup_pairing_response_in_oob_pairing() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.remove_dup_pairing_response_in_oob_pairing",
                   "false") == "true";
  }

  virtual bool reset_after_collision() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth", "com.android.bluetooth.flags.reset_after_collision",
                   "false") == "true";
  }

  virtual bool reset_ag_state_on_collision() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.reset_ag_state_on_collision", "false") == "true";
  }

  virtual bool respect_ble_scan_setting() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.respect_ble_scan_setting", "false") == "true";
  }

  virtual bool retry_esco_with_zero_retransmission_effort() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.retry_esco_with_zero_retransmission_effort",
                   "false") == "true";
  }

  virtual bool refactor_saving_messages_and_metadata() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.refactor_saving_messages_and_metadata",
                   "false") == "true";
  }

  virtual bool rfcomm_always_disc_initiator_in_disc_wait_ua() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.rfcomm_always_disc_initiator_in_disc_wait_ua",
                   "false") == "true";
  }

  virtual bool rfcomm_always_use_mitm() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth", "com.android.bluetooth.flags.rfcomm_always_use_mitm",
                   "false") == "true";
  }

  virtual bool rfcomm_bypass_post_to_main() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.rfcomm_bypass_post_to_main", "false") == "true";
  }

  virtual bool rfcomm_prevent_unnecessary_collisions() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.rfcomm_prevent_unnecessary_collisions",
                   "false") == "true";
  }

  virtual bool rnr_reset_state_at_cancel() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.rnr_reset_state_at_cancel", "false") == "true";
  }

  virtual bool rnr_store_device_type() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth", "com.android.bluetooth.flags.rnr_store_device_type",
                   "false") == "true";
  }

  virtual bool rnr_validate_page_scan_repetition_mode() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.rnr_validate_page_scan_repetition_mode",
                   "false") == "true";
  }

  virtual bool run_ble_audio_ticks_in_worker_thread() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.run_ble_audio_ticks_in_worker_thread",
                   "false") == "true";
  }

  virtual bool run_clock_recovery_in_worker_thread() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.run_clock_recovery_in_worker_thread",
                   "false") == "true";
  }

  virtual bool save_initial_hid_connection_policy() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.save_initial_hid_connection_policy",
                   "false") == "true";
  }

  virtual bool scan_manager_refactor() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth", "com.android.bluetooth.flags.scan_manager_refactor",
                   "false") == "true";
  }

  virtual bool scan_record_manufacturer_data_merge() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.scan_record_manufacturer_data_merge",
                   "false") == "true";
  }

  virtual bool sec_dont_clear_keys_on_encryption_err() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.sec_dont_clear_keys_on_encryption_err",
                   "false") == "true";
  }

  virtual bool separate_service_and_device_discovery() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.separate_service_and_device_discovery",
                   "false") == "true";
  }

  virtual bool set_addressed_player() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth", "com.android.bluetooth.flags.set_addressed_player",
                   "false") == "true";
  }

  virtual bool settings_can_control_hap_preset() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.settings_can_control_hap_preset", "true") == "true";
  }

  virtual bool sink_audio_policy_handover() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.sink_audio_policy_handover", "false") == "true";
  }

  virtual bool signal_connecting_on_focus_gain() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.signal_connecting_on_focus_gain",
                   "false") == "true";
  }

  virtual bool skip_unknown_robust_caching() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.skip_unknown_robust_caching", "false") == "true";
  }

  virtual bool stack_sdp_detect_nil_property_type() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.stack_sdp_detect_nil_property_type",
                   "false") == "true";
  }

  virtual bool stack_sdp_disconnect_when_cancel_in_pending_state() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.stack_sdp_disconnect_when_cancel_in_pending_state",
                   "false") == "true";
  }

  virtual bool support_exclusive_manager() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.support_exclusive_manager", "true") == "true";
  }

  virtual bool support_metadata_device_types_apis() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.support_metadata_device_types_apis",
                   "true") == "true";
  }

  virtual bool suppress_hid_rejection_broadcast() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.suppress_hid_rejection_broadcast",
                   "false") == "true";
  }

  virtual bool system_server_messenger() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth", "com.android.bluetooth.flags.system_server_messenger",
                   "false") == "true";
  }

  virtual bool temporary_pairing_device_properties() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.temporary_pairing_device_properties",
                   "false") == "true";
  }

  virtual bool uncache_player_when_browsed_player_changes() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.uncache_player_when_browsed_player_changes",
                   "false") == "true";
  }

  virtual bool unified_connection_manager() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.unified_connection_manager", "false") == "true";
  }

  virtual bool unix_file_socket_creation_failure() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.unix_file_socket_creation_failure",
                   "true") == "true";
  }

  virtual bool update_active_device_in_band_ringtone() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.update_active_device_in_band_ringtone",
                   "false") == "true";
  }

  virtual bool update_inquiry_result_on_flag_change() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.update_inquiry_result_on_flag_change",
                   "false") == "true";
  }

  virtual bool update_sco_state_correctly_on_rfcomm_disconnect_during_codec_nego() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.update_sco_state_correctly_on_rfcomm_disconnect_"
                   "during_codec_nego",
                   "false") == "true";
  }

  virtual bool use_entire_message_handle() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.use_entire_message_handle", "false") == "true";
  }

  virtual bool use_le_shim_connection_map_guard() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.use_le_shim_connection_map_guard",
                   "false") == "true";
  }

  virtual bool use_local_oob_extended_command() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth",
                   "com.android.bluetooth.flags.use_local_oob_extended_command", "false") == "true";
  }

  virtual bool vcp_mute_unmute() override {
    return server_configurable_flags::GetServerConfigurableFlag(
                   "aconfig_flags.bluetooth", "com.android.bluetooth.flags.vcp_mute_unmute",
                   "true") == "true";
  }

private:
  std::vector<int8_t> cache_ = std::vector<int8_t>(189, -1);
};

std::unique_ptr<flag_provider_interface> provider_ = std::make_unique<flag_provider>();
}  // namespace com::android::bluetooth::flags

bool com_android_bluetooth_flags_a2dp_aidl_encoding_interval() {
  return com::android::bluetooth::flags::a2dp_aidl_encoding_interval();
}

bool com_android_bluetooth_flags_a2dp_async_allow_low_latency() {
  return com::android::bluetooth::flags::a2dp_async_allow_low_latency();
}

bool com_android_bluetooth_flags_a2dp_check_lea_iso_channel() {
  return com::android::bluetooth::flags::a2dp_check_lea_iso_channel();
}

bool com_android_bluetooth_flags_a2dp_concurrent_source_sink() {
  return com::android::bluetooth::flags::a2dp_concurrent_source_sink();
}

bool com_android_bluetooth_flags_a2dp_ignore_started_when_responder() {
  return com::android::bluetooth::flags::a2dp_ignore_started_when_responder();
}

bool com_android_bluetooth_flags_a2dp_offload_codec_extensibility() {
  return com::android::bluetooth::flags::a2dp_offload_codec_extensibility();
}

bool com_android_bluetooth_flags_a2dp_service_looper() {
  return com::android::bluetooth::flags::a2dp_service_looper();
}

bool com_android_bluetooth_flags_abs_volume_sdp_conflict() {
  return com::android::bluetooth::flags::abs_volume_sdp_conflict();
}

bool com_android_bluetooth_flags_airplane_mode_x_ble_on() {
  return com::android::bluetooth::flags::airplane_mode_x_ble_on();
}

bool com_android_bluetooth_flags_allow_switching_hid_and_hogp() {
  return com::android::bluetooth::flags::allow_switching_hid_and_hogp();
}

bool com_android_bluetooth_flags_always_fallback_to_available_device() {
  return com::android::bluetooth::flags::always_fallback_to_available_device();
}

bool com_android_bluetooth_flags_android_headtracker_service() {
  return com::android::bluetooth::flags::android_headtracker_service();
}

bool com_android_bluetooth_flags_api_get_connection_state_using_identity_address() {
  return com::android::bluetooth::flags::api_get_connection_state_using_identity_address();
}

bool com_android_bluetooth_flags_asha_asrc() { return com::android::bluetooth::flags::asha_asrc(); }

bool com_android_bluetooth_flags_asha_encrypted_l2c_coc() {
  return com::android::bluetooth::flags::asha_encrypted_l2c_coc();
}

bool com_android_bluetooth_flags_asymmetric_phy_for_unidirectional_cis() {
  return com::android::bluetooth::flags::asymmetric_phy_for_unidirectional_cis();
}

bool com_android_bluetooth_flags_audio_port_binder_inherit_rt() {
  return com::android::bluetooth::flags::audio_port_binder_inherit_rt();
}

bool com_android_bluetooth_flags_audio_routing_centralization() {
  return com::android::bluetooth::flags::audio_routing_centralization();
}

bool com_android_bluetooth_flags_auto_connect_on_multiple_hfp_when_no_a2dp_device() {
  return com::android::bluetooth::flags::auto_connect_on_multiple_hfp_when_no_a2dp_device();
}

bool com_android_bluetooth_flags_auto_on_feature() {
  return com::android::bluetooth::flags::auto_on_feature();
}

bool com_android_bluetooth_flags_avdt_discover_seps_as_acceptor() {
  return com::android::bluetooth::flags::avdt_discover_seps_as_acceptor();
}

bool com_android_bluetooth_flags_av_stream_reconfigure_fix() {
  return com::android::bluetooth::flags::av_stream_reconfigure_fix();
}

bool com_android_bluetooth_flags_avdtp_error_codes() {
  return com::android::bluetooth::flags::avdtp_error_codes();
}

bool com_android_bluetooth_flags_avoid_static_loading_of_native() {
  return com::android::bluetooth::flags::avoid_static_loading_of_native();
}

bool com_android_bluetooth_flags_avrcp_connect_a2dp_delayed() {
  return com::android::bluetooth::flags::avrcp_connect_a2dp_delayed();
}

bool com_android_bluetooth_flags_avrcp_sdp_records() {
  return com::android::bluetooth::flags::avrcp_sdp_records();
}

bool com_android_bluetooth_flags_ble_check_data_length_on_legacy_advertising() {
  return com::android::bluetooth::flags::ble_check_data_length_on_legacy_advertising();
}

bool com_android_bluetooth_flags_ble_context_map_remove_fix() {
  return com::android::bluetooth::flags::ble_context_map_remove_fix();
}

bool com_android_bluetooth_flags_ble_gatt_server_use_address_type_in_connection() {
  return com::android::bluetooth::flags::ble_gatt_server_use_address_type_in_connection();
}

bool com_android_bluetooth_flags_ble_scan_adv_metrics_redesign() {
  return com::android::bluetooth::flags::ble_scan_adv_metrics_redesign();
}

bool com_android_bluetooth_flags_bluetooth_power_telemetry() {
  return com::android::bluetooth::flags::bluetooth_power_telemetry();
}

bool com_android_bluetooth_flags_bond_transport_after_bond_cancel_fix() {
  return com::android::bluetooth::flags::bond_transport_after_bond_cancel_fix();
}

bool com_android_bluetooth_flags_break_uhid_polling_early() {
  return com::android::bluetooth::flags::break_uhid_polling_early();
}

bool com_android_bluetooth_flags_browsing_refactor() {
  return com::android::bluetooth::flags::browsing_refactor();
}

bool com_android_bluetooth_flags_bt_socket_api_l2cap_cid() {
  return com::android::bluetooth::flags::bt_socket_api_l2cap_cid();
}

bool com_android_bluetooth_flags_bt_system_context_report() {
  return com::android::bluetooth::flags::bt_system_context_report();
}

bool com_android_bluetooth_flags_bta_ag_cmd_brsf_allow_uint32() {
  return com::android::bluetooth::flags::bta_ag_cmd_brsf_allow_uint32();
}

bool com_android_bluetooth_flags_bta_av_use_peer_codec() {
  return com::android::bluetooth::flags::bta_av_use_peer_codec();
}

bool com_android_bluetooth_flags_bta_av_setconfig_rej_type_confusion() {
  return com::android::bluetooth::flags::bta_av_setconfig_rej_type_confusion();
}

bool com_android_bluetooth_flags_bta_dm_defer_device_discovery_state_change_until_rnr_complete() {
  return com::android::bluetooth::flags::
          bta_dm_defer_device_discovery_state_change_until_rnr_complete();
}

bool com_android_bluetooth_flags_bta_dm_discover_both() {
  return com::android::bluetooth::flags::bta_dm_discover_both();
}

bool com_android_bluetooth_flags_cancel_pairing_only_on_disconnected_transport() {
  return com::android::bluetooth::flags::cancel_pairing_only_on_disconnected_transport();
}

bool com_android_bluetooth_flags_channel_sounding() {
  return com::android::bluetooth::flags::channel_sounding();
}

bool com_android_bluetooth_flags_channel_sounding_in_stack() {
  return com::android::bluetooth::flags::channel_sounding_in_stack();
}

bool com_android_bluetooth_flags_cleanup_le_only_device_type() {
  return com::android::bluetooth::flags::cleanup_le_only_device_type();
}

bool com_android_bluetooth_flags_clear_collision_state_on_pairing_complete() {
  return com::android::bluetooth::flags::clear_collision_state_on_pairing_complete();
}

bool com_android_bluetooth_flags_continue_service_discovery_when_cancel_device_discovery() {
  return com::android::bluetooth::flags::continue_service_discovery_when_cancel_device_discovery();
}

bool com_android_bluetooth_flags_device_iot_config_logging() {
  return com::android::bluetooth::flags::device_iot_config_logging();
}

bool com_android_bluetooth_flags_do_not_replace_existing_cod_with_uncategorized_cod() {
  return com::android::bluetooth::flags::do_not_replace_existing_cod_with_uncategorized_cod();
}

bool com_android_bluetooth_flags_dumpsys_acquire_stack_when_executing() {
  return com::android::bluetooth::flags::dumpsys_acquire_stack_when_executing();
}

bool com_android_bluetooth_flags_enable_hap_by_default() {
  return com::android::bluetooth::flags::enable_hap_by_default();
}

bool com_android_bluetooth_flags_enable_sniff_offload() {
  return com::android::bluetooth::flags::enable_sniff_offload();
}

bool com_android_bluetooth_flags_encrypted_advertising_data() {
  return com::android::bluetooth::flags::encrypted_advertising_data();
}

bool com_android_bluetooth_flags_ensure_valid_adv_flag() {
  return com::android::bluetooth::flags::ensure_valid_adv_flag();
}

bool com_android_bluetooth_flags_enumerate_gatt_errors() {
  return com::android::bluetooth::flags::enumerate_gatt_errors();
}

bool com_android_bluetooth_flags_explicit_kill_from_system_server() {
  return com::android::bluetooth::flags::explicit_kill_from_system_server();
}

bool com_android_bluetooth_flags_fallback_when_wired_audio_disconnected() {
  return com::android::bluetooth::flags::fallback_when_wired_audio_disconnected();
}

bool com_android_bluetooth_flags_fast_bind_to_app() {
  return com::android::bluetooth::flags::fast_bind_to_app();
}

bool com_android_bluetooth_flags_fix_hfp_qual_1_9() {
  return com::android::bluetooth::flags::fix_hfp_qual_1_9();
}

bool com_android_bluetooth_flags_fix_le_pairing_passkey_entry_bypass() {
  return com::android::bluetooth::flags::fix_le_pairing_passkey_entry_bypass();
}

bool com_android_bluetooth_flags_fix_nonconnectable_scannable_advertisement() {
  return com::android::bluetooth::flags::fix_nonconnectable_scannable_advertisement();
}

bool com_android_bluetooth_flags_floss_separate_host_privacy_and_llprivacy() {
  return com::android::bluetooth::flags::floss_separate_host_privacy_and_llprivacy();
}

bool com_android_bluetooth_flags_gatt_cleanup_restricted_handles() {
  return com::android::bluetooth::flags::gatt_cleanup_restricted_handles();
}

bool com_android_bluetooth_flags_gatt_client_dynamic_allocation() {
  return com::android::bluetooth::flags::gatt_client_dynamic_allocation();
}

bool com_android_bluetooth_flags_gatt_drop_acl_on_out_of_resources_fix() {
  return com::android::bluetooth::flags::gatt_drop_acl_on_out_of_resources_fix();
}

bool com_android_bluetooth_flags_gatt_fix_device_busy() {
  return com::android::bluetooth::flags::gatt_fix_device_busy();
}

bool com_android_bluetooth_flags_gatt_fix_multiple_direct_connect() {
  return com::android::bluetooth::flags::gatt_fix_multiple_direct_connect();
}

bool com_android_bluetooth_flags_gatt_reconnect_on_bt_on_fix() {
  return com::android::bluetooth::flags::gatt_reconnect_on_bt_on_fix();
}

bool com_android_bluetooth_flags_gatt_rediscover_on_canceled() {
  return com::android::bluetooth::flags::gatt_rediscover_on_canceled();
}

bool com_android_bluetooth_flags_gdx_device_discovery() {
  return com::android::bluetooth::flags::gdx_device_discovery();
}

bool com_android_bluetooth_flags_gdx_service_discovery() {
  return com::android::bluetooth::flags::gdx_service_discovery();
}

bool com_android_bluetooth_flags_get_address_type_api() {
  return com::android::bluetooth::flags::get_address_type_api();
}

bool com_android_bluetooth_flags_guest_mode_bond() {
  return com::android::bluetooth::flags::guest_mode_bond();
}

bool com_android_bluetooth_flags_handle_delivery_sending_failure_events() {
  return com::android::bluetooth::flags::handle_delivery_sending_failure_events();
}

bool com_android_bluetooth_flags_headset_client_am_hf_volume_symmetric() {
  return com::android::bluetooth::flags::headset_client_am_hf_volume_symmetric();
}

bool com_android_bluetooth_flags_headtracker_codec_capability() {
  return com::android::bluetooth::flags::headtracker_codec_capability();
}

bool com_android_bluetooth_flags_headtracker_sdu_size() {
  return com::android::bluetooth::flags::headtracker_sdu_size();
}

bool com_android_bluetooth_flags_hfp_codec_aptx_voice() {
  return com::android::bluetooth::flags::hfp_codec_aptx_voice();
}

bool com_android_bluetooth_flags_hid_report_queuing() {
  return com::android::bluetooth::flags::hid_report_queuing();
}

bool com_android_bluetooth_flags_higher_l2cap_flush_threshold() {
  return com::android::bluetooth::flags::higher_l2cap_flush_threshold();
}

bool com_android_bluetooth_flags_identity_address_null_if_unknown() {
  return com::android::bluetooth::flags::identity_address_null_if_unknown();
}

bool com_android_bluetooth_flags_ignore_notify_when_already_connected() {
  return com::android::bluetooth::flags::ignore_notify_when_already_connected();
}

bool com_android_bluetooth_flags_is_sco_managed_by_audio() {
  return com::android::bluetooth::flags::is_sco_managed_by_audio();
}

bool com_android_bluetooth_flags_keep_hfp_active_during_leaudio_handover() {
  return com::android::bluetooth::flags::keep_hfp_active_during_leaudio_handover();
}

bool com_android_bluetooth_flags_keep_stopped_media_browser_service() {
  return com::android::bluetooth::flags::keep_stopped_media_browser_service();
}

bool com_android_bluetooth_flags_key_missing_as_ordered_broadcast() {
  return com::android::bluetooth::flags::key_missing_as_ordered_broadcast();
}

bool com_android_bluetooth_flags_key_missing_broadcast() {
  return com::android::bluetooth::flags::key_missing_broadcast();
}

bool com_android_bluetooth_flags_key_missing_classic_device() {
  return com::android::bluetooth::flags::key_missing_classic_device();
}

bool com_android_bluetooth_flags_kill_instead_of_exit() {
  return com::android::bluetooth::flags::kill_instead_of_exit();
}

bool com_android_bluetooth_flags_l2cap_le_do_not_adjust_min_interval() {
  return com::android::bluetooth::flags::l2cap_le_do_not_adjust_min_interval();
}

bool com_android_bluetooth_flags_l2cap_p_ccb_check_rewrite() {
  return com::android::bluetooth::flags::l2cap_p_ccb_check_rewrite();
}

bool com_android_bluetooth_flags_l2cap_tx_complete_cb_info() {
  return com::android::bluetooth::flags::l2cap_tx_complete_cb_info();
}

bool com_android_bluetooth_flags_l2cap_update_existing_conn_interval_with_base_interval() {
  return com::android::bluetooth::flags::l2cap_update_existing_conn_interval_with_base_interval();
}

bool com_android_bluetooth_flags_le_ase_read_multiple_variable() {
  return com::android::bluetooth::flags::le_ase_read_multiple_variable();
}

bool com_android_bluetooth_flags_le_audio_base_ecosystem_interval() {
  return com::android::bluetooth::flags::le_audio_base_ecosystem_interval();
}

bool com_android_bluetooth_flags_le_audio_support_unidirectional_voice_assistant() {
  return com::android::bluetooth::flags::le_audio_support_unidirectional_voice_assistant();
}

bool com_android_bluetooth_flags_le_periodic_scanning_reassembler() {
  return com::android::bluetooth::flags::le_periodic_scanning_reassembler();
}

bool com_android_bluetooth_flags_le_scan_fix_remote_exception() {
  return com::android::bluetooth::flags::le_scan_fix_remote_exception();
}

bool com_android_bluetooth_flags_le_scan_use_address_type() {
  return com::android::bluetooth::flags::le_scan_use_address_type();
}

bool com_android_bluetooth_flags_le_scan_use_uid_for_importance() {
  return com::android::bluetooth::flags::le_scan_use_uid_for_importance();
}

bool com_android_bluetooth_flags_leaudio_add_sampling_frequencies() {
  return com::android::bluetooth::flags::leaudio_add_sampling_frequencies();
}

bool com_android_bluetooth_flags_leaudio_allow_leaudio_only_devices() {
  return com::android::bluetooth::flags::leaudio_allow_leaudio_only_devices();
}

bool com_android_bluetooth_flags_leaudio_allowed_context_mask() {
  return com::android::bluetooth::flags::leaudio_allowed_context_mask();
}

bool com_android_bluetooth_flags_leaudio_big_depends_on_audio_state() {
  return com::android::bluetooth::flags::leaudio_big_depends_on_audio_state();
}

bool com_android_bluetooth_flags_leaudio_broadcast_assistant_handle_command_statuses() {
  return com::android::bluetooth::flags::leaudio_broadcast_assistant_handle_command_statuses();
}

bool com_android_bluetooth_flags_leaudio_broadcast_assistant_peripheral_entrustment() {
  return com::android::bluetooth::flags::leaudio_broadcast_assistant_peripheral_entrustment();
}

bool com_android_bluetooth_flags_leaudio_broadcast_audio_handover_policies() {
  return com::android::bluetooth::flags::leaudio_broadcast_audio_handover_policies();
}

bool com_android_bluetooth_flags_leaudio_broadcast_destroy_after_timeout() {
  return com::android::bluetooth::flags::leaudio_broadcast_destroy_after_timeout();
}

bool com_android_bluetooth_flags_leaudio_broadcast_extract_periodic_scanner_from_state_machine() {
  return com::android::bluetooth::flags::
          leaudio_broadcast_extract_periodic_scanner_from_state_machine();
}

bool com_android_bluetooth_flags_leaudio_broadcast_feature_support() {
  return com::android::bluetooth::flags::leaudio_broadcast_feature_support();
}

bool com_android_bluetooth_flags_leaudio_broadcast_monitor_source_sync_status() {
  return com::android::bluetooth::flags::leaudio_broadcast_monitor_source_sync_status();
}

bool com_android_bluetooth_flags_leaudio_broadcast_update_metadata_callback() {
  return com::android::bluetooth::flags::leaudio_broadcast_update_metadata_callback();
}

bool com_android_bluetooth_flags_leaudio_broadcast_volume_control_for_connected_devices() {
  return com::android::bluetooth::flags::leaudio_broadcast_volume_control_for_connected_devices();
}

bool com_android_bluetooth_flags_leaudio_broadcast_volume_control_with_set_volume() {
  return com::android::bluetooth::flags::leaudio_broadcast_volume_control_with_set_volume();
}

bool com_android_bluetooth_flags_leaudio_call_start_scan_directly() {
  return com::android::bluetooth::flags::leaudio_call_start_scan_directly();
}

bool com_android_bluetooth_flags_leaudio_callback_on_group_stream_status() {
  return com::android::bluetooth::flags::leaudio_callback_on_group_stream_status();
}

bool com_android_bluetooth_flags_leaudio_codec_config_callback_order_fix() {
  return com::android::bluetooth::flags::leaudio_codec_config_callback_order_fix();
}

bool com_android_bluetooth_flags_leaudio_dynamic_spatial_audio() {
  return com::android::bluetooth::flags::leaudio_dynamic_spatial_audio();
}

bool com_android_bluetooth_flags_leaudio_getting_active_state_support() {
  return com::android::bluetooth::flags::leaudio_getting_active_state_support();
}

bool com_android_bluetooth_flags_leaudio_hal_client_asrc() {
  return com::android::bluetooth::flags::leaudio_hal_client_asrc();
}

bool com_android_bluetooth_flags_leaudio_mono_location_errata() {
  return com::android::bluetooth::flags::leaudio_mono_location_errata();
}

bool com_android_bluetooth_flags_leaudio_multicodec_aidl_support() {
  return com::android::bluetooth::flags::leaudio_multicodec_aidl_support();
}

bool com_android_bluetooth_flags_leaudio_multiple_vocs_instances_api() {
  return com::android::bluetooth::flags::leaudio_multiple_vocs_instances_api();
}

bool com_android_bluetooth_flags_leaudio_no_context_validate_streaming_request() {
  return com::android::bluetooth::flags::leaudio_no_context_validate_streaming_request();
}

bool com_android_bluetooth_flags_leaudio_quick_leaudio_toggle_switch_fix() {
  return com::android::bluetooth::flags::leaudio_quick_leaudio_toggle_switch_fix();
}

bool com_android_bluetooth_flags_leaudio_resume_active_after_hfp_handover() {
  return com::android::bluetooth::flags::leaudio_resume_active_after_hfp_handover();
}

bool com_android_bluetooth_flags_leaudio_speed_up_reconfiguration_between_call() {
  return com::android::bluetooth::flags::leaudio_speed_up_reconfiguration_between_call();
}

bool com_android_bluetooth_flags_leaudio_start_request_state_mutex_check() {
  return com::android::bluetooth::flags::leaudio_start_request_state_mutex_check();
}

bool com_android_bluetooth_flags_leaudio_start_stream_race_fix() {
  return com::android::bluetooth::flags::leaudio_start_stream_race_fix();
}

bool com_android_bluetooth_flags_leaudio_synchronize_start() {
  return com::android::bluetooth::flags::leaudio_synchronize_start();
}

bool com_android_bluetooth_flags_leaudio_use_audio_mode_listener() {
  return com::android::bluetooth::flags::leaudio_use_audio_mode_listener();
}

bool com_android_bluetooth_flags_load_did_config_from_sysprops() {
  return com::android::bluetooth::flags::load_did_config_from_sysprops();
}

bool com_android_bluetooth_flags_maintain_call_index_after_conference() {
  return com::android::bluetooth::flags::maintain_call_index_after_conference();
}

bool com_android_bluetooth_flags_map_limit_notification() {
  return com::android::bluetooth::flags::map_limit_notification();
}

bool com_android_bluetooth_flags_mcp_allow_play_without_active_player() {
  return com::android::bluetooth::flags::mcp_allow_play_without_active_player();
}

bool com_android_bluetooth_flags_metadata_api_inactive_audio_device_upon_connection() {
  return com::android::bluetooth::flags::metadata_api_inactive_audio_device_upon_connection();
}

bool com_android_bluetooth_flags_msft_addr_tracking_quirk() {
  return com::android::bluetooth::flags::msft_addr_tracking_quirk();
}

bool com_android_bluetooth_flags_nrpa_non_connectable_adv() {
  return com::android::bluetooth::flags::nrpa_non_connectable_adv();
}

bool com_android_bluetooth_flags_opp_fix_multiple_notifications_issues() {
  return com::android::bluetooth::flags::opp_fix_multiple_notifications_issues();
}

bool com_android_bluetooth_flags_opp_ignore_content_observer_after_service_stop() {
  return com::android::bluetooth::flags::opp_ignore_content_observer_after_service_stop();
}

bool com_android_bluetooth_flags_opp_start_activity_directly_from_notification() {
  return com::android::bluetooth::flags::opp_start_activity_directly_from_notification();
}

bool com_android_bluetooth_flags_override_context_to_specify_device_id() {
  return com::android::bluetooth::flags::override_context_to_specify_device_id();
}

bool com_android_bluetooth_flags_pairing_name_discovery_addresss_mismatch() {
  return com::android::bluetooth::flags::pairing_name_discovery_addresss_mismatch();
}

bool com_android_bluetooth_flags_pairing_on_unknown_transport() {
  return com::android::bluetooth::flags::pairing_on_unknown_transport();
}

bool com_android_bluetooth_flags_pan_use_identity_address() {
  return com::android::bluetooth::flags::pan_use_identity_address();
}

bool com_android_bluetooth_flags_phy_to_native() {
  return com::android::bluetooth::flags::phy_to_native();
}

bool com_android_bluetooth_flags_pretend_network_service() {
  return com::android::bluetooth::flags::pretend_network_service();
}

bool com_android_bluetooth_flags_prevent_hogp_reconnect_when_connected() {
  return com::android::bluetooth::flags::prevent_hogp_reconnect_when_connected();
}

bool com_android_bluetooth_flags_randomize_device_level_media_ids() {
  return com::android::bluetooth::flags::randomize_device_level_media_ids();
}

bool com_android_bluetooth_flags_read_model_num_fix() {
  return com::android::bluetooth::flags::read_model_num_fix();
}

bool com_android_bluetooth_flags_remove_address_map_on_unbond() {
  return com::android::bluetooth::flags::remove_address_map_on_unbond();
}

bool com_android_bluetooth_flags_remove_dup_pairing_response_in_oob_pairing() {
  return com::android::bluetooth::flags::remove_dup_pairing_response_in_oob_pairing();
}

bool com_android_bluetooth_flags_reset_after_collision() {
  return com::android::bluetooth::flags::reset_after_collision();
}

bool com_android_bluetooth_flags_reset_ag_state_on_collision() {
  return com::android::bluetooth::flags::reset_ag_state_on_collision();
}

bool com_android_bluetooth_flags_respect_ble_scan_setting() {
  return com::android::bluetooth::flags::respect_ble_scan_setting();
}

bool com_android_bluetooth_flags_retry_esco_with_zero_retransmission_effort() {
  return com::android::bluetooth::flags::retry_esco_with_zero_retransmission_effort();
}

bool com_android_bluetooth_flags_rfcomm_always_disc_initiator_in_disc_wait_ua() {
  return com::android::bluetooth::flags::rfcomm_always_disc_initiator_in_disc_wait_ua();
}

bool com_android_bluetooth_flags_refactor_saving_messages_and_metadata() {
  return com::android::bluetooth::flags::refactor_saving_messages_and_metadata();
}

bool com_android_bluetooth_flags_rfcomm_always_use_mitm() {
  return com::android::bluetooth::flags::rfcomm_always_use_mitm();
}

bool com_android_bluetooth_flags_rfcomm_bypass_post_to_main() {
  return com::android::bluetooth::flags::rfcomm_bypass_post_to_main();
}

bool com_android_bluetooth_flags_rfcomm_prevent_unnecessary_collisions() {
  return com::android::bluetooth::flags::rfcomm_prevent_unnecessary_collisions();
}

bool com_android_bluetooth_flags_rnr_reset_state_at_cancel() {
  return com::android::bluetooth::flags::rnr_reset_state_at_cancel();
}

bool com_android_bluetooth_flags_rnr_store_device_type() {
  return com::android::bluetooth::flags::rnr_store_device_type();
}

bool com_android_bluetooth_flags_rnr_validate_page_scan_repetition_mode() {
  return com::android::bluetooth::flags::rnr_validate_page_scan_repetition_mode();
}

bool com_android_bluetooth_flags_run_ble_audio_ticks_in_worker_thread() {
  return com::android::bluetooth::flags::run_ble_audio_ticks_in_worker_thread();
}

bool com_android_bluetooth_flags_run_clock_recovery_in_worker_thread() {
  return com::android::bluetooth::flags::run_clock_recovery_in_worker_thread();
}

bool com_android_bluetooth_flags_save_initial_hid_connection_policy() {
  return com::android::bluetooth::flags::save_initial_hid_connection_policy();
}

bool com_android_bluetooth_flags_scan_manager_refactor() {
  return com::android::bluetooth::flags::scan_manager_refactor();
}

bool com_android_bluetooth_flags_scan_record_manufacturer_data_merge() {
  return com::android::bluetooth::flags::scan_record_manufacturer_data_merge();
}

bool com_android_bluetooth_flags_sec_dont_clear_keys_on_encryption_err() {
  return com::android::bluetooth::flags::sec_dont_clear_keys_on_encryption_err();
}

bool com_android_bluetooth_flags_separate_service_and_device_discovery() {
  return com::android::bluetooth::flags::separate_service_and_device_discovery();
}

bool com_android_bluetooth_flags_set_addressed_player() {
  return com::android::bluetooth::flags::set_addressed_player();
}

bool com_android_bluetooth_flags_settings_can_control_hap_preset() {
  return com::android::bluetooth::flags::settings_can_control_hap_preset();
}

bool com_android_bluetooth_flags_sink_audio_policy_handover() {
  return com::android::bluetooth::flags::sink_audio_policy_handover();
}

bool com_android_bluetooth_flags_signal_connecting_on_focus_gain() {
  return com::android::bluetooth::flags::signal_connecting_on_focus_gain();
}

bool com_android_bluetooth_flags_skip_unknown_robust_caching() {
  return com::android::bluetooth::flags::skip_unknown_robust_caching();
}

bool com_android_bluetooth_flags_stack_sdp_detect_nil_property_type() {
  return com::android::bluetooth::flags::stack_sdp_detect_nil_property_type();
}

bool com_android_bluetooth_flags_stack_sdp_disconnect_when_cancel_in_pending_state() {
  return com::android::bluetooth::flags::stack_sdp_disconnect_when_cancel_in_pending_state();
}

bool com_android_bluetooth_flags_support_exclusive_manager() {
  return com::android::bluetooth::flags::support_exclusive_manager();
}

bool com_android_bluetooth_flags_support_metadata_device_types_apis() {
  return com::android::bluetooth::flags::support_metadata_device_types_apis();
}

bool com_android_bluetooth_flags_suppress_hid_rejection_broadcast() {
  return com::android::bluetooth::flags::suppress_hid_rejection_broadcast();
}

bool com_android_bluetooth_flags_system_server_messenger() {
  return com::android::bluetooth::flags::system_server_messenger();
}

bool com_android_bluetooth_flags_temporary_pairing_device_properties() {
  return com::android::bluetooth::flags::temporary_pairing_device_properties();
}

bool com_android_bluetooth_flags_uncache_player_when_browsed_player_changes() {
  return com::android::bluetooth::flags::uncache_player_when_browsed_player_changes();
}

bool com_android_bluetooth_flags_unified_connection_manager() {
  return com::android::bluetooth::flags::unified_connection_manager();
}

bool com_android_bluetooth_flags_unix_file_socket_creation_failure() {
  return com::android::bluetooth::flags::unix_file_socket_creation_failure();
}

bool com_android_bluetooth_flags_update_active_device_in_band_ringtone() {
  return com::android::bluetooth::flags::update_active_device_in_band_ringtone();
}

bool com_android_bluetooth_flags_update_inquiry_result_on_flag_change() {
  return com::android::bluetooth::flags::update_inquiry_result_on_flag_change();
}

bool com_android_bluetooth_flags_update_sco_state_correctly_on_rfcomm_disconnect_during_codec_nego() {
  return com::android::bluetooth::flags::
          update_sco_state_correctly_on_rfcomm_disconnect_during_codec_nego();
}

bool com_android_bluetooth_flags_use_entire_message_handle() {
  return com::android::bluetooth::flags::use_entire_message_handle();
}

bool com_android_bluetooth_flags_use_le_shim_connection_map_guard() {
  return com::android::bluetooth::flags::use_le_shim_connection_map_guard();
}

bool com_android_bluetooth_flags_use_local_oob_extended_command() {
  return com::android::bluetooth::flags::use_local_oob_extended_command();
}

bool com_android_bluetooth_flags_vcp_mute_unmute() {
  return com::android::bluetooth::flags::vcp_mute_unmute();
}
