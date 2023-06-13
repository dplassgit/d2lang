R1: record {
  rec2: R2
}

R2: record {
val:int
}


r = new R1
rec2 = new R2
rec2.val=3
r.rec2 = rec2
print "Should be 3:" println r.rec2.val

