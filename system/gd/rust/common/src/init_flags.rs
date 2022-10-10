use log::{error, info};
use paste::paste;
use std::collections::HashMap;
use std::fmt;
use std::sync::Mutex;

// Fallback to bool when type is not specified
macro_rules! type_expand {
    () => {
        bool
    };
    ($type:ty) => {
        $type
    };
}

macro_rules! default_value {
    () => {
        false
    };
    ($type:ty) => {
        <$type>::default()
    };
    ($($type:ty)? = $default:tt) => {
        $default
    };
}

macro_rules! test_value {
    () => {
        true
    };
    ($type:ty) => {
        <$type>::default()
    };
}

macro_rules! create_getter_fn {
    ($flag:ident) => {
        paste! {
            #[doc = concat!(" Return true if ", stringify!($flag), " is enabled")]
            pub fn [<$flag _is_enabled>]() -> bool {
                FLAGS.lock().unwrap().$flag
            }
        }
    };
    ($flag:ident $type:ty) => {
        paste! {
            #[doc = concat!(" Return the flag value of ", stringify!($flag))]
            pub fn [<get_ $flag>]() -> $type {
                FLAGS.lock().unwrap().$flag
            }
        }
    };
}

macro_rules! init_flags {
    (flags: { $($flag:ident $(: $type:ty)? $(= $default:tt)?,)* }
     extra_fields: { $($extra_field:ident : $extra_field_type:ty $(= $extra_default:tt)?,)* }
     extra_parsed_flags: { $($extra_flag:tt => $extra_flag_fn:ident(_, _ $(,$extra_args:tt)*),)*}
     dependencies: { $($parent:ident => $child:ident),* }) => {

        struct InitFlags {
            $($flag : type_expand!($($type)?),)*
            $($extra_field : $extra_field_type,)*
        }

        impl Default for InitFlags {
            fn default() -> Self {
                Self {
                    $($flag : default_value!($($type)? $(= $default)?),)*
                    $($extra_field : default_value!($extra_field_type $(= $extra_default)?),)*
                }
            }
        }

        /// Sets all bool flags to true
        /// Set all other flags and extra fields to their default type value
        pub fn set_all_for_testing() {
            *FLAGS.lock().unwrap() = InitFlags {
                $($flag: test_value!($($type)?),)*
                $($extra_field: test_value!($extra_field_type),)*
            };
        }

        impl InitFlags {
            fn parse(flags: Vec<String>) -> Self {
                let mut init_flags = Self::default();

                for flag in flags {
                    let values: Vec<&str> = flag.split("=").collect();
                    if values.len() != 2 {
                        error!("Bad flag {}, must be in <FLAG>=<VALUE> format", flag);
                        continue;
                    }

                    match values[0] {
                        $(concat!("INIT_", stringify!($flag)) =>
                            init_flags.$flag = values[1].parse().unwrap_or(default_value!($($type)? $(= $default)?)),)*
                        $($extra_flag => $extra_flag_fn(&mut init_flags, values $(, $extra_args)*),)*
                        _ => error!("Unsaved flag: {} = {}", values[0], values[1])
                    }
                }

                init_flags.reconcile()
            }

            fn reconcile(mut self) -> Self {
                loop {
                    // dependencies can be specified in any order
                    $(if self.$parent && !self.$child {
                        self.$child = true;
                        continue;
                    })*
                    break;
                }

                // TODO: acl should not be off if l2cap is on, but need to reconcile legacy code
                if self.gd_l2cap {
                  // TODO This can never be turned off  self.gd_acl = false;
                }

                self
            }
        }

        impl fmt::Display for InitFlags {
            fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
                write!(f, concat!(
                    concat!($(concat!(stringify!($flag), "={}")),*),
                    $(concat!(stringify!($extra_field), "={}")),*),
                    $(self.$flag),*,
                    $(self.$extra_field),*)
            }
        }

        $(create_getter_fn!($flag $($type)?);)*
    }
}

#[derive(Default)]
struct ExplicitTagSettings {
    map: HashMap<String, bool>,
}

impl fmt::Display for ExplicitTagSettings {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(f, "{:?}", self.map)
    }
}

fn parse_logging_tag(flags: &mut InitFlags, values: Vec<&str>, enabled: bool) {
    for tag in values[1].split(',') {
        flags.logging_debug_explicit_tag_settings.map.insert(tag.to_string(), enabled);
    }
}

/// Return true if `tag` is enabled in the flag
pub fn is_debug_logging_enabled_for_tag(tag: &str) -> bool {
    let guard = FLAGS.lock().unwrap();
    *guard
        .logging_debug_explicit_tag_settings
        .map
        .get(tag)
        .unwrap_or(&guard.logging_debug_enabled_for_all)
}

