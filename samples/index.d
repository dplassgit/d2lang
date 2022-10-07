index: proc(target: string, values:string[]): int {
  i = 0 while i < length(values) do i = i + 1 {
    if values[i] == target {
      return i
    }
  }
  return -1
}

print "Should be -1: " println index('nope', ['dope'])
print "Should be 0: " println index('nope', ['nope'])
print "Should be 1: " println index('nope', ['dope', 'nope'])
