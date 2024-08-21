a:int
a=3
b:int[]
c:int[3]
b=c
c[0]=1
print "Should be 1: "
print b[0]
print "\n"

locals:proc() {
  la:int
  la=3
  lb:int[]
  lc:int[3]
  lb=lc
  lc[0]=2
  print "Should be 2: "
  print lb[0]
  print "\n"
}

