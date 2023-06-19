
DS=[0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0]

// int to double.
tod: proc(i: int): double {
  neg = 1.0 
  if i < 0 {neg = -1.0 i = -i}
  d=0.0
  place = 1.0
  while i > 0 {
    last = i%10
    // I hate this
    d = d + place * DS[last]
    place = place * 10.0
    i = i / 10
  }
  return d * neg
}

print "Should be 2.0:" println tod(2)
print "Should be 1234.0:" println tod(1234)
print "Should be -23456.0:" println tod(-23456)
print "Should be 0:" println tod(0)
