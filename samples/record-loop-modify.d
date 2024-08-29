Rec: record{i:int}

recordloopinvariant: proc(rec:Rec): int {
  rec.i = 0
  while rec.i < 10 {
    rec.i = rec.i + 1
  }
  return rec.i
}

val = recordloopinvariant(new Rec)
println val
