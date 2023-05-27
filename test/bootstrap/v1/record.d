rt:record {
  s:string
  i:int
  ii:int
  b:bool
}


gr=new rt
gr.i=123
gr.s="ess"
gr.b=true
//gr.ii=3
//gr.i=gr.ii
//print "should be ess: " println gr.s
//print "should be 3: " println gr.i
//print "should be true: " println gr.b

// This requires a record finder
f:proc(r:rt) {
  print "should be ess: " println r.s
  print "should be 123: " println r.i
  print "should be true: " println r.b
  r.s="afterf"
  r.i=321
  r.b=not r.b
}
f(gr)
//print "should be afterf: " println gr.s
//print "should be 321: " println gr.i
//print "should be false: " println gr.b

g:proc:rt {
  r = new rt
  r.i=456
  r.s="in g"
  r.b=false
  return r
}
gr=g()
print "should be in g: " println gr.s
print "should be 456: " println gr.i
print "should be false: " println gr.b
