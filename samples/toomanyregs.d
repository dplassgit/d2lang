f2:proc(a:int, b:int, c:int, d:int): int {
 return a*a+a+3
}

f1:proc(a:int, b:int, c:int, d:int): int {
  return a+c+d+b*f2(b,a,d,c+d*a+f2(a,a,a,b+a*f2(d+a,b,a,f2(d,c,d,f2(d,c,b,a))))) +
    a+c+d+b*f2(b,a,d,c+d*a+f2(a,a,a,b+a*f2(d+a,b,a,f2(d,c,d,f2(d,c,b,a))))) +
    a+c+d+b*f2(b,a,d,c+d*a+f2(a,a,a,b+a*f2(d+a,b,a,f2(d,c,d,f2(d,c,b,a))))) +
    a+c+d+b*f2(b,a,d,c+d*a+f2(a,a,a,b+a*f2(d+a,b,a,f2(d,c,d,f2(d,c,b,a))))) +
    a+c+d+b*f2(b,a,d,c+d*a+f2(a,a,a,b+a*f2(d+a,b,a,f2(d,c,d,f2(d,c,b,a)))))
}

println "Should print 130"
print f1(1,2,3,4)

