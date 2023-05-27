p: proc() {
  next = fwd(3)
  print "Should be 4:" print next print "\n"
}

fwd: proc(i:int): int {
  print "i is: " print i print "\n"
  return i + 1
}

p()
