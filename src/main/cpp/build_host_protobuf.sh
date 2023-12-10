#!/bin/sh

host_dir="$(pwd)/../../_host"
build_dir="$host_dir/build"
prefix_dir="$host_dir/install"

if ! [ -d "$host_dir" ] || ! [ -d "build_dir" ]  || ! [ -d "prefix_dir" ] ; then
  mkdir -p "$host_dir"
  mkdir -p "$build_dir"
  mkdir -p "$prefix_dir"
fi

pbuf_src_dir="$(pwd)"
cd "$build_dir" || exit

autoreconf -fi "$pbuf_src_dir"
"$pbuf_src_dir"/configure --srcdir="$pbuf_src_dir" --prefix="$prefix_dir" --disable-shared
make install
