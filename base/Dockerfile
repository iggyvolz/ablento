FROM debian
# Force use of bash
RUN ln -sf /bin/bash /bin/sh
RUN apt-get update
RUN apt-get upgrade -y
RUN apt-get install -y build-essential bison gawk python3 texinfo flex
RUN mkdir -p /lfs/{usr,lib,var,etc,bin,sbin,lib64,build,tools}
RUN groupadd lfs
RUN useradd -s /bin/bash -g lfs -m -k /dev/null lfs
RUN chown -R lfs:lfs /lfs
USER lfs
ENV LFS=/lfs
ENV LC_ALL=POSIX
ENV LFS_TGT=x86_64-lfs-linux-gnu
ENV PATH=/lfs/tools/bin:/bin:/usr/bin

# TESTS
# Check environment is proper
RUN test "$LFS" = "/lfs"
RUN test "$LC_ALL" = "POSIX"
RUN test "$LFS_TGT" = "x86_64-lfs-linux-gnu"
RUN test "$PATH" = "/lfs/tools/bin:/bin:/usr/bin"

# Check that LFS user and group exists
RUN id -u lfs
RUN test "`id -gn lfs`" = "lfs"

# Test that /lfs/var exists and has correct owner
RUN test -d /lfs/var
RUN test "`stat -c '%U %G' /lfs/var`" = "lfs lfs"


USER root
RUN echo 'test "$1" = "`echo -e "$1\n$2" | sort -V | head -n1`" || { echo "Version $2 is less than version '$1'"; exit 1; }' > /bin/vercmp
RUN chmod +x /bin/vercmp
USER lfs

# Test bash version
RUN vercmp 3.2 `bash --version|head -n1|cut -d\  -f4|cut -d\( -f1`

# Test /bin/sh is a symbolic link to /bin/bash
RUN test "`readlink -f /bin/sh`" = "/bin/bash"

# Test binutils version >= 2.25, <= 2.34
RUN vercmp 2.25 `ld --version | head -n1 | cut -d\  -f7`
RUN vercmp `ld --version | head -n1 | cut -d\  -f7` 2.34

# Test bison/yacc version >= 2.7
RUN vercmp 2.7 `bison --version | head -n1|cut -d\  -f4`
RUN vercmp 2.7 `yacc --version | head -n1|cut -d\  -f4`

# Test bzip2 version >= 1.0.4
RUN vercmp 1.0.4 `bzip2 --version 2>&1 < /dev/null | head -n1 | cut -d\  -f8 | cut -d, -f1`

# Test coreutils version >= 6.9
RUN vercmp 6.9 `chown --version | head -n1 | cut -d\) -f2|cut -d\  -f2`

# Test diffutils version >= 2.8.1
RUN vercmp 2.8.1 `diff --version | head -n1 | cut -d\  -f4`

# Test findutils version >= 4.2.31
RUN vercmp 4.2.31 `find --version | head -n1 | cut -d\  -f4 | cut -d\- -f1`

# Test awk/gawk version >= 4.0.1
RUN vercmp 4.0.1 `gawk --version | head -n1 | cut -d\  -f3 | cut -d, -f1`
RUN vercmp 4.0.1 `awk --version | head -n1 | cut -d\  -f3 | cut -d, -f1`

# Test gcc version >= 6.2, <= 10.1.0
RUN vercmp 6.2 `gcc --version | head -n1 | cut -d\  -f3 | cut -d\- -f1`
RUN vercmp `gcc --version | head -n1 | cut -d\  -f3 | cut -d\- -f1` 10.1.0
RUN vercmp 6.2 `g++ --version | head -n1 | cut -d\  -f3 | cut -d\- -f1`
RUN vercmp `g++ --version | head -n1 | cut -d\  -f3 | cut -d\- -f1` 10.1.0

# Test glibc version >= 2.11, <= 2.31
RUN vercmp 2.11 `ldd --version | head -n1 | cut -d\  -f4 | cut -d\- -f1`
RUN vercmp `ldd --version | head -n1 | cut -d\  -f4 | cut -d\- -f1` 2.31

# Test grep >= 2.5.2
RUN vercmp 2.5.2 `grep --version | head -n1 | cut -d\  -f4`

# Test gzip >= 1.3.12
RUN vercmp 1.3.12 `gzip --version | head -n1 | cut -d\  -f2`

# Test linux >= 3.2
RUN vercmp 3.2 `cat /proc/version | cut -d\  -f3 | cut -d- -f1`

# Test m4 >= 1.4.10
RUN vercmp 1.4.10 `m4 --version | head -n1 | cut -d\  -f4`

# Test make >= 4.0
RUN vercmp 4.0 `make --version | head -n1 | cut -d\  -f3`

# Test patch >= 2.5.4
RUN vercmp 2.5.4 `patch --version | head -n1 | cut -d\  -f3`

# Test perl >= 5.8.8
RUN vercmp 5.8.8 `perl -V:version | cut -d "'" -f2`

# Test python >= 3.4
RUN vercmp 3.4 `python3 --version | cut -d\  -f2`

# Test sed >= 4.1.5
RUN vercmp 4.1.5 `sed --version | head -n1 | cut -d\  -f4`

# Test tar >= 1.22
RUN vercmp 1.22 `tar --version | head -n1 | cut -d\  -f4`

# Test texinfo >= 4.7
RUN vercmp 4.7 `makeinfo --version | head -n1 | cut -d\  -f4`

# Test xz >= 5.0.0
RUN vercmp 5.0.0 `xz --version | head -n1 | cut -d\  -f4`

# Test that the C compiler can produce executables
WORKDIR /lfs/build
RUN echo "int main(){}" > dummy.c
RUN g++ -o dummy dummy.c
RUN ./dummy
RUN rm dummy{,.c}