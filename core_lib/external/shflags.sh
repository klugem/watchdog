# vim:et:ft=sh:sts=2:sw=2
#
# Copyright 2008-2018 Kate Ward. All Rights Reserved.
# Released under the Apache License 2.0 license.
# http://www.apache.org/licenses/LICENSE-2.0
#
# shFlags is an advanced command-line flag library for Unix shell scripts.
#
# Author: kate.ward@forestent.com (Kate Ward)
# https://github.com/kward/shflags
#
# This module implements something like the gflags library available
# from https://github.com/gflags/gflags.
#
# FLAG TYPES: This is a list of the DEFINE_*'s that you can do.  All flags take
# a name, default value, help-string, and optional 'short' name (one-letter
# name). Some flags have other arguments, which are described with the flag.
#
# DEFINE_string: takes any input, and interprets it as a string.
#
# DEFINE_boolean: does not take any arguments. Say --myflag to set
#   FLAGS_myflag to true, or --nomyflag to set FLAGS_myflag to false. For short
#   flags, passing the flag on the command-line negates the default value, i.e.
#   if the default is true, passing the flag sets the value to false.
#
# DEFINE_float: takes an input and interprets it as a floating point number. As
#   shell does not support floats per-se, the input is merely validated as
#   being a valid floating point value.
#
# DEFINE_integer: takes an input and interprets it as an integer.
#
# SPECIAL FLAGS: There are a few flags that have special meaning:
#   --help (or -?)  prints a list of all the flags in a human-readable fashion
#   --flagfile=foo  read flags from foo.  (not implemented yet)
#   --              as in getopt(), terminates flag-processing
#
# EXAMPLE USAGE:
#
#   -- begin hello.sh --
#   #! /bin/sh
#   . ./shflags
#   DEFINE_string name 'world' "somebody's name" n
#   FLAGS "$@" || exit $?
#   eval set -- "${FLAGS_ARGV}"
#   echo "Hello, ${FLAGS_name}."
#   -- end hello.sh --
#
#   $ ./hello.sh -n Kate
#   Hello, Kate.
#
# CUSTOMIZABLE BEHAVIOR:
#
# A script can override the default 'getopt' command by providing the path to
# an alternate implementation by defining the FLAGS_GETOPT_CMD variable.
#
# NOTES:
#
# * Not all systems include a getopt version that supports long flags. On these
#   systems, only short flags are recognized.

#==============================================================================
# shFlags
#
# Shared attributes:
#   flags_error:  last error message
#   flags_output: last function output (rarely valid)
#   flags_return: last return value
#
#   __flags_longNames: list of long names for all flags
#   __flags_shortNames: list of short names for all flags
#   __flags_boolNames: list of boolean flag names
#
#   __flags_opts: options parsed by getopt
#
# Per-flag attributes:
#   FLAGS_<flag_name>: contains value of flag named 'flag_name'
#   __flags_<flag_name>_default: the default flag value
#   __flags_<flag_name>_help: the flag help string
#   __flags_<flag_name>_short: the flag short name
#   __flags_<flag_name>_type: the flag type
#
# Notes:
# - lists of strings are space separated, and a null value is the '~' char.
#
### ShellCheck (http://www.shellcheck.net/)
# expr may be antiquated, but it is the only solution in some cases.
#   shellcheck disable=SC2003
# $() are not fully portable (POSIX != portable).
#   shellcheck disable=SC2006
# [ p -a q ] are well defined enough (vs [ p ] && [ q ]).
#   shellcheck disable=SC2166

# Return if FLAGS already loaded.
[ -n "${FLAGS_VERSION:-}" ] && return 0
FLAGS_VERSION='1.2.3'

################################
# change by Michael Kluge
# if we are on macOS try to use the binary shipped with Watchdog
if [[ $OSTYPE == darwin* && "${FLAGS_GETOPT_CMD}" == "" ]]; then
  # test if gnu-getopt is installed
  RET_GNU_GETOPT=$(which gnu-getopt)
  if [ $? -eq 0 ]; then
    FLAGS_GETOPT_CMD_TMP="${RET_GNU_GETOPT}"
  else 
    LIB_SCRIPT_FOLDER_FLAGS=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
    FLAGS_GETOPT_CMD_TMP="$LIB_SCRIPT_FOLDER_FLAGS/getopt_mac"
  fi

  # test, if binary works
  $(${FLAGS_GETOPT_CMD_TMP} -T)
  # test, if special test parameter returns a exit code of 4 as documented in http://frodo.looijaard.name/project/getopt
  if [ $? -eq 4 ]; then
    FLAGS_GETOPT_CMD=${FLAGS_GETOPT_CMD_TMP}
  else 
    echo "flags:WARN tried to use GNU mac version of getopt stored in '${FLAGS_GETOPT_CMD_TMP}' but failed. (see shflags.sh)" >&2
  fi
fi
################################

# Return values that scripts can use.
FLAGS_TRUE=0
FLAGS_FALSE=1
FLAGS_ERROR=2

# Logging levels.
FLAGS_LEVEL_DEBUG=0
FLAGS_LEVEL_INFO=1
FLAGS_LEVEL_WARN=2
FLAGS_LEVEL_ERROR=3
FLAGS_LEVEL_FATAL=4
__FLAGS_LEVEL_DEFAULT=${FLAGS_LEVEL_WARN}

# Determine some reasonable command defaults.
__FLAGS_EXPR_CMD='expr --'
__FLAGS_UNAME_S=`uname -s`
if [ "${__FLAGS_UNAME_S}" = 'BSD' ]; then
  __FLAGS_EXPR_CMD='gexpr --'
else
  _flags_output_=`${__FLAGS_EXPR_CMD} 2>&1`
  if [ $? -eq ${FLAGS_TRUE} -a "${_flags_output_}" = '--' ]; then
    # We are likely running inside BusyBox.
    __FLAGS_EXPR_CMD='expr'
  fi
  unset _flags_output_
fi

# Commands a user can override if desired.
FLAGS_EXPR_CMD=${FLAGS_EXPR_CMD:-${__FLAGS_EXPR_CMD}}
FLAGS_GETOPT_CMD=${FLAGS_GETOPT_CMD:-getopt}

