toi: proc(d:double): int {
  neg = 1
  if d < 0.0 {neg = -1 d = -d}
  i = 0
  dplace = 100000.0
  iplace = 100000
  while iplace > 0 {
    while d > dplace {
      d = d - dplace
      i = i + iplace
    }
    iplace = iplace / 10
    dplace = dplace / 10.0
  }
  return i * neg
}

print "should be 123: " println toi(123.45)
print "should be 1: " println toi(1.45)
print "should be -5601: " println toi(-5601.45)
