r:record{a:string}
recordarray:r[2]
recordarray[1] = new r
second = recordarray[1]
second.a='hi'
//            + "println 'Should be hi' \r"
println recordarray[1].a
