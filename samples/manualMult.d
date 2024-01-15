mult: proc(left: int, right: int): int {
  answer = 0

  i = 0y00 while i < 0y20 do i++ {
    if (right & 1) == 1 {
      // add
      answer += left
      //print "answer is now " println answer
    }
    left = left << 1 // shift left
    right = right >> 1  // shift right
  }
  return answer
}

c=34567
d=12345
ans=c*d
answer = mult(c, d)
print c print "*" print d print "=" print ans println " (opt)"
print "calculated=" println answer

c=-23456
d=12345
ans=c*d
answer = mult(c, d)
print c print "*" print d print "=" print ans println " (opt)"
print "calculated = " println answer

c=23456
d=-12345
ans=c*d
answer = mult(c, d)
print c print "*" print d print "=" print ans println " (opt)"
print "calculated = " println answer
