f:proc(a:int):int {
  a++
  a++
  a++
  // a = a+3
  return a
}

println f(3)

g:proc(a:int):int {
  b=a
  b++ // b = a + 1
  b=b+3  // b = a + 4
  b++  // b = a + 5 -> 9
  return b
}

println g(4)