#
# Logging functions.
#

__flags_level=0 # Default logging level.

_flags_debug() {
  [ ${__flags_level} -le ${FLAGS_LEVEL_DEBUG} ] || return
  echo "flags:DEBUG $*" >&2
}
_flags_info() {
  [ ${__flags_level} -le ${FLAGS_LEVEL_INFO} ] || return
  echo "flags:INFO $*" >&2
}
_flags_warn() {
  [ ${__flags_level} -le ${FLAGS_LEVEL_WARN} ] || return
  echo "flags:WARN $*" >&2
}
_flags_error() {
  [ ${__flags_level} -le ${FLAGS_LEVEL_ERROR} ] || return
  echo "flags:ERROR $*" >&2
}
_flags_fatal() {
  [ ${__flags_level} -le ${FLAGS_LEVEL_FATAL} ] || return
  echo "flags:FATAL $*" >&2
  exit ${FLAGS_ERROR}
}

# Get the logging level.
flags_loggingLevel() { echo ${__flags_level}; }

# Set the logging level.
#
# Args:
#   _flags_level_: integer: new logging level
# Returns:
#   nothing
flags_setLoggingLevel() {
  [ $# -ne 1 ] && _flags_fatal "flags_setLevel(): logging level missing"
  _flags_level_=$1
  [ "${_flags_level_}" -ge "${FLAGS_LEVEL_DEBUG}" \
    -a "${_flags_level_}" -le "${FLAGS_LEVEL_FATAL}" ] \
      || _flags_fatal "Invalid logging level '${_flags_level_}' specified."
  __flags_level=$1
  unset _flags_level_
}

#
# Shell checks.
#

if [ -n "${ZSH_VERSION:-}" ]; then
  setopt |grep "^shwordsplit$" >/dev/null
  if [ $? -ne ${FLAGS_TRUE} ]; then
    _flags_fatal 'zsh shwordsplit option is required for proper zsh operation'
  fi
  if [ -z "${FLAGS_PARENT:-}" ]; then
    _flags_fatal "zsh does not pass \$0 through properly. please declare' \
\"FLAGS_PARENT=\$0\" before calling shFlags"
  fi
fi

# Can we use built-ins?
( echo "${FLAGS_TRUE#0}"; ) >/dev/null 2>&1
if [ $? -eq ${FLAGS_TRUE} ]; then
  __FLAGS_USE_BUILTIN=${FLAGS_TRUE}
else
  __FLAGS_USE_BUILTIN=${FLAGS_FALSE}
fi

#
# Constants.
#

# Reserved flag names.
__FLAGS_RESERVED_LIST=' ARGC ARGV ERROR FALSE GETOPT_CMD HELP PARENT TRUE '
__FLAGS_RESERVED_LIST="${__FLAGS_RESERVED_LIST} VERSION "

# Determined getopt version (standard or enhanced).
__FLAGS_GETOPT_VERS_STD=0
__FLAGS_GETOPT_VERS_ENH=1

# shellcheck disable=SC2120
_flags_getopt_vers() {
  _flags_getopt_cmd_=${1:-${FLAGS_GETOPT_CMD}}
  case "`${_flags_getopt_cmd_} -lfoo '' --foo 2>&1`" in
    ' -- --foo') echo ${__FLAGS_GETOPT_VERS_STD} ;;
    ' --foo --') echo ${__FLAGS_GETOPT_VERS_ENH} ;;
    # Unrecognized output. Assuming standard getopt version.
    *) echo ${__FLAGS_GETOPT_VERS_STD} ;;
  esac
  unset _flags_getopt_cmd_
}
# shellcheck disable=SC2119
__FLAGS_GETOPT_VERS=`_flags_getopt_vers`

# getopt optstring lengths
__FLAGS_OPTSTR_SHORT=0
__FLAGS_OPTSTR_LONG=1

__FLAGS_NULL='~'

# Flag info strings.
__FLAGS_INFO_DEFAULT='default'
__FLAGS_INFO_HELP='help'
__FLAGS_INFO_SHORT='short'
__FLAGS_INFO_TYPE='type'

# Flag lengths.
__FLAGS_LEN_SHORT=0
__FLAGS_LEN_LONG=1

# Flag types.
__FLAGS_TYPE_NONE=0
__FLAGS_TYPE_BOOLEAN=1
__FLAGS_TYPE_FLOAT=2
__FLAGS_TYPE_INTEGER=3
__FLAGS_TYPE_STRING=4

# Set the constants readonly.
__flags_constants=`set |awk -F= '/^FLAGS_/ || /^__FLAGS_/ {print $1}'`
for __flags_const in ${__flags_constants}; do
  # Skip certain flags.
  case ${__flags_const} in
    FLAGS_HELP) continue ;;
    FLAGS_PARENT) continue ;;
  esac
  # Set flag readonly.
  if [ -z "${ZSH_VERSION:-}" ]; then
    readonly "${__flags_const}"
    continue
  fi
  case ${ZSH_VERSION} in
    [123].*) readonly "${__flags_const}" ;;
    *) readonly -g "${__flags_const}" ;;  # Declare readonly constants globally.
  esac
done
unset __flags_const __flags_constants

#
# Internal variables.
#

# Space separated lists.
__flags_boolNames=' '     # Boolean flag names.
__flags_longNames=' '     # Long flag names.
__flags_shortNames=' '    # Short flag names.
__flags_definedNames=' '  # Defined flag names (used for validation).

__flags_columns=''  # Screen width in columns.
__flags_opts=''     # Temporary storage for parsed getopt flags.

