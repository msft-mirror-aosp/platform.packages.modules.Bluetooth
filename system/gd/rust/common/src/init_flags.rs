use lazy_static::lazy_static;
use log::{error, info};
use paste::paste;
use std::collections::{BTreeMap, HashMap};
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

#[cfg(test)]
macro_rules! call_getter_fn {
    ($flag:ident) => {
        paste! {
            [<$flag _is_enabled>]()
        }
    };
    ($flag:ident $type:ty) => {
        paste! {
            [<get_ $flag>]()
        }
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
    (
        name: $name:ident
        $($args:tt)*
    ) => {
        init_flags_struct! {
            name: $name
            $($args)*
        }

        init_flags_getters! {
            $($args)*
        }
    }
}

trait FlagHolder: Default {
    fn get_defaults_for_test() -> Self;
    fn parse(flags: Vec<String>) -> Self;
    fn dump(&self) -> BTreeMap<&'static str, String>;
}

macro_rules! init_flags_struct {
    (
     name: $name:ident
     flags: { $($flag:ident $(: $type:ty)? $(= $default:tt)?,)* }
     extra_parsed_flags: { $($extra_flag:tt => $extra_flag_fn:ident(_, _ $(,$extra_args:tt)*),)*}) => {

        struct $name {
            $($flag : type_expand!($($type)?),)*
        }

        impl Default for $name {
            fn default() -> Self {
                Self {
                    $($flag : default_value!($($type)? $(= $default)?),)*
                }
            }
        }

        impl FlagHolder for $name {
            fn get_defaults_for_test() -> Self {
                Self {
                    $($flag: test_value!($($type)?),)*
                }
            }

            fn dump(&self) -> BTreeMap<&'static str, String> {
                [
                    $((stringify!($flag), format!("{}", self.$flag)),)*
                ].into()
            }

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
                            init_flags.$flag = values[1].parse().unwrap_or_else(|e| {
                                error!("Parse failure on '{}': {}", flag, e);
                                default_value!($($type)? $(= $default)?)}),)*
                        $($extra_flag => $extra_flag_fn(&mut init_flags, values $(, $extra_args)*),)*
                        _ => error!("Unsaved flag: {} = {}", values[0], values[1])
                    }
                }

                init_flags
            }
        }

        impl fmt::Display for $name {
            fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
                write!(f,
                    concat!($(concat!(stringify!($flag), "={}")),*),
                    $(self.$flag),*)
            }
        }

    }
}

macro_rules! init_flags_getters {
    (
     flags: { $($flag:ident $(: $type:ty)? $(= $default:tt)?,)* }
     extra_parsed_flags: { $($extra_flag:tt => $extra_flag_fn:ident(_, _ $(,$extra_args:tt)*),)*}) => {

        $(create_getter_fn!($flag $($type)?);)*

        #[cfg(test)]
        mod tests_autogenerated {
            use super::*;
            $(paste! {
                #[test]
                pub fn [<test_get_ $flag>]() {
                    let _guard = tests::ASYNC_LOCK.lock().unwrap();
                    tests::test_load(vec![
                        &*format!(concat!(concat!("INIT_", stringify!($flag)), "={}"), test_value!($($type)?))
                    ]);
                    let get_value = call_getter_fn!($flag $($type)?);
                    drop(_guard); // Prevent poisonning other tests if a panic occurs
                    assert_eq!(get_value, test_value!($($type)?));
                }
            })*
        }
    }
}

#[derive(Default)]
struct ExplicitTagSettings {
    map: HashMap<String, i32>,
}

impl fmt::Display for ExplicitTagSettings {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(f, "{:?}", self.map)
    }
}

fn parse_hci_adapter(flags: &mut InitFlags, values: Vec<&str>) {
    flags.hci_adapter = values[1].parse().unwrap_or(0);
}

/// Sets all bool flags to true
/// Set all other flags and extra fields to their default type value
pub fn set_all_for_testing() {
    *FLAGS.lock().unwrap() = InitFlags::get_defaults_for_test();
}

init_flags!(
    name: InitFlags
    flags: {
        hci_adapter: i32,
        use_unified_connection_manager,
    }
    extra_parsed_flags: {
        "--hci" => parse_hci_adapter(_, _),
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

/// Dumps all flag K-V pairs, storing values as strings
pub fn dump() -> BTreeMap<&'static str, String> {
    FLAGS.lock().unwrap().dump()
}

#[cfg(test)]
mod tests {
    use super::*;
    lazy_static! {
        /// do not run concurrent tests as they all use the same global init_flag struct and
        /// accessor
        pub(super) static ref ASYNC_LOCK: Mutex<bool> = Mutex::new(false);
    }

    pub(super) fn test_load(raw_flags: Vec<&str>) {
        let raw_flags = raw_flags.into_iter().map(|x| x.to_string()).collect();
        load(raw_flags);
    }

    #[test]
    fn int_flag() {
        let _guard = ASYNC_LOCK.lock().unwrap();
        test_load(vec!["--hci=2"]);
        assert_eq!(get_hci_adapter(), 2);
    }

    init_flags_struct!(
        name: InitFlagsForTest
        flags: {
            cat,
        }
        extra_parsed_flags: {}
    );

    #[test]
    fn test_dumpsys() {
        let flags = InitFlagsForTest { ..Default::default() };

        let out = flags.dump();

        assert_eq!(out.len(), 1);
        assert_eq!(out["cat"], "false");
    }
}
