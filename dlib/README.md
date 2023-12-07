# dlib

The D2 standard library. Set the `D2PATH` environment variable and the 
compiler will automatically link against `dlib.obj`.

## Methods

None of these methods currently have NPE checks. _Caveat emptor_.

### `ifind`

`ifind: extern proc(haystack: string, needle: string): int`

Find a string within another string, and return its index, or -1 if not found.

### `substr`

`substr: extern proc(source: string, start: int, end: int): string`

Returns the substring of the soruce from `start` to `end.

### Type conversions

`btoi: extern proc(b: byte): int`: `BYTE` to `INT`

`btos: extern proc(b: type): string`: `BYTE` to `STRING`

`itod: extern proc(i: int): double`: `INT` to `DOUBLE`

`ltod: extern proc(el: long): double`: `LONG` to `DOUBLE`


## "Missing" methods

The following methods already exist in the C Runtime Library so they're
not in dlib:

`atoi: extern proc(s: string): int`

`atoll: extern proc(s: string): long`

`llround: extern proc(d: double): long`

(Note: in the C Runtime Library, `long long` is 64 bits, `int` is 32 bits.)


