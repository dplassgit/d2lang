package com.plasstech.lang.d2.common;

import javax.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.plasstech.lang.d2.phase.PhaseName;

@AutoValue
public abstract class CompilationConfiguration {
  public static CompilationConfiguration create(String sourceCode) {
    return builder().setSourceCode(sourceCode).build();
  }

  public static CompilationConfiguration create(String sourceCode, String filename) {
    return builder().setSourceCode(sourceCode).setFilename(filename).build();
  }

  public static Builder builder() {
    return new AutoValue_CompilationConfiguration.Builder()
        .setLastPhase(PhaseName.ASM_OPTIMIZE) // maximum
        .setLexDebugLevel(0)
        .setParseDebugLevel(0)
        .setTypeDebugLevel(0)
        .setOptDebugLevel(1)
        .setCodeGenDebugLevel(1)
        .setOptimize(false)
        // no error phase means all should succeed
        .setExpectedErrorPhase(PhaseName.PHASE_UNDEFINED);
  }

  public abstract Builder toBuilder();

  public abstract String sourceCode();

  @Nullable
  public abstract String filename();

  public abstract PhaseName lastPhase();

  public abstract boolean optimize();

  public abstract PhaseName expectedErrorPhase();

  @Nullable
  public abstract String expectedErrorMessage();

  public boolean expectedError() {
    return expectedErrorPhase() != PhaseName.PHASE_UNDEFINED;
  }

  public abstract int lexDebugLevel();

  public abstract int parseDebugLevel();

  public abstract int typeDebugLevel();

  public abstract int codeGenDebugLevel();

  public abstract int optDebugLevel();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setSourceCode(String source);

    public abstract Builder setLastPhase(PhaseName phase);

    public abstract Builder setLexDebugLevel(int i);

    public abstract Builder setParseDebugLevel(int i);

    public abstract Builder setTypeDebugLevel(int i);

    public abstract Builder setOptDebugLevel(int level);

    public abstract Builder setCodeGenDebugLevel(int level);

    public abstract Builder setOptimize(boolean optimize);

    public abstract Builder setExpectedErrorPhase(PhaseName phase);

    public abstract Builder setExpectedErrorMessage(String message);

    public abstract String expectedErrorMessage();

    public abstract Builder setFilename(String filename);

    abstract CompilationConfiguration autobuild();

    public CompilationConfiguration build() {
      String eem = expectedErrorMessage();
      if (eem != null && eem.length() > 0) {
        if (!eem.startsWith(".*")) {
          eem = ".*" + eem;
        }
        if (!eem.endsWith(".*")) {
          eem = eem + ".*";
        }
        eem = "(?s)" + eem;
      }
      setExpectedErrorMessage(eem);
      return autobuild();
    }
  }
}
