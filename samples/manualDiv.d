// Manually divide answer=c/d. This works, and can the basis for the assembly language version.

c=1234560
d=2340

print c print "/" print d print "=" print c/d println " (hard-coded, hopefully by the optimizer)"

div:proc(num:int, denom:int): int {
  answer = 0 // we’ll be shifting left and then setting the low bit to 1
  remainder = 0

  topbit=1<<31 // the optimizer does this for us
  // i is a byte to make the increment/decerement easier.
  i=0y21 while i != 0y00 do i-- { // do one more than needed
    answer = answer << 1
    if remainder > denom {
      // shift in a 1
      answer++  // set the low bit
      remainder = remainder - denom
    //} else {
      // shift in a 0, which we did already
    }
    // get the high bit of ‘num’
    hibit = num & topbit
    num = num << 1
    // shift remainder left and possibly shift in a 1
    remainder = remainder << 1
    if hibit != 0 {
      // the old high bit of num was 1; "shift" it in.
      remainder++
    }
  }
  return answer
}

result = div(c, d)
mod = c - result * d
print "calculated = " println result
print "mod= " println mod
print "check = " println result * d + mod
