package com.plasstech.lang.d2.codegen.t100;

import static com.plasstech.lang.d2.codegen.Codegen.fail;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.plasstech.lang.d2.codegen.ListEmitter;
import com.plasstech.lang.d2.codegen.t100.Subroutine.Name;

public class T100Emitter extends ListEmitter {
  // don't add the same subroutine multiple times.
  private final Map<String, List<String>> subroutines = new HashMap<>();

  @Override
  public void emitExternCall(String call) {}

  @Override
  public void emitExit(int exitCode) {
    emit("call 0x0502  ; drop back into BASIC");
    emit("hlt");
  }

  @Override
  public void emitLabel(String label) {
    if (label != null && label.length() > 0) {
      emit0("%s:", label);
    }
  }

  public void emitSubroutines() {
    subroutines.values().stream().flatMap(Collection::stream)
        .forEach(line -> emit0(line));
  }

  public void addSubroutine(Name nameEnum) {
    String name = nameEnum.name();
    if (!subroutines.containsKey(name)) {
      Subroutine sub = Subroutines.get(nameEnum);
      if (sub == null) {
        fail(null, "No code for subroutine %s", name);
      }
      subroutines.put(name, sub.code());
      // ALSO add its deps
      for (Name dep : sub.dependencies()) {
        addSubroutine(dep);
      }
    }
  }

  public void callSubroutine(Name name) {
    addSubroutine(name);
    emit("call %s", name.name());
  }
}
