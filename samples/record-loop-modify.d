r: record{i:int}

updaterec: proc(rec:r) {
  rec.i = rec.i + 1
}

recordloopinvariant: proc(rec:r): int {
  rec.i = 0
  while rec.i < 10 {
    rec.i = rec.i + 1
  }
  return rec.i
}

val = recordloopinvariant(new r)
println val
