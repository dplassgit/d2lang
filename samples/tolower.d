lower:proc(s:string):string {
  out = ''
  i = 0 while i < length(s) do i = i + 1 {
    if (s[i] >= 'A' and s[i] <= 'Z') {
      x = s[i]
      xa = asc(x)
      out = out + chr(xa-(asc('A')-asc('a')))
    } else {
      out = out + s[i]
    }
  }
  return out
}

println lower("Hi")
println lower("hI")
println lower("ha I")
