lt:proc(left:int, right:int):bool {
   return left < right
}

gt:proc(left:int, right:int):bool {
   return left > right
}

println "Trues:"
print "1000<2000: " print lt(1000, 2000) 
print " -2000<1000: " print lt(-2000, 1000)
print " -2000<2000: " println lt(-2000, 2000)
print "2000>1000: " print gt(2000, 1000) 
print " 1000>-2000: " print gt(1000, -2000)
print " 2000>-2000: " println gt(2000, -2000)

println "Falses:"
print "2000<1000: " print lt(2000, 1000) 
print " 1000<-2000: " print lt(1000, -2000)
print " 2000<-2000: " println lt(2000, -2000)
print "1000>2000: " print gt(1000, 2000) 
print " -2000>1000: " print gt(-2000, 1000)
print " -2000>2000: " println gt(-2000, 2000)
