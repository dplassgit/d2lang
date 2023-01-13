start=3
startb=0y03
i=0y00

// cls, heh
print chr(27) print "j"
while i <= 0y05 do i++ {
  print "int " print start
  print ", byte " println startb
  start--
  startb--
}
i=0y00
while i <= 0y05 do i++ {
  print "int " print start
  print ", byte " println startb
  start++
  startb++
}
