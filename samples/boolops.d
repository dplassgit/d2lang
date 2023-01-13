a=0y3

println ""

print "3>2 should be true: " println a>0y2
print "3<2 should be false: " println a<0y2
print "3>=2 should be true: " println a>=0y2
print "3<=2 should be false: " println a<=0y2

print "3>3 should be false: " println a>0y3
print "3<3 should be false: " println a<0y3
print "3>=3 should be true: " println a>=0y3
print "3<=3 should be true: " println a<=0y3

print "3>4 should be false: " println a>0y4
print "3<4 should be true: " println a<0y4
print "3>=4 should be false: " println a>=0y4
print "3<=4 should be true: " println a<=0y4

// This helped flush out a bug in the CloudT emulator that sets
// the carry flag incorrectly for cpi 0x00
print "3>0 should be true: " println a>0y0
print "3<0 should be false: " println a<0y0
print "3>=0 should be true: " println a>=0y0
print "3<=0 should be false: " println a<=0y0
