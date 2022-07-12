f2: proc(c2:bool, b2:string, d2:int, a2:int) {
  print 'c=' println c2 print 'b=' println b2 print 'a=' println a2 
}

f1: proc(a1:int, c1:bool, b1:string, d1:int) {
  x=b1 f2(c1,x,a1,d1)
}

f1(2,true, 'hi', 4)
