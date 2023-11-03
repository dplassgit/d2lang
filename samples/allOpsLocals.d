f1:proc(a:double, b:double): double {
  i=1
  i++
//  print "i: " println i
  m=3
  m--
  // print "m: " println m
  a=2.0 
  b=3.0 
  c=-5.0 
  d=7.0 
  e=11.0 
  f=13.0 
  z=0.0 
  g=a+a*(b+(b+c*(d-(c+d/(-e+(d-e*f)+a)*b)/-c)-d))  print 'g: ' println g
  k=z+4.0/(5.0+(4.0-5.0*-f)) print 'k: ' println k
  k=0.0+-d/(5.0+(4.0-5.0*f)) print 'k: ' println k
  g=a+a*(b+(b+c*(d-(c+d/(e+(d-e*f)))))) print 'g: ' println g
  h=0.0+a+(4.0+3.0*(4.0-(3.0+4.0/(4.0+(5.0-e*6.0)))))  print 'h: ' println h
  j=a+a*(b+(b+c*(d-(c+d/(e+(d-e*f+0.0)))))) print 'j: ' println j
  aa=2.0+a*(3.0+(3.0+5.0*(7.0-(5.0+8.0/11.0)+(7.0-11.0*13.0))*2.0)/b) print "aa: " println aa
  return aa 
}
f2:proc(a:double, b:double):double { return f1(b, a) }
ans=f2(1.0, 2.0)
//println ans
