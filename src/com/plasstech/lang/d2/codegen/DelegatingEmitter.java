package com.plasstech.lang.d2.codegen;

import com.google.common.collect.ImmutableList;

public class DelegatingEmitter implements Emitter {
  private Emitter delegate;

  public DelegatingEmitter(Emitter emitter) {
    this.delegate = emitter;
  }

  public DelegatingEmitter setDelegate(Emitter delegate) {
    this.delegate = delegate;
    return this;
  }

  public Emitter getDelegate() {
    return delegate;
  }

  @Override
  public void emit0(String string, Object... values) {
    delegate.emit0(string, values);
  }

  @Override
  public void emit(String string, Object... values) {
    delegate.emit(string, values);
  }

  @Override
  public ImmutableList<String> all() {
    return delegate.all();
  }

  @Override
  public void addExtern(String extern) {
    delegate.addExtern(extern);
  }

  @Override
  public ImmutableList<String> externs() {
    return delegate.externs();
  }

  @Override
  public void emitExternCall(String call) {
    delegate.emitExternCall(call);
  }

  @Override
  public void addData(String data) {
    delegate.addData(data);
  }

  @Override
  public ImmutableList<String> data() {
    return delegate.data();
  }

  @Override
  public void emitExit(int exitCode) {
    delegate.emitExit(exitCode);
  }

  @Override
  public void emitLabel(String label) {
    delegate.emitLabel(label);
  }
}
