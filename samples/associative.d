i=1
j=2+i+3
k=3*i*5
m=1+k-1

f:proc(ii:int):int {
  jj=2+ii+3
  kk=3*jj*5
  ii=jj*3
  return ii+kk
}

print f(3) print "\n"
