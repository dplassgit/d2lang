f2: proc(c2:bool, b2:string, d2:int, a2:int) {
  println "Should print c=true b='hi' a=4:"
  print 'c=' println c2 print 'b=' println b2 print 'a=' println a2 
}

f1: proc(a1:int, c1:bool, b1:string, d1:int) {
  // set up a temp to see how it does with registers.
  x=b1 f2(c1,x,a1,d1)
}

f1(2,true, 'hi', 4)

if2: proc(a2:int, b2:int, c2:int, d2:int) {
  println "Should print -4,1,2,3"
  println a2 println b2 println c2 println d2
}

if1: proc(a1:int, b1:int, c1:int, d1:int) {
  if2(d1,a1,b1,c1)
}

if1(1,2,3,-4)
