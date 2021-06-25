package com.plasstech.lang.d2.common;

import com.google.devtools.common.options.Option;
import com.google.devtools.common.options.OptionsBase;

public class D2Options extends OptionsBase {
  @Option(name = "help", abbrev = 'h', help = "Prints usage info.", defaultValue = "false")
  public boolean help;

  @Option(name = "debuglex", help = "Sets debug level for lexer.", defaultValue = "0")
  public int debuglex;

  @Option(name = "debugparse", help = "Sets debug level for parser.", defaultValue = "0")
  public int debugparse;

  @Option(name = "debugtype", help = "Sets debug level for type checker.", defaultValue = "0")
  public int debugtype;

  @Option(name = "debugcodegen", help = "Sets debug level for code generator.", defaultValue = "0")
  public int debugcodegen;

  @Option(name = "debugopt", help = "Sets debug level for optimizer.", defaultValue = "0")
  public int debugopt;

  @Option(name = "debugint", help = "Sets debug level for interpreter.", defaultValue = "0")
  public int debugint;
}
