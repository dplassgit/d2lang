fourParams: proc(da:int, db:string, dc:bool, dd:int): int {
  // print "da: " println da
  // print "db: " println db
  // print "dc: " println dc
  // print "dd: " println dd
  return da+dd*2
}
print "should be 9: " println fourParams(1, "fourParam", true, 4)
print "Should be 8: " println fourParams(2, "fourParam", false, 3)
