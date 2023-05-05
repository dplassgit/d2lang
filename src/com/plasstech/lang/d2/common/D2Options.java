package com.plasstech.lang.d2.common;

import com.google.devtools.common.options.EnumConverter;
import com.google.devtools.common.options.Option;
import com.google.devtools.common.options.OptionsBase;
import com.google.devtools.common.options.OptionsParsingException;

public class D2Options extends OptionsBase {
  @Option(
      name = "help",
      abbrev = 'h',
      help = "Prints usage info.",
      defaultValue = "false"
  )
  public boolean help;

  @Option(
      name = "debuglex",
      help = "Sets debug level for lexer.",
      defaultValue = "0"
  )
  public int debuglex;

  @Option(
      name = "debugparse",
      help = "Sets debug level for parser.",
      defaultValue = "0"
  )
  public int debugparse;

  @Option(
      name = "debugtype",
      help = "Sets debug level for type checker.",
      defaultValue = "0"
  )
  public int debugtype;

  @Option(
      name = "debugcodegen",
      help = "Sets debug level for code generator.",
      defaultValue = "0"
  )
  public int debugcodegen;

  @Option(
      name = "debugopt",
      help = "Sets debug level for optimizer.",
      defaultValue = "0"
  )
  public int debugopt;

  @Option(
      name = "debugint",
      help = "Sets debug level for interpreter.",
      defaultValue = "0"
  )
  public int debugint;

  @Option(
      name = "optimize",
      help = "Turns on the optimizer.",
      defaultValue = "true"
  )
  public boolean optimize;

  @Option(
      name = "exe",
      abbrev = 'o',
      help = "Sets the executable name.",
      defaultValue = "d2out.exe"
  )
  public String exeName;

  @Option(
      name = "save-temps",
      help = "If the asm and obj files should be kept.",
      defaultValue = "false"
  )
  public boolean saveTemps;

  @Option(
      name = "compileOnly",
      abbrev = 'S',
      help = "Compile only; do not assemble or link (generates .asm).",
      defaultValue = "false"
  )
  public boolean compileOnly;

  @Option(
      name = "compileAndAssembleOnly",
      abbrev = 'c',
      help = "Compile and assemble; do not link (generates .asm and .obj).",
      defaultValue = "false"
  )
  public boolean compileAndAssembleOnly;

  @Option(
      name = "show-commands",
      abbrev = 'v',
      help = "Show intermediate commands",
      defaultValue = "false"
  )
  public boolean showCommands;

  @Option(
      name = "show-stack-traces",
      abbrev = 'T',
      help = "Shows full stack trace of compile-time errors",
      defaultValue = "false"
  )
  public boolean showStackTraces;

  @Option(
      name = "target",
      help = "Target architecture",
      defaultValue = "x64",
      converter = TargetConverter.class
  )
  public Target target;

  public static class TargetConverter extends EnumConverter<Target> {
    public TargetConverter() {
      super(Target.class, "Target");
    }

    @Override
    public Target convert(String input) throws OptionsParsingException {
      Target it = super.convert(input);
      Target.setTarget(it);
      return it;
    }
  }
}
