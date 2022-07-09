data:string[5]
data[0]="a"
data[1]="z"
data[3]="s"
data[2]="d"
data[4]="o"

len=length(data)

print_array: proc {
  i = 0 while i < len do i = i + 1 {
    print "data[" 
    print i
    println "]='" + data[i] + "'"
  }
}

println "Unsorted:"
print_array()

i = 0 while i < len - 1 do i = i + 1 {
  j = i+1 while j < len  do j = j + 1 {
    if data[i] > data[j] {
      // swap
      temp = data[i]
      data[i] = data[j]
      data[j] = temp
    }
  }
}


println "\nSorted:"
print_array()
