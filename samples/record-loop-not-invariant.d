rt: record{i:int}

recordloopnoninvariant: proc(rec:rt): int {
  rec.i = 0
  while rec.i < 10 {
     re = rec
     re.i = re.i + 1
  }
  return rec.i
}

val = recordloopnoninvariant(new rt)
println "Should be 10:"
println val
