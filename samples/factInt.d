n=12 // 12! = only 479001600 but 13! is too big for 31 bits
fact=1

// cls, heh
print chr(27) print "j"
i=1 while i <= n do i++{
  fact = fact * i
  print i print "! = " println fact
}
