p:proc() {
  a=-18
  b=18
  //print "a=" println a
  //print "b=" println b
  //left=(a>=3) // should be flase
  //print "-18>=3 should be false: " println left
  //right=(b<3)
  //print "b<3 (18<3) should be false: " println right
  //notright = not right
  //print "not false should be true: " println notright
  //both=left or notright
  //print "false or true should be true: " println both
  //print "(false or true) should be true: " println (false or true)
  print "(false or not false) should be true: " println (false or not false)
  print "(false or not true) should be false: " println (false or not true)
  print "(true and not false) should be true: " println (true and not false)
  print "(true and not true) should be false: " println (true and not true)

  broken=(a>=3) or not (b<3) // a>=3 is -18>=3 which is false, b<3 is 18<3 which is false. false or not false = false or true = true
  print "should be true: " println broken
}

p()
