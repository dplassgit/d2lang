lower:proc(s:string, x:string):string {
  out = ''
  i = 0 while i < length(s) do i = i + 1 {
    if (s[i] >= 'A' and s[i] <= 'Z') {
      x = s[i]
      xa = asc(x)
      out = out + chr(xa-(asc('A')-asc('a')))
    } else {
      out = out + s[i]
      // asc of a local, to another local
      xa = asc(out)
      print xa
    }
  }
  return out
}

println lower("Hi", '')
println lower("hI", '')
println lower("ha I", '')
