msg="hi"
voidproc:pRoc() { 
  prINt "should be hi: " PRINT msg print "\n"
}
voidproc()

callsAnother: prOc() { 
  voidproc()
}
callsAnother() 

returnsInt: PROC(): INT { 
  reTURn 3
}
val = returnsInt()
print "Should be 3: " print val print "\n"
