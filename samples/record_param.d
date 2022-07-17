rt: record{s:string i:int }
f:proc(x:rt) :int {i=3 x.i=i return i} 
print f(new rt)
y=new rt
print f(y)

