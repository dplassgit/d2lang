p2: proc {
  println "Should print 1"
  // forward reference
  val = p1()
  print val
}

p1: proc: int {
  return 1
}

p2()
