buffering=0

setBuffering: proc(newval:int):int {
  print "setting buffering to " print newval print "; was " println buffering
  oldbuffering = buffering
  buffering = newval
  return oldbuffering
}

doit: proc(i:int) {
  if i < 2 {
    println "Setting buffering to 1"
    old = setBuffering(1)
    print "old = " println old
    println "Setting buffering to old"
    setBuffering(old)
    print "buffering now = " println buffering
  }
}

doit(0)


