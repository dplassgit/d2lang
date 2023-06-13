testit:proc(a:string, b:string) {
  // this was NPE'ing if one is null
  //println "\nComparing '" + a + "' and '" + b + "':"
  print "\nComparing '" print a print "' and '" print b println "':"
  print "a==b: " println a==b
  print "a!=b: " println a!=b
  print "a>b: " println a>b
  print "a<b: " println a<b
}

testit('hi', null)
testit(null, 'hi')
testit(null, null)
testit('', null)
testit(null, '')
