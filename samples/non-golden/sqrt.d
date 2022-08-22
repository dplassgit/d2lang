abs:proc(x:double):double {
  if x < 0.0 { return -x}
  return x
}

dsqrt:proc(d:double):double {
  guess = d/4.0
  x100 = 1.0/100000.0
  iters=0
  while (abs(guess*guess-d) > (x100)) {
    xguess = d/guess    
    guess = (guess + xguess)/2.0
    iters = iters + 1
    if iters > 100 {
      break
    }
  }
  
  return guess
}

print "dsqrt Should be 3.0:" println dsqrt(9.0)
print "dsqrt Should be 3.162278:" println dsqrt(10.0)
print "dsqrt Should be 153.045745:" println dsqrt(23423.0)

sqrt: extern proc(d:double):double

print "extern Should be 3.0:" println sqrt(9.0)
print "extern Should be 3.162278:" println sqrt(10.0)
print "extern Should be 153.045745:" println sqrt(23423.0)
