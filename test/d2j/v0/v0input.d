a=input
print a print "\n"

p:proc():string {
  b=input
  return b 
}

q:proc():string {
  return input
}

print p() print "\n"
print q() print "\n"
