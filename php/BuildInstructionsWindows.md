# Building Ice for PHP on Windows

This page describes how to build and install Ice for PHP from source code on
Windows. If you prefer, you can also download [binary distributions][1] for the
supported platforms.

## PHP Build Requirements

### Operating Systems and Compilers

Ice for PHP was extensively tested using the operating systems and compiler
versions listed for our [supported platforms][2].

The build requires the [Ice Builder for Visual Studio][3]. You must install
version 4.3.7 or greater to build Ice.

## Building the PHP Extension

Open a Visual Studio command prompt. For example, with Visual Studio 2015, you
can open one of:

- VS2015 x86 Native Tools Command Prompt
- VS2015 x64 Native Tools Command Prompt

Using the first Command Prompt produces `Win32` binaries by default, while
the second Command Promt produces `x64` binaries by default.

In the Command Prompt, change to the `php` subdirectory:

    cd php

Now you're ready to build Ice for PHP:

    msbuild msbuild\ice.proj

This builds the extension with `Release` binaries for the default platform.
The extension will be placed in `lib\x64\Release\php_ice.dll` for the `x64` platform
and `lib\Win32\Release\php_ice.dll` for the `Win32` platform.

The default configuration builds the extension against the thread-safe PHP run time.
You can build with the non-thread-safe run time using the `NTS-Release` or `NTS-Debug`
configuration:

    msbuild msbuild\ice.proj /p:Configuration=NTS-Release

The extension will be placed in `lib\x64\Release\php_ice_nts.dll` directory for
the `x64` platform and `lib\Win32\Release\php_ice_nts.dll` for the `Win32` platform.

The extension is built by default with namespaces enabled. You can build the PHP
extension with namespaces disabled by setting the MSBuild property `PhpUseNamespaces`
to `no`:

    msbuild msbuild\ice.proj /p:PhpUseNamespaces=no

The PHP extension depends on Ice for C++ components from the `cpp` subdirectory, and those
are built if required. It is also possible to build the PHP extension using the Ice for C++
NuGet packages by setting the`ICE_BIN_DIST` msbuild property to `cpp`:

    msbuild msbuild\ice.proj /p:ICE_BIN_DIST=cpp

## Installing the PHP Extension

To install the Ice extension, you must move the extension's shared library into
PHP's extension directory. This directory is determined by the PHP configuration
directive `extension_dir`. You can determine the default value for this
directive by running the command-line version of PHP with the `-i` option:

    > php -i

Review the output for a line like this:

    extension_dir => C:\Program Files\iis express\PHP\v7.1\ext\ => C:\Program Files\iis express\PHP\v7.1\ext\

Once you've copied the extension to the appropriate directory, you will need
to enable the extension in your PHP configuration. First you must discover the
location of PHP's configuration file (`php.ini`), which is also displayed by
the `-i` option. Look for the following line:

    Loaded Configuration File => C:\Program Files\iis express\PHP\v7.1\php.ini

Open `php.ini` and append the following directive:

    extension=php_ice_nts.dll

Read the PHP Dependencies and PHP Source Files sections below for more
information about installing the Ice extension.

## PHP Dependencies

PHP will need to be able to locate the libraries for the Ice run-time libraries
and its third-party dependencies. On Windows, these DLLs are required:

    bzip2.dll
    ice37.dll
    icediscovery37.dll
    icelocatordiscovery37.dll
    icessl37.dll

In general, these libraries must reside in a directory of the user's PATH. For
IIS configured to run PHP as FastCGI, the simplest solution is to copy the
libraries next to the php-cgi.exe in C:\Program Files\iis express\PHP\v7.1.

You can verify that the Ice extension is installed properly by examining the
output of the `php -m` command, or by calling the `phpInfo()` function from a
script. For example, you can create a file in the Web server's document
directory containing the following PHP script:

    <?php
    phpInfo();
    ?>

Then start a browser window and open the URL corresponding to this script. If
the Ice extension is successfully installed, you will see an `ice` section
among the configuration information.

## PHP Source Files

In addition to the binary Ice extension module and its library dependencies,
you will also need to make the Ice for PHP source files available to your
scripts. These files are located in the `lib` subdirectory and consist of the
Ice run time definitions (`Ice.php`) along with PHP source
files generated from the Slice files included in the Ice distribution.

The Ice extension makes no assumptions about the location of these files, so
you can install them anywhere you like.

An easy way to do that is to update the PHP include path in php.ini to include
the directory with Ice for PHP sources, to do that open `php.ini` and append
the following directive:

    include_path=${include_path}";C\ice\php\lib"

## Running the PHP Tests

The test subdirectory contains PHP implementations of the core Ice test suite.
Python is required to run the test suite.

The test suites also require that the Ice for C++ tests be built in the `cpp`
subdirectory of this source distribution. In addition, the scripts require
that the CLI version of the PHP interpreter be available in your PATH.

After a successful build, you can run the tests as follows:

    python allTests.py

If you have built the extension using the Ice for C++ NuGet packages, you must
also set the `ICE_BIN_DIST` environment variable to `cpp` for testing:

    set ICE_BIN_DIST=cpp
    python allTests.py

If everything worked out, you should see lots of `ok` messages. In case of a
failure, the tests abort with `failed`.

[1]: https://zeroc.com/distributions/ice
[2]: https://doc.zeroc.com/display/Rel/Supported+Platforms+for+Ice+3.7.0
[3]: https://github.com/zeroc-ice/ice-builder-visualstudio
