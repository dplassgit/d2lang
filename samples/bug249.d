pr:proc:int {
  print 'hi'
  return 3
}
f:proc {
  x=3
  x=pr()
}
f()
