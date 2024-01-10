mult: proc(left: int, right: int): int {
  answer = 0

  // deal with negatives
  // negative_bit = false
  //MNN = 1<<31
  //if (left & MNN) == MNN {
  //  negative_bit = true
  //  left = -left // this is failing
  //}
  //if (right & MNN) == MNN {
    // flip it, so that two negatives make a positive.
  //  negative_bit = not negative_bit
    // still have to negate "right"
  //  right = -right
 // }
  
  // print "left = " print left print " right = " println right
  i = 0y00 while i < 0y20 do i++ { // and (right != 0 and left != 0) do i++ {
    if (right & 1) == 1 {
      // add
      answer += left
      print "answer is now " println answer
    }
    left = left << 1 // shift left
    right = right >> 1  // shift right
  }
  //if negative_bit {
  //  answer = -answer
  //}
  return answer
}

c=34567
d=12345
ans=c*d
//print c print "*" print d println "=:"
answer = mult(c, d)
print c print "*" print d print "=" print ans println " (opt)"
print "calculated=" println answer

//println ""

c=-23456
d=12345
ans=c*d
answer = mult(c, d)
print c print "*" print d print "=" print ans println " (opt)"
print "calculated = " println answer
