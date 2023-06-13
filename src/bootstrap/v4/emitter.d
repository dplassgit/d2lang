///////////////////////////////////////////////////////////////////////////////
//                                    EMITTER                                //
///////////////////////////////////////////////////////////////////////////////

Emitter: record {
  head: StringListEntry  // set once
  tail: StringListEntry  // set once, but updated
}


emitter_emit0: proc(self: Emitter, s: string): StringListEntry {
  entry = new StringListEntry
  entry.value = s
  if self.head == null {
    self.head = entry
    self.tail = entry
  } else {
    // self.tail.next = entry
    tail = self.tail
    tail.next = entry
    self.tail = entry
  }
  return entry
}

emitter_emit: proc(self: Emitter, s: string): StringListEntry {
  return emitter_emit0(self, "  " + s)
}

emitter_emitLabel: proc(self: Emitter, label: string): StringListEntry {
  return emitter_emit0(self, "\n" + label + ":")
}

emitter_emitNum: proc(self: Emitter, s: string, num: int): StringListEntry {
  return emitter_emit(self, s + toString(num))
}

emitter_printEntries: proc(self: Emitter) {
  head = self.head while head != null do head = head.next {
    println head.value
  }
  println ""
}

