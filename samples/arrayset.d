data:string[5]
data[0]="a"
data[1]="z"
data[2]="d"
data[3]="s"
data[4]="o"

len=length(data)
i = 0 while i < len do i = i + 1 {
  print "data[" 
  print i
  println "]='" + data[i] + "'"
}
