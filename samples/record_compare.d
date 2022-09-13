rt: record {s:string i:int}

a=new rt a.s='hi' a.i=3
b=new rt b.s='hi' b.i=3
print 'a==null should be false: '
println a==null
print 'a!=null should be true: '
println a!=null

print 'a==b Should be true: '
println a==b
print 'a!=b Should be false: '
println a!=b
print 'a==a Should be true: '
println a==a
print 'b==b Should be true: '
println b==b

c=a
print 'c==a Should be true: '
println c==a
print 'c==b Should be true: '
println c==b
print 'c!=b Should be false: '
println c!=b

d=new rt d.s='hi ' d.i=3
print 'a==d Should be false: '
println a==d
print 'a!=d Should be true: '
println a!=d

