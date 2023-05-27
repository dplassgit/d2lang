data:int[14]
data[0]=2
data[1]=1
data[2]=4
data[3]=5
data[4]=20
data[5]=40
data[6]=1
data[7]=9
data[8]=100
data[9]=0
data[10]=8
data[11]=6
data[12]=98
data[13]=0

Node: record {
  left: Node
  right: Node
  value: int
}

head = new Node
head.value = data[0]

i = 1 while i < length(data) do i = i + 1 {
  ptr = head
  v = data[i]

  while true {
    if v < ptr.value {
      // We need to go left
      if ptr.left == null {
        newleft = new Node
        newleft.value = v
        ptr.left = newleft
        break
      } else {
        ptr = ptr.left
      }
    } else {
      // We need to go right
      if ptr.right == null {
        newright = new Node
        newright.value = v
        ptr.right = newright
        break
      } else {
        ptr = ptr.right
      }
    }
  }
}

// now print them. LVR FTR
print_tree: proc(nod: Node) {
  if (nod.left != null) {
    print_tree(nod.left)
  }
  println nod.value
  if (nod.right != null) {
    print_tree(nod.right)
  }
}

print_tree(head)
