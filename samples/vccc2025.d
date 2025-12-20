a:proc(i:int):int{if i<0{return-i}return i}d=[-1,147,293,9,19,229,97,163,5,1]r=-9 while r<10 do r++{c=-9 while c<10 do c++{print chr(32+10*(d[a(r)]>>a(c)&1))}println""}