# Define a flag.
#
# Calling this function will define the following info variables for the
# specified flag:
#   FLAGS_flagname - the name for this flag (based upon the long flag name)
#   __flags_<flag_name>_default - the default value
#   __flags_flagname_help - the help string
#   __flags_flagname_short - the single letter alias
#   __flags_flagname_type - the type of flag (one of __FLAGS_TYPE_*)
#
# Args:
#   _flags_type_: integer: internal type of flag (__FLAGS_TYPE_*)
#   _flags_name_: string: long flag name
#   _flags_default_: default flag value
#   _flags_help_: string: help string
#   _flags_short_: string: (optional) short flag name
# Returns:
#   integer: success of operation, or error
_flags_define() {
  if [ $# -lt 4 ]; then
    flags_error='DEFINE error: too few arguments'
    flags_return=${FLAGS_ERROR}
    _flags_error "${flags_error}"
    return ${flags_return}
  fi

  _flags_type_=$1
  _flags_name_=$2
  _flags_default_=$3
  _flags_help_=${4:-§}  # Special value '§' indicates no help string provided.
  _flags_short_=${5:-${__FLAGS_NULL}}

  _flags_debug "type:${_flags_type_} name:${_flags_name_}" \
      "default:'${_flags_default_}' help:'${_flags_help_}'" \
      "short:${_flags_short_}"

  _flags_return_=${FLAGS_TRUE}
  _flags_usName_="`_flags_underscoreName "${_flags_name_}"`"

  # Check whether the flag name is reserved.
  _flags_itemInList "${_flags_usName_}" "${__FLAGS_RESERVED_LIST}"
  if [ $? -eq ${FLAGS_TRUE} ]; then
    flags_error="flag name (${_flags_name_}) is reserved"
    _flags_return_=${FLAGS_ERROR}
  fi

  # Require short option for getopt that don't support long options.
  if [ ${_flags_return_} -eq ${FLAGS_TRUE} \
      -a "${__FLAGS_GETOPT_VERS}" -ne "${__FLAGS_GETOPT_VERS_ENH}" \
      -a "${_flags_short_}" = "${__FLAGS_NULL}" ]
  then
    flags_error="short flag required for (${_flags_name_}) on this platform"
    _flags_return_=${FLAGS_ERROR}
  fi

  # Check for existing long name definition.
  if [ ${_flags_return_} -eq ${FLAGS_TRUE} ]; then
    if _flags_itemInList "${_flags_usName_}" "${__flags_definedNames}"; then
      flags_error="definition for ([no]${_flags_name_}) already exists"
      _flags_warn "${flags_error}"
      _flags_return_=${FLAGS_FALSE}
    fi
  fi

  # Check for existing short name definition.
  if [ ${_flags_return_} -eq ${FLAGS_TRUE} \
      -a "${_flags_short_}" != "${__FLAGS_NULL}" ]
  then
    if _flags_itemInList "${_flags_short_}" "${__flags_shortNames}"; then
      flags_error="flag short name (${_flags_short_}) already defined"
      _flags_warn "${flags_error}"
      _flags_return_=${FLAGS_FALSE}
    fi
  fi

  # Handle default value. Note, on several occasions the 'if' portion of an
  # if/then/else contains just a ':' which does nothing. A binary reversal via
  # '!' is not done because it does not work on all shells.
  if [ ${_flags_return_} -eq ${FLAGS_TRUE} ]; then
    case ${_flags_type_} in
      ${__FLAGS_TYPE_BOOLEAN})
        if _flags_validBool "${_flags_default_}"; then
          case ${_flags_default_} in
            true|t|0) _flags_default_=${FLAGS_TRUE} ;;
            false|f|1) _flags_default_=${FLAGS_FALSE} ;;
          esac
        else
          flags_error="invalid default flag value '${_flags_default_}'"
          _flags_return_=${FLAGS_ERROR}
        fi
        ;;

      ${__FLAGS_TYPE_FLOAT})
        if _flags_validFloat "${_flags_default_}"; then
          :
        else
          flags_error="invalid default flag value '${_flags_default_}'"
          _flags_return_=${FLAGS_ERROR}
        fi
        ;;

      ${__FLAGS_TYPE_INTEGER})
        if _flags_validInt "${_flags_default_}"; then
          :
        else
          flags_error="invalid default flag value '${_flags_default_}'"
          _flags_return_=${FLAGS_ERROR}
        fi
        ;;

      ${__FLAGS_TYPE_STRING}) ;;  # Everything in shell is a valid string.

      *)
        flags_error="unrecognized flag type '${_flags_type_}'"
        _flags_return_=${FLAGS_ERROR}
        ;;
    esac
  fi

  if [ ${_flags_return_} -eq ${FLAGS_TRUE} ]; then
    # Store flag information.
    eval "FLAGS_${_flags_usName_}='${_flags_default_}'"
    eval "__flags_${_flags_usName_}_${__FLAGS_INFO_TYPE}=${_flags_type_}"
    eval "__flags_${_flags_usName_}_${__FLAGS_INFO_DEFAULT}=\
\"${_flags_default_}\""
    eval "__flags_${_flags_usName_}_${__FLAGS_INFO_HELP}=\"${_flags_help_}\""
    eval "__flags_${_flags_usName_}_${__FLAGS_INFO_SHORT}='${_flags_short_}'"

    # append flag names to name lists
    __flags_shortNames="${__flags_shortNames}${_flags_short_} "
    __flags_longNames="${__flags_longNames}${_flags_name_} "
    [ "${_flags_type_}" -eq "${__FLAGS_TYPE_BOOLEAN}" ] && \
        __flags_boolNames="${__flags_boolNames}no${_flags_name_} "

    # Append flag names to defined names for later validation checks.
    __flags_definedNames="${__flags_definedNames}${_flags_usName_} "
    [ "${_flags_type_}" -eq "${__FLAGS_TYPE_BOOLEAN}" ] && \
        __flags_definedNames="${__flags_definedNames}no${_flags_usName_} "
  fi

  flags_return=${_flags_return_}
  unset _flags_default_ _flags_help_ _flags_name_ _flags_return_ \
      _flags_short_ _flags_type_ _flags_usName_
  [ ${flags_return} -eq ${FLAGS_ERROR} ] && _flags_error "${flags_error}"
  return ${flags_return}
}

# Underscore a flag name by replacing dashes with underscores.
#
# Args:
#   unnamed: string: log flag name
# Output:
#   string: underscored name
_flags_underscoreName() {
  echo "$1" |tr z- z_
}

