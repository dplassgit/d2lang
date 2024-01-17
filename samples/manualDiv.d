// Manually divide answer=c/d. This works, and can the basis for the assembly language version.

div:proc(num:int, denom:int): int {
  neg = False
  if num < 0  {
    num = -num
    neg = True
  }
  if denom < 0 {
    denom = -denom 
    neg = not neg
  }

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
  if neg {
    return -answer
  }
  return answer
}

c=-1234560
d=2340
opt=c/d

result = div(c, d)
print c print "/" print d print "=" print opt println " (hard-coded)"
print "calculated = " println result

c=-1234560
d=-2340
opt=c/d
result = div(c, d)
print c print "/" print d print "=" print opt println " (hard-coded)"
print "calculated = " println result


c=1234560
d=2340
opt=c/d
result = div(c, d)
print c print "/" print d print "=" print opt println " (hard-coded)"
print "calculated = " println result

