r: record { i: int s: string}

// just being weird
i='hi'
s=3

make_r: proc(): r {
  return new r
}

an_r = make_r()
an_r.i = s
an_r.s = i

b = an_r.i
c= an_r.s

println an_r.i
println an_r.s
println b
