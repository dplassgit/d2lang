count_split_parts: proc(s: string, div: string): int {
  adiv = asc(div)
  part_count = 0 

  indiv = false // are we in a divider?
  i = 0 while i < length(s) do i++ {
    ach = asc(s[i])
    if ach != adiv and indiv { 
      // we need a new one
      part_count++
    }
    indiv = ach == adiv
  }
  if not indiv {
    part_count++
  }
  return part_count
}


trim: proc(s: string): string {
  i = 0 while s[i] == ' ' and i < length(s) do i++ {}
  j = length(s) - 1 while (s[j] == ' ' or s[j] == '\n') and j >= 0 do j-- {}
  if i == 0 and j == length(s) - 1 {
    return s
  }
  out = ''
  // This isn't super efficient because D doesn't free
  k = i while k <= j do k++ {out = out + s[k]}
  return out 
}


split: proc(s: string, div: string): string[] {
  s = trim(s)
  parts: string[count_split_parts(s, div)]
  adiv = asc(div)

  part_count = 0
  indiv = false // are we in a divider?
  part = ''
  i = 0 while i < length(s) do i++ {
    ch = s[i]
    if asc(ch) != adiv {
      if indiv and part != '' {
        // we need a new one
        parts[part_count] = part
        part_count++
        part = ''
      }
      part = part + ch
    }
    indiv = asc(ch) == adiv
  }
  if part != '' {
    parts[part_count] = part
  }
  return parts
}

expectEqual:proc(actual: string[], expected: string[]) {
  print "Test case: " print actual
  if length(actual) != length(expected) {
    println ": FAIL" 
    print "Wrong lengths. Actual: " print actual print "; expected: " println expected
    return
  }
  i = 0 while i < length(actual) do i++ {
    if actual[i] != expected[i] {
      println ": FAIL" 
      print "Wrong value at index " println i
      return
    }
  }
  println ": Pass"
}


expectEqual(split("hi there", " "), ["hi", "there"])
expectEqual(split("hi e", " "), ["hi", "e"])
expectEqual(split("i e", " "), ["i", "e"])
expectEqual(split(" i e ", " "), ["i", "e"])
expectEqual(split(" leading there", " "), ["leading", "there"])
expectEqual(split(" leading trailing ", " "), ["leading", "trailing"])
expectEqual(split("  twoleading  twobetween,twotrailing  ", " "), ["twoleading", "twobetween,twotrailing"])
expectEqual(split("hi  thentwo", " "), ["hi", "thentwo"])
expectEqual(split(" leading  thentwo", " "), ["leading", "thentwo"])
expectEqual(split("hi  thentwo you", " "), ["hi", "thentwo", "you"])
