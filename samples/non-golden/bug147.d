sqrt: extern proc(d:double):double

// fails.
asqrt: proc(d:double):double {
  return sqrt(d)
}

// this seems to work, though it's similar to the one in ge.d that fails
bsqrt: proc(d:double):double {
  f=sqrt(d)   // this local seems to be OK.
  return f
}

// this fails.
csqrt: proc(d:double):double {
  e=d // this creates another local, which seems to screw up the call
  f=sqrt(e)
  return f
}

dd:double
// this works
dsqrt: proc(d:double):double {
  dd=sqrt(d)
  return dd
}

print "axtern Should be 153.045745:" println asqrt(23423.0)
print "dxtern Should be 153.045745:" println dsqrt(23423.0)
print "cxtern Should be 153.045745:" println csqrt(23423.0)
print "bxtern Should be 153.045745:" println bsqrt(23423.0)
