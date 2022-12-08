////////////////////////////////////////////////////
// SET
////////////////////////////////////////////////////
DSet: record {
  buckets:DList[29]
}

PRIMES = [2,3,5,7,11]
hashCode: proc(value: string): int {
  hash = 17 + length(value)
  i = 0 while i < length(value) do i = i + 1 {
    ch = value[i]
    hash = hash + asc(ch) * PRIMES[i%5]
  }
  return hash
}

addToSet: proc(set: DSet, value: string): bool {
  hash = hashCode(value)
  hash = hash % length(set.buckets)
  bucket = set.buckets[hash]
  if listContains(bucket, value) {
    // already there
    return true
  }
  if bucket == null {
    sb = set.buckets
    bucket = new DList
    sb[hash] = bucket
  }

  // put it in the head of the list.
  push(bucket, string2DValue(value))
  return false
}

removeFromSet: proc(set: DSet, value: string): bool {
  hash = hashCode(value)
  hash = hash % length(set.buckets)
  bucket = set.buckets[hash]
  if not listContains(bucket, value) {
    // not there
    return false
  }

  // remove it from the bucket (!)
  removeFromList(bucket, value)
  return true
}

setContains: proc(set: DSet, value:string): bool {
  hash = hashCode(value)
  hash = hash % length(set.buckets)
  bucket = set.buckets[hash]
  return listContains(bucket, value)
}

// a list of all values in the set
allSetValues: proc(set: DSet): DList {
  list = new DList
  i = 0 while i < length(set.buckets) do i = i + 1 {
    b = set.buckets[i]
    if b == null {
      continue
    }
    h = b.head while h != null do h = h.next {
      // add each one
      append(list, h.value)
    }
  }
  return list
}

printSet: proc(set: DSet) {
  i = 0 while i < length(set.buckets) do i = i + 1 {
    print "bucket[" print i print "]: "
    b = set.buckets[i]
    if b != null {
      printList(b)
    } else {
      println ""
    }
  }
}


////////////////////////////////////////////////////
// LIST
////////////////////////////////////////////////////

DValue:record { // your data here
  value:string
}
DEntry:record { value: DValue next:DEntry}
DList:record { head:DEntry }

printList: proc(list: DList) {
  tail = list.head while tail != null do tail = tail.next {
    print tail.value.value
    if tail.next != null {
      print ", "
    }
  }
  println ""
}

push: proc(list: DList, value: DValue): DList {
  e = new DEntry
  e.value = value
  e.next = list.head
  list.head = e
  return list
}

append: proc(list: DList, value: DValue): DList {
  // 1. Find tail
  // 2. Add new tail
  newtail = new DEntry
  newtail.value = value
  if list.head == null {
    // no tail
    list.head = newtail
    return list
  }

  tail = list.head while tail.next != null do tail = tail.next {
  }
  // tail is really the tail
  tail.next = newtail
  return list
}

pop: proc(list: DList): DValue {
  if list.head == null {
    exit "head of list is null"
  }
  value = list.head.value
  list.head = list.head.next
  return value
}

head: proc(list: DList): DValue {
  if list.head == null {
    exit "head of list is null"
  }
  return list.head.value
}

removeFromList: proc(list: DList, target: string):bool {
  h = list.head
  if h == null {
    return false
  }
  if h.value.value == target {
    // need to change head
    list.head = h.next
    return true
  } else {

    while h != null do h = h.next {
      next = h.next
      if next == null {
        return false
      }
      if next.value.value == target {
        // found it.
        h.next = next.next
        return true
      }
    }
  }
  return false
}

string2DValue: proc(v:string): DValue {
  dval = new DValue
  dval.value = v
  return dval
}

listContains: proc(list:DList, value: string): bool {
  if list == null {
    return false
  }
  return listEntryContains(list.head, value) 
}

listEntryContains: proc(head:DEntry, value: string): bool {
  if head == null {
    return false
  }
  if head.value.value == value {
    return true
  }
  return listEntryContains(head.next, value)
}


// 
s = new DSet
print "Should be false: " println setContains(s, 'hi')
print "Should be false: " println addToSet(s, 'hi')
printSet(s)

i = 0 while i < 26 do i = i + 1 {
  addToSet(s, chr(i+asc('a')))
  addToSet(s, chr(i+asc('A')))
  addToSet(s, chr(i+asc('A'))+'Bcde')
  addToSet(s, "A" + chr(i+asc('a')) + 'A')
  addToSet(s, "a" + chr(i+asc('A')) + 'a')
  addToSet(s, "AB" + chr(i+asc('a')) + 'BA')
  addToSet(s, "ab" + chr(i+asc('A')) + 'ba')
  addToSet(s, "BC" + chr(i+asc('a')) + 'CBA')
  addToSet(s, "bc" + chr(i+asc('A')) + 'cba')
  addToSet(s, "CDE" + chr(i+asc('a')))
  addToSet(s, "cdE" + chr(i+asc('A')))
  addToSet(s, "DEFG" + chr(i+asc('a')))
  addToSet(s, "cdefg" + chr(i+asc('A')))
}
printSet(s)
addToSet(s, 'A')
addToSet(s, 'ab')
addToSet(s, 'ab')
addToSet(s, 'zy')
addToSet(s, 'zy')
addToSet(s, 'abcdefghijklmnop')
addToSet(s, 'abcdefghijklmnop')
addToSet(s, 'qrstuvwxyzABCDFG')
addToSet(s, 'qrstuvwxyzABCDFG')

print "Should be true: " println setContains(s, 'hi')
print "Should be true: " println addToSet(s, 'hi')
printList(allSetValues(s))

printSet(s)

removeFromSet(s, 'ab')
printSet(s)
removeFromSet(s, 'r')
printSet(s)

