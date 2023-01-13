f2:proc(a:int, b:int, c:int, d:int): int {
 return a*a+a+3
}

f1:proc(a:int, b:int, c:int, d:int): int {
  x=a+c+d+b*f2(b,a,d,c+d*a+f2(a,a,a,b+a*f2(d+a,b,a,f2(d,c,d,f2(d,c,b,a))))) +
    //a+c+d+b*f2(b,a,d,c+d*a+f2(a,a,a,b+a*f2(d+a,b,a,f2(d,c,d,f2(d,c,b,a))))) +
    //a+c+d+b*f2(b,a,d,c+d*a+f2(a,a,a,b+a*f2(d+a,b,a,f2(d,c,d,f2(d,c,b,a))))) +
    //a+c+d+b*f2(b,a,d,c+d*a+f2(a,a,a,b+a*f2(d+a,b,a,f2(d,c,d,f2(d,c,b,a))))) +
    a+c+d+b*f2(b,a,d,c+d*a+f2(a,a,a,b+a*f2(d+a,b,a,f2(d,c,d,f2(d,c,b,a)))))
  return x
}

f3:proc(a:bool, b:bool, c:bool, d:bool): bool {
 return a and b or c xor d
}

f4:proc(a:bool, b:bool, c:bool, d:bool): bool {
  x =a and c or d xor b and f3(b,a,d,c or d xor a or f3(a,a,a,b or a xor f3(d and a,b,a,f3(d,c,d,f3(d,c,b,a))))) 
    //a+c+d+b*f2(b,a,d,c+d*a+f2(a,a,a,b+a*f2(d+a,b,a,f2(d,c,d,f2(d,c,b,a))))) +
    //a+c+d+b*f2(b,a,d,c+d*a+f2(a,a,a,b+a*f2(d+a,b,a,f2(d,c,d,f2(d,c,b,a))))) +
    //a+c+d+b*f2(b,a,d,c+d*a+f2(a,a,a,b+a*f2(d+a,b,a,f2(d,c,d,f2(d,c,b,a))))) +
    //a+c+d+b*f2(b,a,d,c+d*a+f2(a,a,a,b+a*f2(d+a,b,a,f2(d,c,d,f2(d,c,b,a)))))
  return x
}
print "Should print 52: "
println f1(1,2,3,4)
print "Should print true: "
println f4(true, false, true, false)

