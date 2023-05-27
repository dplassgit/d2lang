rt:record { name:string index:int}

procs:rt[10]
i=0 while i < length(procs) do i = i + 1 {
  procs[i] = new rt
  p=procs[i]
  p.name='item '+chr(65+i)
  p.index=i
}


// this fails because "atom" doesn't iterate
print "Should be 'item A':" println procs[0].index
i=0 while i < length(procs) do i = i + 1 {
  p=procs[i]
  print "name: " print p.name print "; index: " println p.index
}
