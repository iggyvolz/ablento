# Sources
Sources end with .src.yaml
## name
Name of the source
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
# Packages
A package ends in .pkg.yaml
## name
Name of the package
## from
Package that this should build from.
## source
Source or list of sources.
Of the form: name:destination,name:destination (destination may be omitted)
### name
Name of the source
### destination
Destination of the source; defaults to the root source directory
## script
Build script; list of strings
## test
Optional test script; list of strings