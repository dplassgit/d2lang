reverseIter: proc(s: string): string {
  reverse = ""
  i = length(s) - 1 while i >= 0 do i=i-1 {
    reverse = reverse + s[i]
  }
  return reverse
}

////////////////////////////////////////////////////
// Read data all at once
////////////////////////////////////////////////////
global_data = input

next_line_loc = 0
reset_input: proc() {
  next_line_loc = 0
}

// Get the next line. Returns null at EOF.
next_line: proc: String {
  line = ''
  len = length(global_data)
  while next_line_loc < len {
    ch = global_data[next_line_loc]
    next_line_loc = next_line_loc + 1
    if asc(ch) != asc('\n') {
      line = line + ch
    } else {
      return line
    }
  }
  // got to eof
  return null
}

line = next_line() while line != null {
  println reverseIter(line)
  line = next_line()
}
