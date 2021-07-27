data = [2,1,4,5,20,40,1,9,100,0,8,6,98,0]

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
print_tree: proc(head: Node) {
  if (head.left != null) {
    print_tree(head.left)
  }
  println head.value
  if (head.right != null) {
    print_tree(head.right)
  }
}

print_tree(head)
