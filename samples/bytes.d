a=0y01
b=a+0y02
print "should be 3: " println b

f:proc(x:byte): byte {
  x = x + 0y01
  return x * 0y4
}

print "Should be 12: " println f(0y02)

