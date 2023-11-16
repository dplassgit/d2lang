program = 'print "hi"'
copy = ""

i = 0 while i < length(program) do i = i + 1 {
  println program[i]
  copy = copy + program[i]
  println copy
}

p:proc(): void {
  //program:string
  program = 'print "hi"'
  //copy:string 
  copy = ""

  //i:int
  i = 0 while i < length(program) do i = i + 1 {
    println program[i]
    copy = copy + program[i]
    println copy
  }
  return
}

p()
