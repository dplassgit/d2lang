f:proc(b:int) {
  a:int
  a=b 
  print "Should be " print b print ":" println a
}

g:proc {
  a:string
  a=null
  print "Should be null:" println a
}

f(3)
g()