# Return valid getopt options using currently defined list of long options.
#
# This function builds a proper getopt option string for short (and long)
# options, using the current list of long options for reference.
#
# Args:
#   _flags_optStr: integer: option string type (__FLAGS_OPTSTR_*)
# Output:
#   string: generated option string for getopt
# Returns:
#   boolean: success of operation (always returns True)
_flags_genOptStr() {
  _flags_optStrType_=$1

  _flags_opts_=''

  for _flags_name_ in ${__flags_longNames}; do
    _flags_usName_="`_flags_underscoreName "${_flags_name_}"`"
    _flags_type_="`_flags_getFlagInfo "${_flags_usName_}" "${__FLAGS_INFO_TYPE}"`"
    [ $? -eq ${FLAGS_TRUE} ] || _flags_fatal 'call to _flags_type_ failed'
    case ${_flags_optStrType_} in
      ${__FLAGS_OPTSTR_SHORT})
        _flags_shortName_="`_flags_getFlagInfo \
            "${_flags_usName_}" "${__FLAGS_INFO_SHORT}"`"
        if [ "${_flags_shortName_}" != "${__FLAGS_NULL}" ]; then
          _flags_opts_="${_flags_opts_}${_flags_shortName_}"
          # getopt needs a trailing ':' to indicate a required argument.
          [ "${_flags_type_}" -ne "${__FLAGS_TYPE_BOOLEAN}" ] && \
              _flags_opts_="${_flags_opts_}:"
        fi
        ;;

      ${__FLAGS_OPTSTR_LONG})
        _flags_opts_="${_flags_opts_:+${_flags_opts_},}${_flags_name_}"
        # getopt needs a trailing ':' to indicate a required argument
        [ "${_flags_type_}" -ne "${__FLAGS_TYPE_BOOLEAN}" ] && \
            _flags_opts_="${_flags_opts_}:"
        ;;
    esac
  done

  echo "${_flags_opts_}"
  unset _flags_name_ _flags_opts_ _flags_optStrType_ _flags_shortName_ \
      _flags_type_ _flags_usName_
  return ${FLAGS_TRUE}
}

# Returns flag details based on a flag name and flag info.
#
# Args:
#   string: underscored flag name
#   string: flag info (see the _flags_define function for valid info types)
# Output:
#   string: value of dereferenced flag variable
# Returns:
#   integer: one of FLAGS_{TRUE|FALSE|ERROR}
_flags_getFlagInfo() {
  # Note: adding gFI to variable names to prevent naming conflicts with calling
  # functions
  _flags_gFI_usName_=$1
  _flags_gFI_info_=$2

  # Example: given argument usName (underscored flag name) of 'my_flag', and
  # argument info of 'help', set the _flags_infoValue_ variable to the value of
  # ${__flags_my_flag_help}, and see if it is non-empty.
  _flags_infoVar_="__flags_${_flags_gFI_usName_}_${_flags_gFI_info_}"
  _flags_strToEval_="_flags_infoValue_=\"\${${_flags_infoVar_}:-}\""
  eval "${_flags_strToEval_}"
  if [ -n "${_flags_infoValue_}" ]; then
    # Special value '§' indicates no help string provided.
    [ "${_flags_gFI_info_}" = ${__FLAGS_INFO_HELP} \
        -a "${_flags_infoValue_}" = '§' ] && _flags_infoValue_=''
    flags_return=${FLAGS_TRUE}
  else
    # See if the _flags_gFI_usName_ variable is a string as strings can be
    # empty...
    # Note: the DRY principle would say to have this function call itself for
    # the next three lines, but doing so results in an infinite loop as an
    # invalid _flags_name_ will also not have the associated _type variable.
    # Because it doesn't (it will evaluate to an empty string) the logic will
    # try to find the _type variable of the _type variable, and so on. Not so
    # good ;-)
    #
    # Example cont.: set the _flags_typeValue_ variable to the value of
    # ${__flags_my_flag_type}, and see if it equals '4'.
    _flags_typeVar_="__flags_${_flags_gFI_usName_}_${__FLAGS_INFO_TYPE}"
    _flags_strToEval_="_flags_typeValue_=\"\${${_flags_typeVar_}:-}\""
    eval "${_flags_strToEval_}"
    # shellcheck disable=SC2154
    if [ "${_flags_typeValue_}" = "${__FLAGS_TYPE_STRING}" ]; then
      flags_return=${FLAGS_TRUE}
    else
      flags_return=${FLAGS_ERROR}
      flags_error="missing flag info variable (${_flags_infoVar_})"
    fi
  fi

  echo "${_flags_infoValue_}"
  unset _flags_gFI_usName_ _flags_gfI_info_ _flags_infoValue_ _flags_infoVar_ \
      _flags_strToEval_ _flags_typeValue_ _flags_typeVar_
  [ ${flags_return} -eq ${FLAGS_ERROR} ] && _flags_error "${flags_error}"
  return ${flags_return}
}

# Check for presence of item in a list.
#
# Passed a string (e.g. 'abc'), this function will determine if the string is
# present in the list of strings (e.g.  ' foo bar abc ').
#
# Args:
#   _flags_str_: string: string to search for in a list of strings
#   unnamed: list: list of strings
# Returns:
#   boolean: true if item is in the list
_flags_itemInList() {
  _flags_str_=$1
  shift

  case " ${*:-} " in
    *\ ${_flags_str_}\ *) flags_return=${FLAGS_TRUE} ;;
    *) flags_return=${FLAGS_FALSE} ;;
  esac

  unset _flags_str_
  return ${flags_return}
}

# Returns the width of the current screen.
#
# Output:
#   integer: width in columns of the current screen.
_flags_columns() {
  if [ -z "${__flags_columns}" ]; then
    if eval stty size >/dev/null 2>&1; then
      # stty size worked :-)
      # shellcheck disable=SC2046
      set -- `stty size`
      __flags_columns="${2:-}"
    fi
  fi
  if [ -z "${__flags_columns}" ]; then
   if eval tput cols >/dev/null 2>&1; then
      # shellcheck disable=SC2046
      set -- `tput cols`
      __flags_columns="${1:-}"
    fi
  fi
  echo "${__flags_columns:-80}"
}

