arr=[1,3,8]

println arr
println arr
// i wonder if this will work!
// it wlil not because the temp is deallocated after the 3rd one is assigned...
f:proc {
// this crashes at compile time
  println ['a', 'b', 'd']
}

f()
