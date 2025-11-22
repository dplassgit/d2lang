rt:record{i:int}
shortProcRecord:proc():rt {
  x = new rt
  x.i=3
  return x
}
r = shortProcRecord()
println r.i
