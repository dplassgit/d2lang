fun:proc():int { 
  a=1 b=2 c=3 d=4 e=5 f=6 g=3
  g=a+a*(b-c*(b+d*(e-f*(g+a)*(b-c*(b+d)*(e+f*(g-a))))))
  return g 
} 
println "Should be -86715:"
println fun()

funlong:proc():long { 
  a=10L b=20L c=30L d=40L e=50L f=60L g=30L
  h=a+a*(b-c*(b+d*(e-f*(g+a)*(b-c*(b+d)*(e+f*(g-a))))))
  return h 
} 
println "Should be -64799424605790:"
println funlong()

