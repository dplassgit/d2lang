_time64: extern proc(ignored:long): long
clock: extern proc(): int

last_seconds = 0L
count = 0
last_ms = 0
while count < 20 {
  seconds = _time64(0L)
  seconds_ish = 12L- (seconds % 12L)
  if seconds_ish != last_seconds {
    print seconds_ish print " "
    last_seconds = seconds_ish
    print "Clock returned " println clock()
    count++
  }
}



