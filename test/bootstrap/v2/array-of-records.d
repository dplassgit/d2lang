RT:record {index:int}

arr:RT[5]
i=0 while i < 5 do i = i + 1 {
  r=new RT
  r.index=i*10+i+1
  arr[i]=r
}

i=0 while i < 5 do i = i + 1 {
  n=arr[i]
  print "arr[" print i print "]=" println n.index
}

