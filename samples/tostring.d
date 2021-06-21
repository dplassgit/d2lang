toString: proc(i: int): string {
  if i == 0 {
    return '0'
  }
  val = ''
  while i > 0 do i = i / 10 {
    val = chr((i % 10) +asc('0')) + val
  }
  return val
}

main {
  println "Should be 314159:"
  println toString(314159)

  println "Should be 0:"
  println toString(0)

  println "Should be 300:"
  println toString(300)

  println "Should be 4:"
  println toString(4)
}
