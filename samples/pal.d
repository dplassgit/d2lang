toLower: proc(s:string): string{
  c = s[0]
  if c >= 'A' and c <= 'Z' {
    return chr(asc(c) + 32)
  }
  return c
}

isPalindrome: proc(s:string): bool {
  i=0 while i < length(s) do i = i + 1 {
    if toLower(s[i]) != toLower(s[length(s)-i-1]) {
      return false
    }
  }
  return true
}

toString: proc(b:bool): string {
  if b {
    return 'true'
  } else {
    return 'false'
  }
}

tester: proc(s:string, expected:bool) {
  is = isPalindrome(s)
  if is == expected {
    println s + " is correctly " + toString(is)
  } else {
    println s + " is UNEXPECTEDLY "  + toString(is)
  }
}

main {
  tester("hello", false)
  tester("Hello", false)
  tester("racecar", true)
  tester("RaceCar", true)
  tester("madamimadam", true)
  tester("MadamIMAdam", true)
  tester("Madam, I'm Adam", false)
}


