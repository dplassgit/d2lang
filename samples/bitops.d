a=123 
b=a&64 
c=a|31
d=!a

p:proc(pa:int):int {
pb=pa&64 
pc=pa|31
pd=!pa
pa=234

println pa
println pb
println pc
println pd

pb=pa&pa 
pc=pa|pa
pd=pa+pa
pe=pa-pa
pf=pa/pa

return pa
}

println a
println b
println c
println d
p(123)
