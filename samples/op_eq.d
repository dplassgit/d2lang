a=3
a+=4
if a != 7 { exit "ERROR" }
println a
a*=4
if a != 28 { exit "ERROR" }
println a
a-=4
if a != 24 { exit "ERROR" }
println a

b=6
a/=b
if a != 4 { exit "ERROR" }
println a