# Validate a boolean.
#
# Args:
#   _flags__bool: boolean: value to validate
# Returns:
#   bool: true if the value is a valid boolean
_flags_validBool() {
  _flags_bool_=$1

  flags_return=${FLAGS_TRUE}
  case "${_flags_bool_}" in
    true|t|0) ;;
    false|f|1) ;;
    *) flags_return=${FLAGS_FALSE} ;;
  esac

  unset _flags_bool_
  return ${flags_return}
}

# Validate a float.
#
# Args:
#   _flags_float_: float: value to validate
# Returns:
#   bool: true if the value is a valid integer
_flags_validFloat() {
  # allow empty default values (Michael Kluge)
  if [ -z $1 ]; then
    return ${FLAGS_TRUE};
  fi
  #####

  flags_return=${FLAGS_FALSE}
  [ -n "$1" ] || return ${flags_return}
  _flags_float_=$1

  if _flags_validInt "${_flags_float_}"; then
    flags_return=${FLAGS_TRUE}
  elif _flags_useBuiltin; then
    _flags_float_whole_=${_flags_float_%.*}
    _flags_float_fraction_=${_flags_float_#*.}
    [ "${_flags_float_whole_}" = '-' ] && _flags_float_whole_='-0'
    if _flags_validInt "${_flags_float_whole_:-0}" -a \
      _flags_validInt "${_flags_float_fraction_}"; then
      flags_return=${FLAGS_TRUE}
    fi
    unset _flags_float_whole_ _flags_float_fraction_
  else
    flags_return=${FLAGS_TRUE}
    case ${_flags_float_} in
      -*)  # Negative floats.
        _flags_test_=`${FLAGS_EXPR_CMD} "${_flags_float_}" :\
            '\(-[0-9]*\.[0-9]*\)'`
        ;;
      *)  # Positive floats.
        _flags_test_=`${FLAGS_EXPR_CMD} "${_flags_float_}" :\
            '\([0-9]*\.[0-9]*\)'`
        ;;
    esac
    [ "${_flags_test_}" != "${_flags_float_}" ] && flags_return=${FLAGS_FALSE}
    unset _flags_test_
  fi

  unset _flags_float_ _flags_float_whole_ _flags_float_fraction_
  return ${flags_return}
}

# Validate an integer.
#
# Args:
#   _flags_int_: integer: value to validate
# Returns:
#   bool: true if the value is a valid integer
_flags_validInt() {
  # allow empty default values (Michael Kluge)
  if [ -z $1 ]; then
    return ${FLAGS_TRUE};
  fi
  ###

  expr \( "$1" + '0' \) '=' "$1" >/dev/null 2>&1
}

# Parse command-line options using the standard getopt.
#
# Note: the flag options are passed around in the global __flags_opts so that
# the formatting is not lost due to shell parsing and such.
#
# Args:
#   @: varies: command-line options to parse
# Returns:
#   integer: a FLAGS success condition
_flags_getoptStandard() {
  flags_return=${FLAGS_TRUE}
  _flags_shortOpts_=`_flags_genOptStr ${__FLAGS_OPTSTR_SHORT}`

  # Check for spaces in passed options.
  for _flags_opt_ in "$@"; do
    # Note: the silliness with the x's is purely for ksh93 on Ubuntu 6.06.
    _flags_match_=`echo "x${_flags_opt_}x" |sed 's/ //g'`
    if [ "${_flags_match_}" != "x${_flags_opt_}x" ]; then
      flags_error='the available getopt does not support spaces in options'
      flags_return=${FLAGS_ERROR}
      break
    fi
  done

  if [ ${flags_return} -eq ${FLAGS_TRUE} ]; then
    __flags_opts=`getopt "${_flags_shortOpts_}" "$@" 2>&1`
    _flags_rtrn_=$?
    if [ ${_flags_rtrn_} -ne ${FLAGS_TRUE} ]; then
      _flags_warn "${__flags_opts}"
      flags_error='unable to parse provided options with getopt.'
      flags_return=${FLAGS_ERROR}
    fi
  fi

  unset _flags_match_ _flags_opt_ _flags_rtrn_ _flags_shortOpts_
  return ${flags_return}
}

# Parse command-line options using the enhanced getopt.
#
# Note: the flag options are passed around in the global __flags_opts so that
# the formatting is not lost due to shell parsing and such.
#
# Args:
#   @: varies: command-line options to parse
# Returns:
#   integer: a FLAGS success condition
_flags_getoptEnhanced() {
  flags_return=${FLAGS_TRUE}
  _flags_shortOpts_=`_flags_genOptStr ${__FLAGS_OPTSTR_SHORT}`
  _flags_boolOpts_=`echo "${__flags_boolNames}" \
      |sed 's/^ *//;s/ *$//;s/ /,/g'`
  _flags_longOpts_=`_flags_genOptStr ${__FLAGS_OPTSTR_LONG}`

  __flags_opts=`${FLAGS_GETOPT_CMD} \
      -o "${_flags_shortOpts_}" \
      -l "${_flags_longOpts_},${_flags_boolOpts_}" \
      -- "$@" 2>&1`
  _flags_rtrn_=$?
  if [ ${_flags_rtrn_} -ne ${FLAGS_TRUE} ]; then
    _flags_warn "${__flags_opts}"
    flags_error='unable to parse provided options with getopt.'
    flags_return=${FLAGS_ERROR}
  fi

  unset _flags_boolOpts_ _flags_longOpts_ _flags_rtrn_ _flags_shortOpts_
  return ${flags_return}
}

