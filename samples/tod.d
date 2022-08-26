
DS=[0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0]

// int to double.
tod: proc(i: int): double {
  neg = false
  if i < 0 {neg = true i = -i}
  d=0.0
  place = 1.0
  while i > 0 {
    last = i%10
    // I hate this
    d = d + place * DS[last]
    place = place * 10.0
    i = i / 10
  }
  if neg {return -d}
  return d
}

print "Should be 2.0:" println tod(2)
print "Should be 0.0:" println tod(0)
