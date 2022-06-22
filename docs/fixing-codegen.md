
1. symbols need to know what kind they are: local global or param.
2. params symbols need to know what index they are
3. a location gets generated from the *symbol*

THINK about removing the "SymbolStorage" enum because it's not enough information.
public enum SymbolStorage {
  GLOBAL, // known memory location
  LOCAL, // usually stack
  PARAM, // also usually stack
  REGISTER, // ?
  TEMP, // temporary, may be a register or stack
  HEAP, // dynamically stored. kind of like global.
  IMMEDIATE; // not really a storage location, but it is in a way.
}


location types:

StaticLocation (global)
ParamLocation (needs index)
LocalLocation (needs index too, for stack offset)
TempLocation (?)
HeapLocation (?) isn't this global?
ImmediateLocation (?) do we even need this? no.

Hm, do temps have symbols? yes. they're a Symbol with SymbolStorage.TEMP


EXISTING:

Node
  has Location (!!) ugh.

Operand
  ConstantOperand
  Location implements Operand (???wth)?
    FieldSetAddress
    MemoryAddress (this is used for "new" record)
    RegisterLocation (not used? no, only used in the nasm code generator)
    StackLocation
    TempLocation

Symbol has VarType
  AbstractSymbol
    ProcSymbol
    RecordSymbol (is this RecordDefinition? Or a symbol whose *type* is record?)
    VariableSymbol


PROPOSAL:

Node
oh but node needs something...
  has Location (?) GONE. Only IlCodeGenerator needed this. <<< NOT TRUE WTH

Opcodes have operands:

Operand
  ConstantOperand
  VariableOperand has Symbol and Location
    FieldSetOperand (?) this is needed for an l-value only - but seems extranous because we can calculate the memory location and then set it. but, it makes the interpreter much easier...
    TempOperand has Location


Symbol has name, VarType
  RecordDefSymbol
  ProcSymbol
  VariableSymbol has Location
    GlobalSymbol
    LocalSymbol
    FormalSymbol has index (?)

Location
  RegisterLocation (includes which register) OK
  StackLocation (includes offset)
  ParamLocation? converts to register location or stack location later ? may make the ilcodegenerator easier.
  MemoryAddress
  HeapLocation

PROBLEM:
**IL (CG?) code only uses strings for locals and params, but needs to use SYMBOLS**
