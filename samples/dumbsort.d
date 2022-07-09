MAX=99999999

main {
  data:int[7]
  data[0]=2
  data[1]=1
  data[2]=4
  data[3]=8
  data[4]=6
  data[5]=98
  data[6]=0
  min=MAX
  last_min = -MAX
  // 1. find next element greater than min
  j = 0 while j < length(data) do j = j + 1 {
    min=MAX
    i = 0 while i < length(data) do i = i + 1 {
      if data[i] > last_min and data[i] < min {
        min = data[i]
      }
    }
    println min
    // now need to find something greater than min
    last_min = min
  }
}
