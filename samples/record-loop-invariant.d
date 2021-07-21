rt: record{i:int}

updaterec: proc(re:rt) {
  re.i = re.i + 1
}

recordloopinvariant: proc(rec:rt): int {
  rec.i = 0
  while rec.i < 10 {
    updaterec(rec)
  }
  return rec.i
}

val = recordloopinvariant(new rt)
println val
