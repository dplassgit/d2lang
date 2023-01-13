b=0y03 
b=b << 0y02
// 3*4=12
print "b should be 12: "
println b 

b=0y03 
a=0yf5
// -11*8=-88
a=a<<b 
print "a should be -88: "
println a 

// -88/16=-6
c=a>>0y04
print "c should be -6: "
println c

// same as above
c=0ya8>>0y04
print "c should still be -6: "
println c


