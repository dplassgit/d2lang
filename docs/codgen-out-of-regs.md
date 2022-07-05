# Dealing with out-of-registers

'''
pick the least recently used one (say, the first one)
reg = lruRegs.remove(0);
// don't have to deallocate, since we're just re-using the register out from underneath
// the registers object.
// push the value onto the stack
// remove the alias too.
aliases.inverse().remove(reg);
lruRegs.add(reg); // add to the end.
'''
