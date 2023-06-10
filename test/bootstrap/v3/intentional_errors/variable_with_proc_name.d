f:proc{}

g:proc:int {
  // Amazingly, this works in v3 but not in dcc.
  f=3
  return f
}
print "Should be 3:" println g()
