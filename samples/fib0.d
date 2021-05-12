
n=10
n1 = 0
n2 = 1
i=1 while i < n *2 do i = i+1 {
  if (i%2)==0 {
    continue
  }
  nth = n1 + n2
  n1 = n2
  n2 = nth
  print nth
}
print nth
