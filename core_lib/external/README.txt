===============================================
For macOS users or users with BSD version of getopt:
===============================================

As macOS comes with the BSD version of getopt, which does not support
supports long flags, a version of the gnu-getop package is shipped with Watchdog.

The getopt_mac binary was compiled as fat binary for the i386 and x86_64 architecture,
which should be fine for most users (support for ppc was dropped in 10.6 Snow Leopard in 2009).
If you are running a old mac with ppc you can download the library from 

http://frodo.looijaard.name/project/getopt

compile your system and replace the getopt_mac binary located in this folder with your new version.


================================================
$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
================================================

Alternatively, if you already have a version of getopt installed that supports long flags,
you can set the FLAGS_GETOPT_CMD variable to the binary path of getopt.

(extracted from shflags.sh)
# A script can override the default 'getopt' command by providing the path to
# an alternate implementation by defining the FLAGS_GETOPT_CMD variable.

================================================