# Dynamically parse a getopt result and set appropriate variables.
#
# This function does the actual conversion of getopt output and runs it through
# the standard case structure for parsing. The case structure is actually quite
# dynamic to support any number of flags.
#
# Args:
#   argc: int: original command-line argument count
#   @: varies: output from getopt parsing
# Returns:
#   integer: a FLAGS success condition
_flags_parseGetopt() {
  _flags_argc_=$1
  shift

  flags_return=${FLAGS_TRUE}

  if [ "${__FLAGS_GETOPT_VERS}" -ne "${__FLAGS_GETOPT_VERS_ENH}" ]; then
    # The @$ must be unquoted as it needs to be re-split.
    #   shellcheck disable=SC2068
    set -- $@
  else
    # Note the quotes around the `$@' -- they are essential!
    eval set -- "$@"
  fi

  # Provide user with the number of arguments to shift by later.
  # NOTE: the FLAGS_ARGC variable is obsolete as of 1.0.3 because it does not
  # properly give user access to non-flag arguments mixed in between flag
  # arguments. Its usage was replaced by FLAGS_ARGV, and it is being kept only
  # for backwards compatibility reasons.
  FLAGS_ARGC=`_flags_math "$# - 1 - ${_flags_argc_}"`
  # export FLAGS_ARGC # allow calling of scripts (modification by Michael Kluge)

  # Handle options. note options with values must do an additional shift.
  while true; do
    _flags_opt_=$1
    _flags_arg_=${2:-}
    _flags_type_=${__FLAGS_TYPE_NONE}
    _flags_name_=''

    # Determine long flag name.
    case "${_flags_opt_}" in
      --) shift; break ;;  # Discontinue option parsing.

      --*)  # Long option.
        if _flags_useBuiltin; then
          _flags_opt_=${_flags_opt_#*--}
        else
          _flags_opt_=`${FLAGS_EXPR_CMD} "${_flags_opt_}" : '--\(.*\)'`
        fi
        _flags_len_=${__FLAGS_LEN_LONG}
        if _flags_itemInList "${_flags_opt_}" "${__flags_longNames}"; then
          _flags_name_=${_flags_opt_}
        else
          # Check for negated long boolean version.
          if _flags_itemInList "${_flags_opt_}" "${__flags_boolNames}"; then
            if _flags_useBuiltin; then
              _flags_name_=${_flags_opt_#*no}
            else
              _flags_name_=`${FLAGS_EXPR_CMD} "${_flags_opt_}" : 'no\(.*\)'`
            fi
            _flags_type_=${__FLAGS_TYPE_BOOLEAN}
            _flags_arg_=${__FLAGS_NULL}
          fi
        fi
        ;;

      -*)  # Short option.
        if _flags_useBuiltin; then
          _flags_opt_=${_flags_opt_#*-}
        else
          _flags_opt_=`${FLAGS_EXPR_CMD} "${_flags_opt_}" : '-\(.*\)'`
        fi
        _flags_len_=${__FLAGS_LEN_SHORT}
        if _flags_itemInList "${_flags_opt_}" "${__flags_shortNames}"; then
          # Yes. Match short name to long name. Note purposeful off-by-one
          # (too high) with awk calculations.
          _flags_pos_=`echo "${__flags_shortNames}" \
              |awk 'BEGIN{RS=" ";rn=0}$0==e{rn=NR}END{print rn}' \
                  e="${_flags_opt_}"`
          _flags_name_=`echo "${__flags_longNames}" \
              |awk 'BEGIN{RS=" "}rn==NR{print $0}' rn="${_flags_pos_}"`
        fi
        ;;
    esac

    # Die if the flag was unrecognized.
    if [ -z "${_flags_name_}" ]; then
      flags_error="unrecognized option (${_flags_opt_})"
      flags_return=${FLAGS_ERROR}
      break
    fi

    # Set new flag value.
    _flags_usName_=`_flags_underscoreName "${_flags_name_}"`
    [ ${_flags_type_} -eq ${__FLAGS_TYPE_NONE} ] && \
        _flags_type_=`_flags_getFlagInfo \
            "${_flags_usName_}" ${__FLAGS_INFO_TYPE}`
    case ${_flags_type_} in
      ${__FLAGS_TYPE_BOOLEAN})
        if [ ${_flags_len_} -eq ${__FLAGS_LEN_LONG} ]; then
          if [ "${_flags_arg_}" != "${__FLAGS_NULL}" ]; then
            eval "FLAGS_${_flags_usName_}=${FLAGS_TRUE}"
          else
            eval "FLAGS_${_flags_usName_}=${FLAGS_FALSE}"
          fi
        else
          _flags_strToEval_="_flags_val_=\
\${__flags_${_flags_usName_}_${__FLAGS_INFO_DEFAULT}}"
          eval "${_flags_strToEval_}"
          # shellcheck disable=SC2154
          if [ "${_flags_val_}" -eq ${FLAGS_FALSE} ]; then
            eval "FLAGS_${_flags_usName_}=${FLAGS_TRUE}"
          else
            eval "FLAGS_${_flags_usName_}=${FLAGS_FALSE}"
          fi
        fi
        ;;

      ${__FLAGS_TYPE_FLOAT})
        if _flags_validFloat "${_flags_arg_}"; then
          eval "FLAGS_${_flags_usName_}='${_flags_arg_}'"
        else
          flags_error="invalid float value (${_flags_arg_})"
          flags_return=${FLAGS_ERROR}
          break
        fi
        ;;

      ${__FLAGS_TYPE_INTEGER})
        if _flags_validInt "${_flags_arg_}"; then
          eval "FLAGS_${_flags_usName_}='${_flags_arg_}'"
        else
          flags_error="invalid integer value (${_flags_arg_})"
          flags_return=${FLAGS_ERROR}
          break
        fi
        ;;

      ${__FLAGS_TYPE_STRING})
        eval "FLAGS_${_flags_usName_}='${_flags_arg_}'"
        ;;
    esac

    # Handle special case help flag.
    if [ "${_flags_usName_}" = 'help' ]; then
      # shellcheck disable=SC2154
      if [ "${FLAGS_help}" -eq ${FLAGS_TRUE} ]; then
        flags_help
        flags_error='help requested'
        flags_return=${FLAGS_FALSE}
        break
      fi
    fi

    # Shift the option and non-boolean arguments out.
    shift
    [ "${_flags_type_}" != ${__FLAGS_TYPE_BOOLEAN} ] && shift
  done

  # Give user back non-flag arguments.
  FLAGS_ARGV=''
  while [ $# -gt 0 ]; do
    FLAGS_ARGV="${FLAGS_ARGV:+${FLAGS_ARGV} }'$1'"
    shift
  done

  unset _flags_arg_ _flags_len_ _flags_name_ _flags_opt_ _flags_pos_ \
      _flags_strToEval_ _flags_type_ _flags_usName_ _flags_val_
  return ${flags_return}
}

