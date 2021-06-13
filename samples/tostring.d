NUMBERS = "0123456789"

toString: proc(i: int): string {
  if i == 0 {
    return '0'
  }
  val = ''
  while i > 0 do i = i / 10 {
    c = i % 10
    d = NUMBERS[c]
    val = d + val
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
