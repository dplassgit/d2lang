package com.plasstech.lang.d2.codegen.x64.optimize;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.plasstech.lang.d2.phase.Phase;
import com.plasstech.lang.d2.phase.State;

/**
 * Optimizes the asm code using a few simple transformations.
 */
public class NasmOptimizer implements Phase {

  private static final List<Optimizer> OPTIMIZERS = ImmutableList.of(
      new NopOptimizer(),
      new JmpOptimizer(),
      new RetJmpOptimizer(),
      new LinePatternOptimizer(),
      new AddSubOptimizer());

  @Override
  public State execute(State input) {
    ImmutableList<String> code = input.asmCode();
    boolean changed = false;
    do {
      changed = false;
      for (Optimizer opt : OPTIMIZERS) {
        code = opt.optimize(code);
        changed |= opt.isChanged();
      }
    } while (changed);

    return input.addAsmCode(code);
  }

  /** Remove empty lines and lines that are only comments. */
  private static class NopOptimizer extends Optimizer {
    @Override
    protected ImmutableList<String> doOptimize(ImmutableList<String> code) {
      return code.stream()
          // can't map to trimmed, because non-labels must start in column 2
          .filter(line -> line.trim().length() > 0)
          .filter(line -> !line.trim().startsWith(";"))
          .collect(ImmutableList.toImmutableList());
    }
  }

  /** Remove jmp label followed by the same label */
  private static class JmpOptimizer extends Optimizer {
    @Override
    protected ImmutableList<String> doOptimize(ImmutableList<String> input) {
      List<String> code = new ArrayList<String>(input);
      for (int i = 0; i < code.size() - 1; ++i) {
        String op = code.get(i);
        if (!op.trim().startsWith("jmp")) {
          continue;
        }
        String next = code.get(i + 1);
        if (!next.startsWith("_")) {
          continue;
        }
        // get the label
        String label = op.trim().substring(4) + ":";
        if (next.trim().equals(label)) {
          // kill the jmp
          code.set(i, "; " + op);
        }
      }
      return ImmutableList.copyOf(code);
    }
  }

  /** Remove a jmp after a ret */
  private static class RetJmpOptimizer extends Optimizer {
    @Override
    protected ImmutableList<String> doOptimize(ImmutableList<String> input) {
      List<String> code = new ArrayList<String>(input);
      for (int i = 0; i < code.size() - 1; ++i) {
        String op = code.get(i);
        if (!op.trim().equals("ret")) {
          continue;
        }
        String next = code.get(i + 1).trim();
        if (next.startsWith("jmp ")) {
          // kill the jmp
          code.set(i + 1, "; " + next);
        }
      }
      return ImmutableList.copyOf(code);
    }
  }

  private static final String XOR_REG = "  xor $2, $2";
  // Yeah these are a little weird, (e.g., RAL doesn't exist)
  private static final String BIG4_REGS = "([RE]?[A-D][XLH])";
  private static final String INDEX_REGS = "([RE]?[SD]IL?)";
  private static final String EXT_REGS = "([Rr][0-9][0-5]?[bwd]?)";
  private static final String REGS = String.format("(%s|%s|%s)", BIG4_REGS, INDEX_REGS, EXT_REGS);
  // I don't love this.
  private static final String MODIFIER = "([A-Z]* )?";
  private static final ImmutableMap<String, String> PATTERNS = ImmutableMap.of(
      "^  ((add|sub|mov).*) *;.*$", "  $1",
      "^  add (.*), 1 *$", "  inc $1",
      "^  sub (.*), 1 *$", "  dec $1",
      "^  mov " + MODIFIER + REGS + ", 0[ ]*$", XOR_REG);

  /**
   * Replace ADD ..., 1 with INC ... (same for sub and dec) and mov (register), 0 with xor register,
   * register, using the abov elist of patterns.
   */
  private static class LinePatternOptimizer extends Optimizer {
    @Override
    protected ImmutableList<String> doOptimize(ImmutableList<String> input) {
      return input.stream()
          .map(line -> {
            for (Map.Entry<String, String> entry : PATTERNS.entrySet()) {
              if (line.matches(entry.getKey())) {
                return line.replaceAll(entry.getKey(), entry.getValue());
              }
            }
            return line;
          })
          .collect(ImmutableList.toImmutableList());
    }
  }

  /** When add RSP, 0x20 followed by sub RSP, 0x20 is seen, both are removed. */
  private static class AddSubOptimizer extends Optimizer {
    @Override
    protected ImmutableList<String> doOptimize(ImmutableList<String> input) {
      List<String> code = new ArrayList<String>(input);
      for (int i = 0; i < code.size() - 1; ++i) {
        String op = code.get(i).trim();
        if (!op.startsWith("add RSP, 0x")) {
          continue;
        }
        String next = code.get(i + 1).trim();
        if (!next.startsWith("sub RSP, 0x")) {
          continue;
        }
        if (next.endsWith(op.substring(8))) {
          // kill both
          code.set(i, "  ; " + op);
          code.set(i + 1, "  ; " + next);
        }
      }
      return ImmutableList.copyOf(code);
    }
  }
}
