a=3
b=4
c=5
if a==3 or b != 3 or c == 5 {
  print "Should show this\n"
}
if a==3 and b != 3 and c == 5 {
  print "Should show this too\n"
}
if a==3 and b != 3 or c == 5 or a > b{
  print "Should show this also\n"
}
