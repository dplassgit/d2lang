package com.plasstech.lang.d2.codegen.x64;

import com.plasstech.lang.d2.codegen.Emitter;
import com.plasstech.lang.d2.type.SymTab;

class ArgsCodeGenerator {

  private final Emitter emitter;
  private final SymTab symbolTable;

  public ArgsCodeGenerator(Emitter emitter, SymTab symbolTable) {
    this.emitter = emitter;
    this.symbolTable = symbolTable;
  }

  public void generate() {
    if (symbolTable.get("ARGS") != null) {
      emitter.emit("; convert argc, argv to ARGS global array");

      emitter.emit("mov DWORD [RSP + 16], ECX");
      emitter.emit("mov QWORD [RSP + 24], RDX");
      emitter.emitExternCall("__main");

      // argc is now in [rsp+16] and argv start at [[rsp+24]]
      // we can use ANY register(s) we need because this is at the beginning of the whole file!
      emitter.emit("");
      // 1. allocate an array of size 8*argc + 5
      emitter.emit("mov edx, DWORD [RSP + 16]  ; edx = argc");
      emitter.emit("imul edx, 8  ; 8 bytes per string");
      emitter.emit("add edx, 5  ; 1 byte for dimensions, 4 for size");
      emitter.emit("mov RCX, 1  ; # of 'entries' for calloc");
      emitter.emitExternCall("calloc");
      // 2. put it at ARGS
      emitter.emit("mov [_ARGS], RAX");
      // 3. set ARGS[0] to 1, ARGS[1] to argc
      emitter.emit("mov BYTE [RAX], 1  ; # dimensions ");
      // edx may have been destroyed by calloc.
      emitter.emit("mov r8d, DWORD [RSP + 16]  ; r8d = argc");
      emitter.emit("mov DWORD [RAX + 1], r8d  ; argc");

      // 4. copy argv to ARGS[5]
      emitter.emit("");
      emitter.emit("; copy argv to the ARGS array");
      // we need to copy argc*8 bytes from argv to args+5
      emitter.emit("mov rcx, [_ARGS]  ; dest");
      emitter.emit("add rcx, 5  ; args+5");
      emitter.emit("mov rdx, [rsp+24] ; location of first entry");
      emitter.emit("imul r8d, 8  ; 8 bytes per string");
      emitter.emitExternCall("memcpy");
    }
  }
}
