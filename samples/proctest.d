

ok:proc(): int {
  return 3
}

notok:proc(): int {
	      return 3
}

notok2:proc(): int {
  //if (true) {
	  //return 3
  //} elif (3==3) {
	  //return 4
  //} else {
	  //print 3
	  ////return 5
  //}
  if (true) {
	  if (true) {
		  print 1
			  return 3
	  } elif (3==3) {
		  print 2
		  return 3
	  } else {
		  print 3
		  return 3
	  }
  } elif (3==3) {
	  if (true) {
		  print 1
		  return 3
	  } elif (3==3) {
		  print 2
		  return 3
	  } else {
		  print 3
		  return 3
	  }
  }
  //} else {
	  //print 3
	  //return 5
  //}

}
