fun:proc():int { 
  a=1 b=2 c=3 d=4 e=5 f=6 g=3
  g=a+a*(b-c*(b+d*(e-f*(g+a)*(b-c*(b+d)*(e+f*(g-a))))))
  return g 
} 
println "Should be -86715:"
print fun()

