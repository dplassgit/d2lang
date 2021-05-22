
advance:proc(this:string) {
}

makeInt:proc(this:string, start:string):string  {
  value=0
  cc = 1
  while cc >=0 & cc <=9 do x=advance(this) {
    value=value * 10 + cc
  }
  return "hi"
}
