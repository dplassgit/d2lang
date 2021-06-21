loc:int
loc = -1
advance:proc(this:string):string {
  loc = loc + 1
	return this[loc]
}

makeInt:proc(this:string):int  {
  value=0
  cc = 1
  x = advance(this)
  while cc >=0 & cc <=9 do x=advance(this) {
    value=value * 10 + asc(x)
  }
  return value
}

//makeInt("123")
