Rec: record { i: int s: string}

// just being weird
// i:string
i='hi'
// s:int
s=3

an_r = new Rec
an_r.i = s
an_r.s = i

b = an_r.i
c = an_r.s

println an_r.i
println an_r.s
println b
