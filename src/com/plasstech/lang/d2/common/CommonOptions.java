package com.plasstech.lang.d2.common;

import com.google.devtools.common.options.Option;
import com.google.devtools.common.options.OptionsBase;

public class CommonOptions extends OptionsBase {
  @Option(
      name = "help",
      abbrev = 'h',
      help = "Prints usage info.",
      defaultValue = "false"
    )
  public boolean help;

  @Option(
      name = "debug",
      abbrev = 'd',
      help = "Sets debug level.",
      defaultValue = "0"
    )
  public int debug;
}
