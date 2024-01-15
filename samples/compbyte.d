lt:proc(left:byte, right:byte):bool {
   return left < right
}

gt:proc(left:byte, right:byte):bool {
   return left > right
}

//println "Trues:"
//print "0<2: " print lt(0y00, 0y02) 
//print " -2<0: " print lt(-0y02, 0y00)
//print " -2<2: " println lt(-0y02, 0y02)
//print "2>0: " print gt(0y02, 0y00) 
//print " 0>-2: " print gt(0y00, -0y02)
//print " 2>-2: " println gt(0y02, -0y02)

//println "Falses:"
//print "2<0: " print lt(0y02, 0y00) 
//print " 0<-2: " print lt(0y00, -0y02)
//print " 2<-2: " println lt(0y02, -0y02)
//print "0>2: " print gt(0y00, 0y02) 
//print " -2>0: " print gt(-0y02, 0y00)
//print " -2>2: " println gt(-0y02, 0y02)
//
//print "2>2: " print gt(0y02, 0y02)
//print " 0>0: " print gt(0y00, 0y00)
//print " -2>-2: " println gt(-0y02, -0y02)
//print "2<2: " print lt(0y02, 0y02)
//print " 0<0: " print lt(0y00, 0y00)
//print " -2<-2: " println lt(-0y02, -0y02)

le:proc(left:byte, right:byte):bool {
   return left <= right
}

ge:proc(left:byte, right:byte):bool {
   return left >= right
}

println "Trues:"
//print "0<=2: " print le(0y00, 0y02) 
//print " -2<=0: " print le(-0y02, 0y00)
//print " -2<=2: " println le(-0y02, 0y02)
//print "2>=0: " print ge(0y02, 0y00) 
//print " 0>=-2: " print ge(0y00, -0y02)
//print " 2>=-2: " println ge(0y02, -0y02)

// these are failing
print "2>=2: " print ge(0y02, 0y02)
print " 0>=0: " print ge(0y00, 0y00)
print " -2>=-2: " println ge(-0y02, -0y02)
print "2<=2: " print le(0y02, 0y02)
print " 0<=0: " print le(0y00, 0y00)
print " -2<=-2: " println le(-0y02, -0y02)

println "Falses:"
print "2<=0: " print le(0y02, 0y00) 
print " 0<=-2: " print le(0y00, -0y02)
print " 2<=-2: " println le(0y02, -0y02)
print "0>=2: " print ge(0y00, 0y02) 
print " -2>=0: " print ge(-0y02, 0y00)
print " -2>=2: " println ge(-0y02, 0y02)
