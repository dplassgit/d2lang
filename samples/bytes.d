a=0y01
b=a+0y02
print "should be 3: " println 0y03
print "should be -13: " println 0yf3

print "should be 3: " println b
c=b-0y2f
print "should be -44: " println c
d=a+b+b
print "should be 7: " println d

a++
print "should be 2: " println a
b--
print "should be 2: " println b

func:proc(x:byte): byte {
  x = x + 0y01
  return x * 0y04
}

e=-c
print "should be 44: " println e
f=-d
print "should be -7: " println f
g=!f
print "should be 6: " println g

print "Should be true: " println a==b
print "Should be false: " println g==b
print "Should be true: " println g!=b
print "Should be false: " println a!=b

print "Should be 12: " println func(0y02)

