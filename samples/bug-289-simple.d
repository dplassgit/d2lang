DSet: record {
}

addToSet: proc(set: DSet, value: string): bool {
  return false
}

s = new DSet
i = 0
addToSet(s, 'A')
e = chr(i+asc('A'))+'B'
print "e: " println e


