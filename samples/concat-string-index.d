h="hello "
w="world"
len = length(h+w)
println 'Should be hello world:'
i = 0 while i < len do i = i + 1 {
  print ((h+w)[i])[0]
}

