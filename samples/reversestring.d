
reverseIter: proc(s: string): string {
  reverse: string 
  reverse = ""
  i=length(s)-1 while i >= 0 do i=i-1 {
    reverse = reverse + s[i]
  }
  return reverse
}

reverseRecursive: proc(s: string): string {
  return reverse2(length(s) ,s)
}

reverse2: proc(start: int, s: string): string {
  if start == 0 {
    return ""
  } else {
    return s[start - 1] + reverse2(start - 1, s)
  }
}

main {
  println "Recursive reverse of 'Reverse' is " + reverseRecursive("Reverse")
  println "Iterative reverse of 'Reverse' is " + reverseIter("Reverse")
}
