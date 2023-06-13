f:proc: int {
  r1=5
  r2=r1+10
  r3=r2+20
  r4=r3+30
  r5=r4+40
  return r5
}

print "Should be 105: " println f()

g:proc: int {
  r1=1
  r2=r1+10
  r3=r2+20
  r4=r3+30
  r5=r4+40
  r6=r5+50
  r7=r6+60
  r8=r7+70
  r9=r8+80
  r10=r9+90
  r11=r10+100
  return r11
}

print "Should be 551: " println g()

