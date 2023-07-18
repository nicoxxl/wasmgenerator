# WASM Generator

This a simple prototype for a Minecraft map generator plugin from outside code. It uses a WebAssembly to have a function
that define each blocks.

## The plugin

It uses [wasmtime](https://wasmtime.dev/) through [kawamuray](https://github.com/kawamuray/wasmtime-java) wrapper.

It is made to use code generated from AssemblyScript and instead of directly calling a function returning a block for a
single coordinate, it is optimized to be asked a range of blocks, return a string containing in the first part
a list of block names separated by `;` and a in second part a list of indices separated by `;` ; the parts are separated
by `|`.

## Test Project

Simple test project used to showcase and test how to use the plugin.