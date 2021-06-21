MAX=99999999

main {
  input = [2,1,4,8,6,98,0]
  min=MAX
  last_min = -MAX
  // 1. find next element greater than min
  j = 0 while j < length(input) do j = j + 1 {
    min=MAX
    i = 0 while i < length(input) do i = i + 1 {
      if input[i] > last_min and input[i] < min {
        min = input[i]
      }
    }
    println min
    // now need to find something greater than min
    last_min = min
  }
}
