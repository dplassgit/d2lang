manyParams: proc(da:int, db:string, dc:bool, dd:int, de:int, df: string, dg:bool, dh:int): int {
  print "da: " println da
  print "db: " println db
  print "dc: " println dc
  print "dd: " println dd
  print "de: " println de
  print "df: " println df
  print "dg: " println dg
  print "dh: " println dh
  return da+dd*2+de*3+dh*4
}
print "should be 48: " println manyParams(1, "fourParam", true, 4, 5, "many", false, 6)
print "Should be 40: " println manyParams(2, "fourParam", false, 3, 4, "many", true, 5)
