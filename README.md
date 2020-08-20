# Sources
Sources end with .src.yaml
## name
Name of the source
## version
Version of the source

If a build with the same version exists, it may ignore any changes and treat the source as already built.

If omitted, the source will be rebuilt every time.
## remotes
Lists the files that need to be downloaded for this source.

Remotes will be processed in order, overwiting previous remotes if needed
### url
Lists the URL of the file
### sha2
SHA2-256 hash
### sha3
SHA3-256 hash - a warning will be issued if either the SHA2-256 or SHA3-256 hash is missing
### desination
Where the file should be.  Default is / for extractable files or /(filename of url) for non-extractable
### extract
Whether to extract the file or not.  Default is true if the file ends in a supported format, or false otherwise.

Supported formats:
* .tar.gz
* .tar.xz
* .tar.bz2
* .tar
* .zip
### strip
Distributed source files typically consist of an archive with a single folder inside of them.  Setting strip to true will take files inside of this folder.  Has no effect if extract is false.  Defaults to autodetecting (checks if there is exactly one file/folder within the extracted file).
## script
Script that is run to prepare the final source - list of strings.  Defaults to empty
# Packages
A package ends in .pkg.yaml
## name
Name of the package
## version
Version of the package

If a build with the same version exists, it may ignore any changes and treat the package as already built.

If omitted, the package will be rebuilt every time.
## from
Package that this should build from.
## source/sources
Source or list of sources.  Can be either a list or a single entry.  If an entry is a string, all defaults are used except name.
### name
Name of the source
### destination
Destination of the source; defaults to the root source directory
## script
Build script; list of strings
## test
Optional test script; list of strings