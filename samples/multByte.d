 left=0y05
right=0y11

glob = left * right
print "left * right: s/b 85: "
println glob
print "left * left: s/b 25: "
println left * left
print "right * right: s/b 33 (b/c of overflow): "
println right * right
print "left * 10: s/b 50: "
println left * 0y0a
print "7 * right: s/b 119: "
println 0y07 * right
print "-7 * right: s/b -119: "
println -0y07 * right
