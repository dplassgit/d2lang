msg="hi"
a=7

voidproc:proc() { 
  print "should be hi: " print msg print "\n"
}
voidproc()

callsAnother: proc() { 
  voidproc()
}
callsAnother() 

returnsInt: proc(): int { 
  return 3
}
val = returnsInt()
print "Should be 3: " print val print "\n"

returnsString: proc(): string { 
  return "hi\n"
}
print "Should be hi: " print returnsString()

oneParam: proc(aa:int): int { 
  print "aa: " print aa print "\n"
  return 1+aa
}
print "should be 9:" print oneParam(8) print "\n"
oneParam(9) print "\n"
twoParam: proc(ba:int, bb:string): int { 
  print "ba: " print ba print "\n"
  print "bb: " print bb print "\n"
  return 2+ba
}
print "should be 9:" print twoParam(7, 'twoparam') print "\n"
twoParam(5, 'twoparam2') print "\n"
threeParam: proc(ca:int, cb:string, cc:bool): int { 
  print "ca: " print ca print "\n"
  print "cb: " print cb print "\n"
  print "cc: " print cc print "\n"
  return 3+ca
}
print "should be 9:" print threeParam(6, 'threeparam', false) print "\n"
threeParam(3, 'threeparam2', true)
fourParam: proc(da:int, db:string, dc:bool, dd:int): int {
  print "da: " print da print "\n"
  print "db: " print db print "\n"
  print "dc: " print dc print "\n"
  print "dd: " print dd print "\n"
  return da+dd*2
}
print "should be 9:" print fourParam(1, "fourParam", true, 4) print "\n"
fourParam(2, "fourparam2", false, 5) print "\n"


local: proc(): int {
  b = returnsInt()
  c = 5
  // (7+3)*5+5 = 55
  return (a+b)*c+c
}
print "Should be 55: " print local() print "\n"

returnvoid: proc(i:int) {
  if i > 0 {
    print "Positive\n"
    return
  }
  print "Negative\n"
}
returnvoid(1)
returnvoid(-1)

