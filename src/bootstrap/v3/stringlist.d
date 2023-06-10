StringListEntry: record {
  value: string
  index: int
  next: StringListEntry
}
StringList: record {
  head: StringListEntry
}

// Append the given string to the list. Returns the entry. If the string
// was already on the list, does not append.
append: proc(self: StringList, newvalue: string): StringListEntry {
  node = new StringListEntry
  node.value = newvalue
  if self.head == null {
    // no entries, make one
    self.head = node
    return node
  }
  head = self.head last = head while head != null do head = head.next {
    if head.value == newvalue {
      // duplicate
      return head
    }
    last = head
  }
  
  // new entry
  node.index = last.index + 1
  last.next = node
  return node
}
