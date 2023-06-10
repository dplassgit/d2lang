f:proc(s:string) {
  a=asc(s)
  println a
}
print "Should be 65: " f("A")
print "Should NPE:" f(null)