# Perform some path using built-ins.
#
# Args:
#   $@: string: math expression to evaluate
# Output:
#   integer: the result
# Returns:
#   bool: success of math evaluation
_flags_math() {
  if [ $# -eq 0 ]; then
    flags_return=${FLAGS_FALSE}
  elif _flags_useBuiltin; then
    # Variable assignment is needed as workaround for Solaris Bourne shell,
    # which cannot parse a bare $((expression)).
    # shellcheck disable=SC2016
    _flags_expr_='$(($@))'
    eval echo ${_flags_expr_}
    flags_return=$?
    unset _flags_expr_
  else
    eval expr "$@"
    flags_return=$?
  fi

  return ${flags_return}
}

# Cross-platform strlen() implementation.
#
# Args:
#   _flags_str: string: to determine length of
# Output:
#   integer: length of string
# Returns:
#   bool: success of strlen evaluation
_flags_strlen() {
  _flags_str_=${1:-}

  if [ -z "${_flags_str_}" ]; then
    flags_output=0
  elif _flags_useBuiltin; then
    flags_output=${#_flags_str_}
  else
    flags_output=`${FLAGS_EXPR_CMD} "${_flags_str_}" : '.*'`
  fi
  flags_return=$?

  unset _flags_str_
  echo "${flags_output}"
  return ${flags_return}
}

# Use built-in helper function to enable unit testing.
#
# Args:
#   None
# Returns:
#   bool: true if built-ins should be used
_flags_useBuiltin() { return ${__FLAGS_USE_BUILTIN}; }

#------------------------------------------------------------------------------
# public functions
#
# A basic boolean flag. Boolean flags do not take any arguments, and their
# value is either 1 (false) or 0 (true). For long flags, the false value is
# specified on the command line by prepending the word 'no'. With short flags,
# the presence of the flag toggles the current value between true and false.
# Specifying a short boolean flag twice on the command results in returning the
# value back to the default value.
#
# A default value is required for boolean flags.
#
# For example, lets say a Boolean flag was created whose long name was 'update'
# and whose short name was 'x', and the default value was 'false'. This flag
# could be explicitly set to 'true' with '--update' or by '-x', and it could be
# explicitly set to 'false' with '--noupdate'.
DEFINE_boolean() { _flags_define ${__FLAGS_TYPE_BOOLEAN} "$@"; }

# Other basic flags.
DEFINE_float()   { _flags_define ${__FLAGS_TYPE_FLOAT} "$@"; }
DEFINE_integer() { _flags_define ${__FLAGS_TYPE_INTEGER} "$@"; }
DEFINE_string()  { _flags_define ${__FLAGS_TYPE_STRING} "$@"; }

# Parse the flags.
#
# Args:
#   unnamed: list: command-line flags to parse
# Returns:
#   integer: success of operation, or error
FLAGS() {
  # Define a standard 'help' flag if one isn't already defined.
  [ -z "${__flags_help_type:-}" ] && \
      DEFINE_boolean 'help' false 'show this help' 'h'

  ###########################################
  #        modify positional parameters     #
  #         change by Michael Kluge         #
  ###########################################
  args=("$@")
  declare -a newARGS=()
  regex="^['\"](.*)['\"]$"
  for ((i=0; i<${#args[@]}; i++)); do
    if [[ ${args[$i]} =~ $regex ]]; then
      newARGS[$i]="${BASH_REMATCH[1]}"
    else
      newARGS[$i]="${args[$i]}"
    fi
  done
  set --
  set -- "${newARGS[@]}"
  ###########################################

  # Parse options.
  if [ $# -gt 0 ]; then
    if [ "${__FLAGS_GETOPT_VERS}" -ne "${__FLAGS_GETOPT_VERS_ENH}" ]; then
      _flags_getoptStandard "$@"
    else
      _flags_getoptEnhanced "$@"
    fi
    flags_return=$?
  else
    # Nothing passed; won't bother running getopt.
    __flags_opts='--'
    flags_return=${FLAGS_TRUE}
  fi

  if [ ${flags_return} -eq ${FLAGS_TRUE} ]; then
    _flags_parseGetopt $# "${__flags_opts}"
    flags_return=$?
  fi

  [ ${flags_return} -eq ${FLAGS_ERROR} ] && _flags_fatal "${flags_error}"
  return ${flags_return}
}

# This is a helper function for determining the 'getopt' version for platforms
# where the detection isn't working. It simply outputs debug information that
# can be included in a bug report.
#
# Args:
#   none
# Output:
#   debug info that can be included in a bug report
# Returns:
#   nothing
flags_getoptInfo() {
  # Platform info.
  _flags_debug "uname -a: `uname -a`"
  _flags_debug "PATH: ${PATH}"

  # Shell info.
  if [ -n "${BASH_VERSION:-}" ]; then
    _flags_debug 'shell: bash'
    _flags_debug "BASH_VERSION: ${BASH_VERSION}"
  elif [ -n "${ZSH_VERSION:-}" ]; then
    _flags_debug 'shell: zsh'
    _flags_debug "ZSH_VERSION: ${ZSH_VERSION}"
  fi

  # getopt info.
  ${FLAGS_GETOPT_CMD} >/dev/null
  _flags_getoptReturn=$?
  _flags_debug "getopt return: ${_flags_getoptReturn}"
  _flags_debug "getopt --version: `${FLAGS_GETOPT_CMD} --version 2>&1`"

  unset _flags_getoptReturn
}

# Returns whether the detected getopt version is the enhanced version.
#
# Args:
#   none
# Output:
#   none
# Returns:
#   bool: true if getopt is the enhanced version
flags_getoptIsEnh() {
  test "${__FLAGS_GETOPT_VERS}" -eq "${__FLAGS_GETOPT_VERS_ENH}"
}

# Returns whether the detected getopt version is the standard version.
#
# Args:
#   none
# Returns:
#   bool: true if getopt is the standard version
flags_getoptIsStd() {
  test "${__FLAGS_GETOPT_VERS}" -eq "${__FLAGS_GETOPT_VERS_STD}"
}

# This is effectively a 'usage()' function. It prints usage information and
# exits the program with ${FLAGS_FALSE} if it is ever found in the command line
# arguments. Note this function can be overridden so other apps can define
# their own --help flag, replacing this one, if they want.
#
# Args:
#   none
# Returns:
#   integer: success of operation (always returns true)
flags_help() {
  if [ -n "${FLAGS_HELP:-}" ]; then
    echo "${FLAGS_HELP}" >&2
  else
    echo "USAGE: ${FLAGS_PARENT:-$0} [flags] args" >&2
  fi
  if [ -n "${__flags_longNames}" ]; then
    echo 'flags:' >&2
    for flags_name_ in ${__flags_longNames}; do
      flags_flagStr_=''
      flags_boolStr_=''
      flags_usName_=`_flags_underscoreName "${flags_name_}"`

      flags_default_=`_flags_getFlagInfo \
          "${flags_usName_}" ${__FLAGS_INFO_DEFAULT}`
      flags_help_=`_flags_getFlagInfo \
          "${flags_usName_}" ${__FLAGS_INFO_HELP}`
      flags_short_=`_flags_getFlagInfo \
          "${flags_usName_}" ${__FLAGS_INFO_SHORT}`
      flags_type_=`_flags_getFlagInfo \
          "${flags_usName_}" ${__FLAGS_INFO_TYPE}`

      [ "${flags_short_}" != "${__FLAGS_NULL}" ] && \
          flags_flagStr_="-${flags_short_}"

      if [ "${__FLAGS_GETOPT_VERS}" -eq "${__FLAGS_GETOPT_VERS_ENH}" ]; then
        [ "${flags_short_}" != "${__FLAGS_NULL}" ] && \
            flags_flagStr_="${flags_flagStr_},"
        # Add [no] to long boolean flag names, except the 'help' flag.
        [ "${flags_type_}" -eq ${__FLAGS_TYPE_BOOLEAN} \
          -a "${flags_usName_}" != 'help' ] && \
            flags_boolStr_='[no]'
        flags_flagStr_="${flags_flagStr_}--${flags_boolStr_}${flags_name_}:"
      fi

      case ${flags_type_} in
        ${__FLAGS_TYPE_BOOLEAN})
          if [ "${flags_default_}" -eq ${FLAGS_TRUE} ]; then
            flags_defaultStr_='true'
          else
            flags_defaultStr_='false'
          fi
          ;;
        ${__FLAGS_TYPE_FLOAT}|${__FLAGS_TYPE_INTEGER})
          flags_defaultStr_=${flags_default_} ;;
        ${__FLAGS_TYPE_STRING}) flags_defaultStr_="'${flags_default_}'" ;;
      esac
      flags_defaultStr_="(default: ${flags_defaultStr_})"

      flags_helpStr_="  ${flags_flagStr_}  ${flags_help_:+${flags_help_} }${flags_defaultStr_}"
      _flags_strlen "${flags_helpStr_}" >/dev/null
      flags_helpStrLen_=${flags_output}
      flags_columns_=`_flags_columns`

      if [ "${flags_helpStrLen_}" -lt "${flags_columns_}" ]; then
        echo "${flags_helpStr_}" >&2
      else
        echo "  ${flags_flagStr_}  ${flags_help_}" >&2
        # Note: the silliness with the x's is purely for ksh93 on Ubuntu 6.06
        # because it doesn't like empty strings when used in this manner.
        flags_emptyStr_="`echo \"x${flags_flagStr_}x\" \
            |awk '{printf "%"length($0)-2"s", ""}'`"
        flags_helpStr_="  ${flags_emptyStr_}  ${flags_defaultStr_}"
        _flags_strlen "${flags_helpStr_}" >/dev/null
        flags_helpStrLen_=${flags_output}

        if [ "${__FLAGS_GETOPT_VERS}" -eq "${__FLAGS_GETOPT_VERS_STD}" \
            -o "${flags_helpStrLen_}" -lt "${flags_columns_}" ]; then
          # Indented to match help string.
          echo "${flags_helpStr_}" >&2
        else
          # Indented four from left to allow for longer defaults as long flag
          # names might be used too, making things too long.
          echo "    ${flags_defaultStr_}" >&2
        fi
      fi
    done
  fi

  unset flags_boolStr_ flags_default_ flags_defaultStr_ flags_emptyStr_ \
      flags_flagStr_ flags_help_ flags_helpStr flags_helpStrLen flags_name_ \
      flags_columns_ flags_short_ flags_type_ flags_usName_
  return ${FLAGS_TRUE}
}

# Reset shflags back to an uninitialized state.
#
# Args:
#   none
# Returns:
#   nothing
flags_reset() {
  for flags_name_ in ${__flags_longNames}; do
    flags_usName_=`_flags_underscoreName "${flags_name_}"`
    flags_strToEval_="unset FLAGS_${flags_usName_}"
    for flags_type_ in \
        ${__FLAGS_INFO_DEFAULT} \
        ${__FLAGS_INFO_HELP} \
        ${__FLAGS_INFO_SHORT} \
        ${__FLAGS_INFO_TYPE}
    do
      flags_strToEval_=\
"${flags_strToEval_} __flags_${flags_usName_}_${flags_type_}"
    done
    eval "${flags_strToEval_}"
  done

  # Reset internal variables.
  __flags_boolNames=' '
  __flags_longNames=' '
  __flags_shortNames=' '
  __flags_definedNames=' '

  # Reset logging level back to default.
  flags_setLoggingLevel ${__FLAGS_LEVEL_DEFAULT}

  unset flags_name_ flags_type_ flags_strToEval_ flags_usName_
}

#
# Initialization
#

# Set the default logging level.
flags_setLoggingLevel ${__FLAGS_LEVEL_DEFAULT}
