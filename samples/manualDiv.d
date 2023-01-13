// Manually divide answer=c/d. This works, and can the basis for the assembly language version.

c=10234000
d=243

print c print "/" print d print "=" println c/d

num = c   // we’ll be capturing its high bit and shifting this left
denom = d
answer = 0 // we’ll be shifting left and then setting the low bit to 1
remainder = 0

i=0y21 while i != 0y0 do i-- { // do one more than needed
  answer = answer << 1
  if remainder > denom {
    // shift in a 1
    answer++  // set the low bit
    remainder = remainder - denom
  //} else {
    // shift in a 0, which we did already
  }
  // get the high bit of ‘num’
  hibit = num & (1<<31) // 2^31=2147483648, but that's too big for a Java constant..
  num = num << 1
  // shift remainder left and possibly shift in a 1
  remainder = remainder << 1
  if hibit != 0 {
    // the old high bit of num was 1; "shift" it in.
    remainder++
  }
}

print "calculated = " println answer