fn parse_hci_adapter(flags: &mut InitFlags, values: Vec<&str>) {
    flags.hci_adapter = values[1].parse().unwrap_or(0);
}

init_flags!(
    flags: {
        btaa_hci = true,
        gatt_robust_caching_client = true,
        gatt_robust_caching_server,
        gd_core,
        gd_l2cap,
        gd_link_policy,
        gd_rust,
        gd_security,
        hci_adapter: i32,
        irk_rotation,
        logging_debug_enabled_for_all,
        sdp_serialization = true,
    }
    // extra_fields are not a 1 to 1 match with "INIT_*" flags
    extra_fields: {
        logging_debug_explicit_tag_settings: ExplicitTagSettings,
    }
    extra_parsed_flags: {
        "INIT_logging_debug_enabled_for_tags" => parse_logging_tag(_, _, true),
        "INIT_logging_debug_disabled_for_tags" => parse_logging_tag(_, _, false),
        "--hci" => parse_hci_adapter(_, _),
    }
    dependencies: {
        gd_core => gd_security
    }
);

lazy_static! {
    /// Store some flag values
    static ref FLAGS: Mutex<InitFlags> = Mutex::new(InitFlags::default());
    /// Store the uid of bluetooth
    pub static ref AID_BLUETOOTH: Mutex<u32> = Mutex::new(1002);
    /// Store the prefix for file system
    pub static ref MISC: Mutex<String> = Mutex::new("/data/misc/".to_string());
}

/// Loads the flag values from the passed-in vector of string values
pub fn load(raw_flags: Vec<String>) {
    crate::init_logging();

    let flags = InitFlags::parse(raw_flags);
    info!("Flags loaded: {}", flags);
    *FLAGS.lock().unwrap() = flags;
}

#[cfg(test)]
mod tests {
    use super::*;
    lazy_static! {
        /// do not run concurrent tests as they all use the same global init_flag struct and
        /// accessor
        static ref ASYNC_LOCK: Mutex<bool> = Mutex::new(false);
    }

    fn test_load(raw_flags: Vec<&str>) {
        let raw_flags = raw_flags.into_iter().map(|x| x.to_string()).collect();
        load(raw_flags);
    }

    #[test]
    fn simple_flag() {
        let _guard = ASYNC_LOCK.lock().unwrap();
        test_load(vec![
            "INIT_btaa_hci=false", //override a default flag
            "INIT_gatt_robust_caching_server=true",
        ]);
        assert!(!btaa_hci_is_enabled());
        assert!(gatt_robust_caching_server_is_enabled());
    }
    #[test]
    fn parsing_failure() {
        let _guard = ASYNC_LOCK.lock().unwrap();
        test_load(vec![
            "foo=bar=?",                                // vec length
            "foo=bar",                                  // flag not save
            "INIT_btaa_hci=not_false",                  // parse error but has default value
            "INIT_gatt_robust_caching_server=not_true", // parse error
        ]);
        assert!(btaa_hci_is_enabled());
        assert!(!gatt_robust_caching_server_is_enabled());
    }
    #[test]
    fn int_flag() {
        let _guard = ASYNC_LOCK.lock().unwrap();
        test_load(vec!["--hci=2"]);
        assert_eq!(get_hci_adapter(), 2);
    }
    #[test]
    fn explicit_flag() {
        let _guard = ASYNC_LOCK.lock().unwrap();
        test_load(vec![
            "INIT_logging_debug_enabled_for_all=true",
            "INIT_logging_debug_enabled_for_tags=foo,bar",
            "INIT_logging_debug_disabled_for_tags=foo,bar2",
            "INIT_logging_debug_enabled_for_tags=bar2",
        ]);
        assert!(!is_debug_logging_enabled_for_tag("foo"));
        assert!(is_debug_logging_enabled_for_tag("bar"));
        assert!(is_debug_logging_enabled_for_tag("bar2"));
        assert!(is_debug_logging_enabled_for_tag("unknown_flag"));
        assert!(logging_debug_enabled_for_all_is_enabled());
        FLAGS.lock().unwrap().logging_debug_enabled_for_all = false;
        assert!(!is_debug_logging_enabled_for_tag("foo"));
        assert!(is_debug_logging_enabled_for_tag("bar"));
        assert!(is_debug_logging_enabled_for_tag("bar2"));
        assert!(!is_debug_logging_enabled_for_tag("unknown_flag"));
        assert!(!logging_debug_enabled_for_all_is_enabled());
    }
}
