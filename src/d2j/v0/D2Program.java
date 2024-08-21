package d2j.v0;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class D2Program {
  private static boolean debug;
  private static int TOKEN_EOF;
  private static int TOKEN_PLUS;
  private static int TOKEN_MINUS;
  private static int TOKEN_MULT;
  private static int TOKEN_DIV;
  private static int TOKEN_MOD;
  private static int TOKEN_EQEQ;
  private static int TOKEN_NEQ;
  private static int TOKEN_LT;
  private static int TOKEN_GT;
  private static int TOKEN_LEQ;
  private static int TOKEN_GEQ;
  private static int TOKEN_BIT_NOT;
  private static int TOKEN_INT;
  private static int TOKEN_BOOL;
  private static int TOKEN_STRING;
  private static int TOKEN_VARIABLE;
  private static int TOKEN_EQ;
  private static int TOKEN_LPAREN;
  private static int TOKEN_RPAREN;
  private static int TOKEN_LBRACE;
  private static int TOKEN_RBRACE;
  private static int TOKEN_COLON;
  private static int TOKEN_COMMA;
  private static int TOKEN_KEYWORD;
  private static int TOKEN_LBRACKET;
  private static int TOKEN_RBRACKET;
  private static int KW_PRINT;
  private static int KW_IF;
  private static int KW_ELSE;
  private static int KW_ELIF;
  private static int KW_PROC;
  private static int KW_RETURN;
  private static int KW_WHILE;
  private static int KW_DO;
  private static int KW_BREAK;
  private static int KW_CONTINUE;
  private static int KW_INT;
  private static int KW_BOOL;
  private static int KW_STRING;
  private static int KW_NULL;
  private static int KW_INPUT;
  private static int KW_LENGTH;
  private static int KW_CHR;
  private static int KW_ASC;
  private static int KW_EXIT;
  private static int KW_AND;
  private static int KW_OR;
  private static int KW_NOT;
  private static int KW_RECORD;
  private static int KW_NEW;
  private static int KW_PRINTLN;
  private static String[] KEYWORDS;
  private static String lexerText;
  private static int lexerLoc;
  private static int lexerCc;
  private static int lexTokenType;
  private static String lexTokenString;
  private static int lexTokenInt;
  private static int lexTokenKw;
  private static boolean lexTokenBool;
  private static int TYPE_UNKNOWN;
  private static int TYPE_INT;
  private static int TYPE_BOOL;
  private static int TYPE_STRING;
  private static int TYPE_ARRAY;
  private static int TYPE_INT_ARRAY;
  private static int TYPE_BOOL_ARRAY;
  private static int TYPE_STRING_ARRAY;
  private static int TYPE_VOID;
  private static String[] D_TYPE_NAMES;
  private static String[] TYPE_NAMES;
  private static int numGlobals;
  private static int MAX_GLOBALS;
  private static String[] globalNames;
  private static int[] globalTypes;
  private static String[] globBuffer;
  private static int procBufferIndex;
  private static String[] procBuffer;
  private static int bufferIndex;
  private static String[] emitBuffer;
  private static int MAX_NUM_PROCS;
  private static int numProcs;
  private static String[] procNames;
  private static int[] returnTypes;
  private static int[] numParams;
  private static int PARAMS_PER_PROC;
  private static String[] paramNames;
  private static int[] paramTypes;
  private static int[] numLocals;
  private static int LOCALS_PER_PROC;
  private static String[] localNames;
  private static int[] localTypes;
  private static int currentProcNum;
  private static boolean needsInput;
  private static int indentSize;
  private static int numWhiles;

  private static void newLexer(String text) {
    lexerText = text;
    resetLexer();
  }


  private static void resetLexer() {
    lexerLoc = 0;
    lexerCc = 0;
    advanceLex();
  }


  private static String nextToken() {
    for (; (lexerCc == 32 || lexerCc == 10 || lexerCc == 9 || lexerCc == 13);) {
      advanceLex();
    }
    if (lexerCc != 0) {
      if (isDigit(lexerCc)) {
        return makeIntToken();
      }
      else if (isLetter(lexerCc)) {
        return makeTextToken();
      }
      else {
        return makeSymbolToken();
      }
    }
    return Token(TOKEN_EOF,"");
  }


  private static void advanceLex() {
    if (lexerLoc < lexerText.length()) {
      lexerCc = lexerText.substring(lexerLoc, lexerLoc + 1).charAt(0);
    }
    else {
      lexerCc = 0;
    }
    lexerLoc = lexerLoc + 1;
    if (debug) {
    }
  }


  private static String Token(int type, String value) {
    lexTokenType = type;
    lexTokenString = value;
    lexTokenInt = -1;
    lexTokenKw = -1;
    lexTokenBool = false;
    if (debug) {
      System.out.print("; Making token type: ");
      System.out.print(type);
      System.out.print(" value: (skipped)\n");
    }
    return "t " + toString(type) + " " + value;
  }


  private static String IntToken(int value, String valueAsString) {
    lexTokenType = TOKEN_INT;
    lexTokenString = valueAsString;
    lexTokenInt = value;
    lexTokenKw = -1;
    lexTokenBool = false;
    return "i " + valueAsString;
  }


  private static String BoolToken(boolean value, String valueAsString) {
    lexTokenType = TOKEN_BOOL;
    lexTokenString = valueAsString;
    lexTokenInt = -1;
    lexTokenKw = -1;
    lexTokenBool = value;
    return "b " + lexTokenString;
  }


  private static String KeywordToken(int value, String valueAsString) {
    lexTokenType = TOKEN_KEYWORD;
    lexTokenString = valueAsString;
    lexTokenKw = value;
    lexTokenInt = -1;
    lexTokenBool = false;
    return "k " + lexTokenString;
  }


  private static String toString(int i) {
    if (i == 0) {
      return "0";
    }
    String val;
    val = "";
    for (; i > 0; i = i / 10) {
      val = Character.toString((i % 10) + 48) + val;
    }
    return val;
  }


  private static boolean isLetter(int c) {
    return (c >= 97 && c <= 122) || (c >= 65 && c <= 90) || c == 95;
  }


  private static boolean isDigit(int c) {
    return c >= 48 && c <= 57;
  }


  private static boolean isLetterOrDigit(int c) {
    return isLetter(c) || isDigit(c);
  }


  private static String makeTextToken() {
    String value;
    value = "";
    if (isLetter(lexerCc)) {
      value = value + Character.toString(lexerCc);
      advanceLex();
    }
    for (; isLetterOrDigit(lexerCc);) {
      value = value + Character.toString(lexerCc);
      advanceLex();
    }
    if (value.compareTo("true") == 0 || value.compareTo("false") == 0) {
      return BoolToken(value.compareTo("true") == 0,value);
    }
    int i;
    i = 0;
    for (; i < KEYWORDS.length; i = i + 1) {
      if (value.compareTo(KEYWORDS[i]) == 0) {
        return KeywordToken(i,value);
      }
    }
    return Token(TOKEN_VARIABLE,value);
  }


  private static String makeIntToken() {
    int value;
    value = 0;
    String value_as_string;
    value_as_string = "";
    for (; isDigit(lexerCc); advanceLex()) {
      value = value * 10 + lexerCc - 48;
      value_as_string = value_as_string + Character.toString(lexerCc);
    }
    return IntToken(value,value_as_string);
  }


  private static String startsWithSlash() {
    advanceLex();
    if (lexerCc == 47) {
      advanceLex();
      for (; lexerCc != 10 && lexerCc != 0; advanceLex()) {
      }
      if (lexerCc != 0) {
        advanceLex();
      }
      return nextToken();
    }
    return Token(TOKEN_DIV,"/");
  }


  private static String startsWithBang() {
    advanceLex();
    if (lexerCc == 61) {
      advanceLex();
      return Token(TOKEN_NEQ,"!=");
    }
    System.out.print("Unknown character:");
    System.out.print(Character.toString(lexerCc));
    System.out.print(" ASCII code: ");
    System.out.print(lexerCc);
    System.out.print(" @ ");
    System.out.print(lexerLoc);
    System.out.print("\n");
    System.exit(-1);
    return "";
  }


  private static String startsWithGt() {
    advanceLex();
    if (lexerCc == 61) {
      advanceLex();
      return Token(TOKEN_GEQ,">=");
    }
    return Token(TOKEN_GT,">");
  }


  private static String startsWithLt() {
    advanceLex();
    if (lexerCc == 61) {
      advanceLex();
      return Token(TOKEN_LEQ,"<=");
    }
    return Token(TOKEN_LT,"<");
  }


  private static String startsWithEq() {
    advanceLex();
    if (lexerCc == 61) {
      advanceLex();
      return Token(TOKEN_EQEQ,"==");
    }
    return Token(TOKEN_EQ,"=");
  }


  private static String makeStringLiteralToken(int firstQuote) {
    advanceLex();
    String sb;
    sb = "";
    for (; lexerCc != firstQuote && lexerCc != 0;) {
      if (lexerCc == 92) {
        advanceLex();
        if (lexerCc == 110) {
          sb = sb + Character.toString(10);
        }
        else if (lexerCc == 92) {
          sb = sb + Character.toString(92);
        }
      }
      else {
        sb = sb + Character.toString(lexerCc);
      }
      advanceLex();
    }
    if (lexerCc == 0) {
      System.out.print("ERROR: Unclosed string literal ");
      System.out.print(sb);
      System.out.print("\n");
      System.out.print(" @ ");
      System.out.print(lexerLoc);
      System.out.print("\n");
      System.exit(-1);
    }
    advanceLex();
    return Token(TOKEN_STRING,sb);
  }


  private static String makeSymbolToken() {
    int oc;
    oc = lexerCc;
    if (oc == 61) {
      return startsWithEq();
    }
    else if (oc == 60) {
      return startsWithLt();
    }
    else if (oc == 62) {
      return startsWithGt();
    }
    else if (oc == 43) {
      advanceLex();
      return Token(TOKEN_PLUS,"+");
    }
    else if (oc == 45) {
      advanceLex();
      return Token(TOKEN_MINUS,"-");
    }
    else if (oc == 40) {
      advanceLex();
      return Token(TOKEN_LPAREN,"(");
    }
    else if (oc == 41) {
      advanceLex();
      return Token(TOKEN_RPAREN,")");
    }
    else if (oc == 42) {
      advanceLex();
      return Token(TOKEN_MULT,"*");
    }
    else if (oc == 47) {
      return startsWithSlash();
    }
    else if (oc == 37) {
      advanceLex();
      return Token(TOKEN_MOD,"%");
    }
    else if (oc == 33) {
      return startsWithBang();
    }
    else if (oc == 123) {
      advanceLex();
      return Token(TOKEN_LBRACE,"{");
    }
    else if (oc == 125) {
      advanceLex();
      return Token(TOKEN_RBRACE,"}");
    }
    else if (oc == 91) {
      advanceLex();
      return Token(TOKEN_LBRACKET,"[");
    }
    else if (oc == 93) {
      advanceLex();
      return Token(TOKEN_RBRACKET,"]");
    }
    else if (oc == 58) {
      advanceLex();
      return Token(TOKEN_COLON,":");
    }
    else if (oc == 34 || oc == 39) {
      return makeStringLiteralToken(oc);
    }
    else if (oc == 44) {
      advanceLex();
      return Token(TOKEN_COMMA,",");
    }
    System.out.print("ERROR: Unknown character:");
    System.out.print(Character.toString(lexerCc));
    System.out.print(" ASCII code: ");
    System.out.print(lexerCc);
    System.out.print(" @ ");
    System.out.print(lexerLoc);
    System.out.print("\n");
    System.exit(-1);
    return "";
  }


  private static void printToken() {
    if (lexTokenType == TOKEN_EOF) {
      System.out.print("Token: EOF");
      System.out.print("\n");
    }
    else if (lexTokenType == TOKEN_INT) {
      System.out.print("Int token: ");
      System.out.print(lexTokenInt);
      System.out.print("\n");
    }
    else if (lexTokenType == TOKEN_STRING) {
      System.out.print("String token: \"");
      System.out.print(lexTokenString);
      System.out.print("\"\n");
    }
    else if (lexTokenType == TOKEN_BOOL) {
      if (lexTokenBool) {
        System.out.print("Bool token: true\n");
      }
      else {
        System.out.print("Bool token: false\n");
      }
    }
    else if (lexTokenType == TOKEN_KEYWORD) {
      System.out.print("Keyword token: ");
      System.out.print(lexTokenString);
      System.out.print("\n");
    }
    else if (lexTokenType == TOKEN_VARIABLE) {
      System.out.print("Variable: ");
      System.out.print(lexTokenString);
      System.out.print("\n");
    }
    else {
      System.out.print("Token: ");
      System.out.print(lexTokenString);
      System.out.print(" type: ");
      System.out.print(lexTokenType);
      System.out.print("\n");
    }
  }


  private static void advanceParser() {
    nextToken();
    if (debug) {
    }
  }


  private static void expectToken(int expectedTokenType, String tokenStr) {
    if (lexTokenType != expectedTokenType) {
      System.out.print("ERROR: expected '" + tokenStr + "' but found: ");
      printToken();
      System.out.print("@ ");
      System.out.print(lexerLoc);
      System.exit(-1);
    }
    advanceParser();
  }


  private static void expectKeyword(int expectedKwType, String tokenStr) {
    if (lexTokenType != TOKEN_KEYWORD || lexTokenKw != expectedKwType) {
      System.out.print("ERROR: expected '");
      System.out.print(tokenStr);
      System.out.print("' but found: ");
      printToken();
      System.out.print("@ ");
      System.out.print(lexerLoc);
      System.exit(-1);
    }
    advanceParser();
  }


  private static void registerGlobal(String name, int type) {
    if (type == TYPE_UNKNOWN) {
      System.out.print("ERROR: Cannot register global '");
      System.out.print(name);
      System.out.print("' with unknown type\n");
      System.out.print(" @ ");
      System.out.print(lexerLoc);
      System.out.print("\n");
      System.exit(-1);
    }
    int i;
    i = 0;
    for (; i < numGlobals; i = i + 1) {
      if (globalNames[i].compareTo(name) == 0) {
        return;
      }
    }
    if (debug) {
      System.out.print("// Adding global name " + name);
      System.out.print("\n");
    }
    globalNames[numGlobals]=name;
    globalTypes[numGlobals]=type;
    numGlobals = numGlobals + 1;
  }


  private static int lookupGlobal(String name) {
    int i;
    i = 0;
    for (; i < numGlobals; i = i + 1) {
      if (globalNames[i].compareTo(name) == 0) {
        return globalTypes[i];
      }
    }
    return TYPE_UNKNOWN;
  }


  private static void emit(String line) {
    emitBuffer[bufferIndex]=line;
    bufferIndex = bufferIndex + 1;
  }


  private static void spoolBuffer(String[] buffer, int start, int end) {
    int i;
    i = start;
    for (; i < end; i = i + 1) {
      emit(buffer[i]);
    }
  }


  private static void registerProc(String name, int returnType) {
    if (returnType == TYPE_UNKNOWN) {
      System.out.print("ERROR: Cannot have unknown proc return type\n");
      System.out.print(" @ ");
      System.out.print(lexerLoc);
      System.out.print("\n");
      System.exit(-1);
    }
    procNames[numProcs]=name;
    returnTypes[numProcs]=returnType;
    numProcs = numProcs + 1;
  }


  private static void setCurrentProcNum(String name) {
    int i;
    i = 0;
    for (; i < numProcs; i = i + 1) {
      if (procNames[i].compareTo(name) == 0) {
        currentProcNum = i;
        return;
      }
    }
    System.out.print("ERROR: Cannot set current proc num for proc '");
    System.out.print(name);
    System.out.print("'\n");
    System.out.print(" @ ");
    System.out.print(lexerLoc);
    System.out.print("\n");
    System.exit(-1);
  }


  private static int lookupProcReturnType(String name) {
    int i;
    i = 0;
    for (; i < numProcs; i = i + 1) {
      if (name.compareTo(procNames[i]) == 0) {
        return returnTypes[i];
      }
    }
    System.out.print("ERROR: Cannot find proc '");
    System.out.print(name);
    System.out.print("'\n");
    System.out.print(" @ ");
    System.out.print(lexerLoc);
    System.out.print("\n");
    System.exit(-1);
    return -1;
  }


  private static int lookupParam(String name) {
    if (currentProcNum == -1) {
      System.out.print("ERROR: Cannot lookup parameter ");
      System.out.print(name);
      System.out.print(" because not in a proc");
      System.out.print(" @ ");
      System.out.print(lexerLoc);
      System.out.print("\n");
      System.exit(-1);
      return -1;
    }
    int base;
    base = currentProcNum * PARAMS_PER_PROC;
    int i;
    i = 0;
    for (; i < numParams[currentProcNum]; i = i + 1) {
      if (paramNames[base].compareTo(name) == 0) {
        return base;
      }
      base = base + 1;
    }
    return -1;
  }


  private static int lookupLocal(String name) {
    if (currentProcNum == -1) {
      System.out.print("ERROR: Cannot lookup local ");
      System.out.print(name);
      System.out.print(" because not in a proc");
      System.out.print(" @ ");
      System.out.print(lexerLoc);
      System.out.print("\n");
      System.exit(-1);
    }
    int base;
    base = currentProcNum * 10;
    int i;
    i = 0;
    for (; i < numLocals[currentProcNum]; i = i + 1) {
      if (localNames[base].compareTo(name) == 0) {
        return base;
      }
      base = base + 1;
    }
    return -1;
  }


  private static int expr() {
    return boolOr();
  }


  private static int boolOr() {
    int leftType;
    leftType = boolAnd();
    if (lexTokenType == TOKEN_KEYWORD && leftType == TYPE_BOOL) {
      for (; lexTokenKw == KW_OR;) {
        advanceParser();
        emit(" || ");
        boolAnd();
      }
    }
    return leftType;
  }


  private static int boolAnd() {
    int leftType;
    leftType = compare();
    if (lexTokenType == TOKEN_KEYWORD && leftType == TYPE_BOOL) {
      for (; lexTokenKw == KW_AND;) {
        advanceParser();
        emit(" && ");
        compare();
      }
    }
    return leftType;
  }


  private static int compare() {
    int leftType;
    leftType = addSub();
    String opstring;
    opstring = lexTokenString;
    if (leftType == TYPE_INT && (lexTokenType >= TOKEN_EQEQ && lexTokenType <= TOKEN_GEQ)) {
      advanceParser();
      emit(" ");
      emit(opstring);
      emit(" ");
      addSub();
      return TYPE_BOOL;
    }
    if (leftType == TYPE_STRING && (lexTokenType == TOKEN_EQEQ || lexTokenType == TOKEN_NEQ)) {
      advanceParser();
      String[] addSubBuffer;
      addSubBuffer = new String[100];
      String[] oldEmitBuffer;
      oldEmitBuffer = emitBuffer;
      emitBuffer = addSubBuffer;
      int oldBufferIndex;
      oldBufferIndex = bufferIndex;
      bufferIndex = 0;
      int rightType;
      rightType = addSub();
      int count;
      count = bufferIndex;
      emitBuffer = oldEmitBuffer;
      bufferIndex = oldBufferIndex;
      if (rightType == TYPE_VOID) {
        emit(" ");
        emit(opstring);
        emit(" null");
      }
      else {
        emit(".compareTo(");
        spoolBuffer(addSubBuffer,0,count);
        emit(") ");
        emit(opstring);
        emit(" 0");
      }
      return TYPE_BOOL;
    }
    return leftType;
  }


  private static int addSub() {
    int leftType;
    leftType = mulDiv();
    for (; lexTokenType == TOKEN_PLUS || lexTokenType == TOKEN_MINUS;) {
      String opstring;
      opstring = lexTokenString;
      advanceParser();
      emit(" ");
      emit(opstring);
      emit(" ");
      int rightType;
      rightType = mulDiv();
      if (leftType != rightType) {
        System.out.print("ERROR: Type mismatch. Left operand is ");
        System.out.print(TYPE_NAMES[leftType]);
        System.out.print(", but right operand is ");
        System.out.print(TYPE_NAMES[rightType]);
        System.out.print("\n");
        System.out.print("@ ");
        System.out.print(lexerLoc);
        System.out.print("\n");
        System.exit(-1);
      }
      if (leftType == TYPE_BOOL) {
        System.out.print("ERROR: Cannot add or subtract booleans\n");
        System.out.print("@ ");
        System.out.print(lexerLoc);
        System.out.print("\n");
        System.exit(-1);
      }
      if (leftType == TYPE_STRING && lexTokenType == TOKEN_MINUS) {
        System.out.print("ERROR: Cannot subtract strings\n");
        System.out.print("@ ");
        System.out.print(lexerLoc);
        System.out.print("\n");
        System.exit(-1);
      }
    }
    return leftType;
  }


  private static int mulDiv() {
    int leftType;
    leftType = unary();
    for (; leftType == TYPE_INT && (lexTokenType == TOKEN_MULT || lexTokenType == TOKEN_DIV || lexTokenType == TOKEN_MOD);) {
      String opstring;
      opstring = lexTokenString;
      advanceParser();
      emit(" ");
      emit(opstring);
      emit(" ");
      unary();
    }
    return leftType;
  }


  private static int unary() {
    int type;
        if (lexTokenType == TOKEN_PLUS) {
      advanceParser();
      return unary();
    }
    else if (lexTokenType == TOKEN_MINUS) {
      advanceParser();
      String[] unaryBuffer;
      unaryBuffer = new String[100];
      String[] oldEmitBuffer;
      oldEmitBuffer = emitBuffer;
      emitBuffer = unaryBuffer;
      int oldBufferIndex;
      oldBufferIndex = bufferIndex;
      bufferIndex = 0;
      type = unary();
      int count;
      count = bufferIndex;
      emitBuffer = oldEmitBuffer;
      bufferIndex = oldBufferIndex;
      if (type == TYPE_INT) {
        emit("-");
        spoolBuffer(unaryBuffer,0,count);
        return type;
      }
      System.out.print("ERROR: cannot codegen negative non-ints yet\n");
      System.out.print("@ ");
      System.out.print(lexerLoc);
      System.out.print("\n");
      System.exit(-1);
    }
    else if (lexTokenType == TOKEN_KEYWORD && lexTokenKw == KW_LENGTH) {
      advanceParser();
      expectToken(TOKEN_LPAREN,"(");
      type = expr();
      expectToken(TOKEN_RPAREN,")");
      if (type == TYPE_STRING) {
        emit(".length()");
      }
      else if (isArrayType(type)) {
        emit(".length");
      }
      else {
        System.out.print("ERROR: Cannot take LENGTH of ");
        System.out.print(TYPE_NAMES[type]);
        System.out.print("\n");
        System.out.print(" @ ");
        System.out.print(lexerLoc);
        System.out.print("\n");
        System.exit(-1);
      }
      return TYPE_INT;
    }
    else if (lexTokenType == TOKEN_KEYWORD && lexTokenKw == KW_ASC) {
      advanceParser();
      expectToken(TOKEN_LPAREN,"(");
      type = expr();
      emit(".charAt(0)");
      expectToken(TOKEN_RPAREN,")");
      if (type != TYPE_STRING) {
        System.out.print("ERROR: Cannot take ASC of ");
        System.out.print(TYPE_NAMES[type]);
        System.out.print("\n");
        System.out.print(" @ ");
        System.out.print(lexerLoc);
        System.out.print("\n");
        System.exit(-1);
      }
      return TYPE_INT;
    }
    else if (lexTokenType == TOKEN_KEYWORD && lexTokenKw == KW_CHR) {
      advanceParser();
      expectToken(TOKEN_LPAREN,"(");
      emit("Character.toString(");
      type = expr();
      emit(")");
      expectToken(TOKEN_RPAREN,")");
      if (type != TYPE_INT) {
        System.out.print("ERROR: Cannot take CHR of ");
        System.out.print(TYPE_NAMES[type]);
        System.out.print("\n");
        System.out.print(" @ ");
        System.out.print(lexerLoc);
        System.out.print("\n");
        System.exit(-1);
      }
      return TYPE_STRING;
    }
    return composite();
  }


  private static boolean isArrayType(int type) {
    return type > TYPE_ARRAY && type <= TYPE_STRING_ARRAY;
  }


  private static int toBaseType(int arrayType) {
    return arrayType - TYPE_ARRAY;
  }


  private static int generateArrayIndex(int arrayType) {
    int baseType;
    baseType = toBaseType(arrayType);
    emit("[");
    int indexType;
    indexType = expr();
    if (indexType != TYPE_INT) {
      System.out.print("ERROR: Array index must be int; was ");
      System.out.print(TYPE_NAMES[indexType]);
      System.out.print("\n");
      System.out.print(" @ ");
      System.out.print(lexerLoc);
      System.out.print("\n");
      System.exit(-1);
    }
    expectToken(TOKEN_RBRACKET,"]");
    emit("]");
    return baseType;
  }


  private static void generateStringIndex() {
    emit(".substring(");
    String[] exprBuffer;
    exprBuffer = new String[100];
    String[] oldEmitBuffer;
    oldEmitBuffer = emitBuffer;
    emitBuffer = exprBuffer;
    int oldBufferIndex;
    oldBufferIndex = bufferIndex;
    bufferIndex = 0;
    int indexType;
    indexType = expr();
    expectToken(TOKEN_RBRACKET,"]");
    int count;
    count = bufferIndex;
    emitBuffer = oldEmitBuffer;
    bufferIndex = oldBufferIndex;
    if (indexType != TYPE_INT) {
      System.out.print("ERROR: String index must be int; was ");
      System.out.print(TYPE_NAMES[indexType]);
      System.out.print("\n");
      System.out.print(" @ ");
      System.out.print(lexerLoc);
      System.out.print("\n");
      System.exit(-1);
    }
    spoolBuffer(exprBuffer,0,count);
    emit(", ");
    spoolBuffer(exprBuffer,0,count);
    emit(" + 1)");
  }


  private static int composite() {
    int leftType;
    leftType = atom();
    if (lexTokenType == TOKEN_LBRACKET) {
      expectToken(TOKEN_LBRACKET,"[");
      if (isArrayType(leftType)) {
        return generateArrayIndex(leftType);
      }
      else if (leftType == TYPE_STRING) {
        generateStringIndex();
        return leftType;
      }
      System.out.print("ERROR: Cannot take index of ");
      System.out.print(TYPE_NAMES[leftType]);
      System.out.print("\n");
      System.out.print(" @ ");
      System.out.print(lexerLoc);
      System.out.print("\n");
      System.exit(-1);
    }
    return leftType;
  }


  private static int generateGetVariable(String variable) {
    int varType;
    varType = lookupGlobal(variable);
    if (varType != TYPE_UNKNOWN) {
      emit(variable);
      return varType;
    }
    if (currentProcNum == -1) {
      System.out.print("ERROR: Cannot find global variable ");
      System.out.print(variable);
      System.out.print("\n");
      System.out.print(" @ ");
      System.out.print(lexerLoc);
      System.out.print("\n");
      System.exit(-1);
    }
    int index;
    index = lookupLocal(variable);
    if (index != -1) {
      emit(variable);
      return localTypes[index];
    }
    index = lookupParam(variable);
    if (index == -1) {
      System.out.print("ERROR: Cannot find param ");
      System.out.print(variable);
      System.out.print("\n");
      System.out.print(" @ ");
      System.out.print(lexerLoc);
      System.out.print("\n");
      System.exit(-1);
    }
    emit(variable);
    return paramTypes[index];
  }


  private static void generateProcCall(String procname) {
    emit(procname);
    expectToken(TOKEN_LPAREN,"(");
    emit("(");
    int numArgs;
    numArgs = 0;
    for (; lexTokenType != TOKEN_RPAREN && lexTokenType != TOKEN_EOF;) {
      numArgs = numArgs + 1;
      int argType;
      argType = expr();
      if (lexTokenType == TOKEN_COMMA) {
        emit(",");
        advanceParser();
      }
    }
    expectToken(TOKEN_RPAREN,")");
    emit(")");
  }


  private static void generateInput() {
    emit("__d2_input()");
    needsInput = true;
  }


  private static void outputInput() {
    System.out.print("\n  private static String __d2_input() {\n    String input = \"\";\n    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));\n    try {\n      String line = reader.readLine();\n      while (line != null) {\n        input += line + \"\\n\";\n        line = reader.readLine();\n      }\n    } catch (IOException e) {\n      throw new RuntimeException(\"Could not read standard in\", e);\n    }\n    return input;\n  }\n");
  }


  private static int atom() {
    if (lexTokenType == TOKEN_STRING) {
      emit("\"");
      String s;
      s = lexTokenString;
      int i;
      i = 0;
      for (; i < s.length(); i = i + 1) {
        String c;
        c = s.substring(i, i + 1);
        if (c.compareTo("\n") == 0) {
          emit("\\n");
          continue;
        }
        else if (c.compareTo("") == 0) {
          emit("\\r");
          continue;
        }
        else if (c.compareTo("\"") == 0) {
          emit("\\\"");
          continue;
        }
        else if (c.compareTo("\\") == 0) {
          emit("\\\\");
          continue;
        }
        emit(c);
      }
      emit("\"");
      advanceParser();
      return TYPE_STRING;
    }
    else if (lexTokenType == TOKEN_INT) {
      String intval;
      intval = lexTokenString;
      advanceParser();
      emit(intval);
      return TYPE_INT;
    }
    else if (lexTokenType == TOKEN_BOOL) {
      boolean boolval;
      boolval = lexTokenBool;
      advanceParser();
      if (boolval) {
        emit("true");
      }
      else {
        emit("false");
      }
      return TYPE_BOOL;
    }
    else if (lexTokenType == TOKEN_VARIABLE) {
      String variable;
      variable = lexTokenString;
      advanceParser();
      if (lexTokenType != TOKEN_LPAREN) {
        int varType;
        varType = generateGetVariable(variable);
        return varType;
      }
      generateProcCall(variable);
      int type;
      type = lookupProcReturnType(variable);
      if (type == TYPE_VOID) {
        System.out.print("ERROR: Return type of ");
        System.out.print(variable);
        System.out.print(" is void. Cannot assign it to a variable.\n");
        System.out.print(" @ ");
        System.out.print(lexerLoc);
        System.out.print("\n");
        System.exit(-1);
      }
      return type;
    }
    else if (lexTokenType == TOKEN_LPAREN) {
      expectToken(TOKEN_LPAREN,"(");
      emit("(");
      int exprType;
      exprType = expr();
      expectToken(TOKEN_RPAREN,")");
      emit(")");
      return exprType;
    }
    else if (lexTokenType == TOKEN_KEYWORD && lexTokenKw == KW_INPUT) {
      advanceParser();
      generateInput();
      return TYPE_STRING;
    }
    else if (lexTokenType == TOKEN_KEYWORD && lexTokenKw == KW_NULL) {
      advanceParser();
      emit("null");
      return TYPE_VOID;
    }
    System.out.print("ERROR: cannot parse token in atom(): ");
    printToken();
    System.out.print(" @ ");
    System.out.print(lexerLoc);
    System.out.print("\n");
    System.exit(-1);
    return -1;
  }


  private static int parseType() {
    int i;
    i = 1;
    for (; i <= 3; i = i + 1) {
      if (D_TYPE_NAMES[i].compareTo(lexTokenString) == 0) {
        advanceParser();
        return i;
      }
    }
    System.out.print("ERROR: Unknown type ");
    printToken();
    System.out.print(" @ ");
    System.out.print(lexerLoc);
    System.out.print("\n");
    System.exit(-1);
    return -1;
  }


  private static void parseVarDecl(String variable) {
    int baseType;
    baseType = parseType();
    if (lexTokenType != TOKEN_LBRACKET) {
      registerOrLookUpVariable(variable,baseType);
      return;
    }
    int arrayType;
    arrayType = baseType + TYPE_ARRAY;
    expectToken(TOKEN_LBRACKET,"[");
    if (lexTokenType == TOKEN_RBRACKET) {
      expectToken(TOKEN_RBRACKET,"]");
      registerOrLookUpVariable(variable,arrayType);
      return;
    }
    String[] exprBuffer;
    exprBuffer = new String[100];
    String[] oldEmitBuffer;
    oldEmitBuffer = emitBuffer;
    emitBuffer = exprBuffer;
    int oldBufferIndex;
    oldBufferIndex = bufferIndex;
    bufferIndex = 0;
    int sizeType;
    sizeType = expr();
    int count;
    count = bufferIndex;
    emitBuffer = oldEmitBuffer;
    bufferIndex = oldBufferIndex;
    if (sizeType != TYPE_INT) {
      System.out.print("ERROR: Array size must be an int, but was ");
      System.out.print(TYPE_NAMES[sizeType]);
      System.out.print("\n");
      System.out.print(" @ ");
      System.out.print(lexerLoc);
      System.out.print("\n");
      System.exit(-1);
    }
    expectToken(TOKEN_RBRACKET,"]");
    registerOrLookUpVariable(variable,arrayType);
    emit(variable);
    emit(" = new ");
    emit(TYPE_NAMES[baseType]);
    emit("[");
    spoolBuffer(exprBuffer,0,count);
    emit("];\n");
  }


  private static void parseProc(String procName) {
    expectKeyword(KW_PROC,"PROC");
    if (currentProcNum != -1) {
      System.out.print("ERROR: cannot define nested procs\n");
      System.out.print(" @ ");
      System.out.print(lexerLoc);
      System.out.print("\n");
      System.exit(-1);
    }
    setCurrentProcNum(procName);
    expectToken(TOKEN_LPAREN,"(");
    for (; lexTokenType != TOKEN_RPAREN;) {
      expectToken(TOKEN_VARIABLE,"variable");
      expectToken(TOKEN_COLON,":");
      parseType();
      if (lexTokenType == TOKEN_LBRACKET) {
        expectToken(TOKEN_LBRACKET,"[");
        expectToken(TOKEN_RBRACKET,"]");
      }
      if (lexTokenType == TOKEN_COMMA) {
        advanceParser();
      }
      else {
        break;
      }
    }
    expectToken(TOKEN_RPAREN,")");
    int oldBufferIndex;
    oldBufferIndex = bufferIndex;
    String[] oldBuffer;
    oldBuffer = emitBuffer;
    emitBuffer = procBuffer;
    bufferIndex = procBufferIndex;
    int returnType;
    returnType = TYPE_VOID;
    if (lexTokenType == TOKEN_COLON) {
      advanceParser();
      returnType = parseType();
    }
    emit("\n  private static ");
    emit(TYPE_NAMES[returnType]);
    emit(" ");
    emit(procName);
    emit("(");
    int i;
    i = 0;
    for (; i < numParams[currentProcNum]; i = i + 1) {
      emit(TYPE_NAMES[paramTypes[currentProcNum * 4 + i]]);
      emit(" ");
      emit(paramNames[currentProcNum * 4 + i]);
      if (i < numParams[currentProcNum] - 1) {
        emit(", ");
      }
    }
    emit(") ");
    parseBlock(true);
    currentProcNum = -1;
    emit("\n");
    procBufferIndex = bufferIndex;
    emitBuffer = oldBuffer;
    bufferIndex = oldBufferIndex;
  }


  private static void parseProcSignature(String procName) {
    expectToken(TOKEN_LPAREN,"(");
    int myProcNum;
    myProcNum = numProcs;
    int paramIndex;
    paramIndex = myProcNum * PARAMS_PER_PROC;
    int index;
    index = 0;
    for (; lexTokenType != TOKEN_RPAREN;) {
      if (lexTokenType != TOKEN_VARIABLE) {
        System.out.print("ERROR: expected variable but found: ");
        printToken();
        System.out.print(" @ ");
        System.out.print(lexerLoc);
        System.out.print("\n");
        System.exit(-1);
      }
      if (numParams[myProcNum] == PARAMS_PER_PROC) {
        System.out.print("ERROR: More than 4 parameters declared for proc ");
        System.out.print(procName);
        System.out.print("\n");
        System.out.print(" @ ");
        System.out.print(lexerLoc);
        System.out.print("\n");
        System.exit(-1);
      }
      String paramName;
      paramName = lexTokenString;
      advanceParser();
      expectToken(TOKEN_COLON,":");
      int type;
      type = parseType();
      if (lexTokenType == TOKEN_LBRACKET) {
        expectToken(TOKEN_LBRACKET,"[");
        expectToken(TOKEN_RBRACKET,"]");
        type = type + TYPE_ARRAY;
      }
      paramNames[paramIndex]=paramName;
      paramTypes[paramIndex]=type;
      paramIndex = paramIndex + 1;
      index = index + 1;
      numParams[myProcNum]=numParams[myProcNum] + 1;
      if (lexTokenType == TOKEN_COMMA) {
        advanceParser();
      }
      else {
        break;
      }
    }
    expectToken(TOKEN_RPAREN,")");
    int returnType;
    returnType = TYPE_VOID;
    if (lexTokenType == TOKEN_COLON) {
      advanceParser();
      returnType = parseType();
    }
    registerProc(procName,returnType);
  }


  private static boolean isAtStartOfExpression() {
    if (lexTokenType == TOKEN_KEYWORD) {
      return lexTokenKw == KW_ASC || lexTokenKw == KW_CHR || lexTokenKw == KW_INPUT || lexTokenKw == KW_LENGTH || lexTokenKw == KW_NEW || lexTokenKw == KW_NOT;
    }
    return lexTokenType == TOKEN_INT || lexTokenType == TOKEN_BOOL || lexTokenType == TOKEN_STRING || lexTokenType == TOKEN_BIT_NOT || lexTokenType == TOKEN_LPAREN || lexTokenType == TOKEN_MINUS || lexTokenType == TOKEN_PLUS || lexTokenType == TOKEN_VARIABLE;
  }


  private static void parseReturn() {
    if (currentProcNum == -1) {
      System.out.print("ERROR: Cannot return outside proc\n");
      System.out.print(" @ ");
      System.out.print(lexerLoc);
      System.out.print("\n");
      System.exit(-1);
    }
    String currentProcName;
    currentProcName = procNames[currentProcNum];
    emit("return");
    if (isAtStartOfExpression()) {
      emit(" ");
      int actualType;
      actualType = expr();
      int expectedType;
      expectedType = returnTypes[currentProcNum];
      if (actualType != expectedType) {
        System.out.print("ERROR: Incorrect return type of '");
        System.out.print(currentProcName);
        System.out.print("'. Expected ");
        System.out.print(TYPE_NAMES[expectedType]);
        System.out.print(" but found ");
        System.out.print(TYPE_NAMES[actualType]);
        System.out.print("\n");
        System.out.print(" @ ");
        System.out.print(lexerLoc);
        System.out.print("\n");
        System.exit(-1);
      }
    }
    emit(";\n");
  }


  private static void registerLocal(String name, int type) {
    if (type == TYPE_UNKNOWN) {
      System.out.print("ERROR: Cannot register local '");
      System.out.print(name);
      System.out.print("' with unknown type\n");
      System.out.print(" @ ");
      System.out.print(lexerLoc);
      System.out.print("\n");
      System.exit(-1);
    }
    int myLocalCount;
    myLocalCount = numLocals[currentProcNum];
    if (myLocalCount == LOCALS_PER_PROC) {
      System.out.print("ERROR: Too many locals. Max is ");
      System.out.print(LOCALS_PER_PROC);
      System.out.print("\n");
      System.out.print(" @ ");
      System.out.print(lexerLoc);
      System.out.print("\n");
      System.exit(-1);
    }
    int base;
    base = currentProcNum * LOCALS_PER_PROC + myLocalCount;
    localNames[base]=name;
    localTypes[base]=type;
    numLocals[currentProcNum]=myLocalCount + 1;
  }


  private static int registerOrLookUpVariable(String variable, int exprType) {
    int varType;
    varType = lookupGlobal(variable);
    boolean isGlobal;
    isGlobal = varType != TYPE_UNKNOWN || currentProcNum == -1;
    if (isGlobal) {
      if (varType == TYPE_UNKNOWN) {
        registerGlobal(variable,exprType);
        varType = exprType;
        System.out.print(("  private static "));
        System.out.print((TYPE_NAMES[varType]));
        System.out.print((" "));
        System.out.print((variable));
        System.out.print(";\n");
        indent();
      }
      return varType;
    }
    int index;
    index = lookupParam(variable);
    if (index != -1) {
      varType = paramTypes[index];
      return varType;
    }
    index = lookupLocal(variable);
    if (index != -1) {
      varType = localTypes[index];
    }
    else {
      registerLocal(variable,exprType);
      varType = exprType;
      emit(TYPE_NAMES[varType]);
      emit(" ");
      emit(variable);
      emit(";\n");
      indent();
    }
    return varType;
  }


  private static void generateArraySet(String variable) {
    emit(variable);
    emit("[");
    expectToken(TOKEN_LBRACKET,"[");
    int indexType;
    indexType = expr();
    if (indexType != TYPE_INT) {
      System.out.print("ERROR: Array index must be int; was ");
      System.out.print(TYPE_NAMES[indexType]);
      System.out.print("\n");
      System.out.print(" @ ");
      System.out.print(lexerLoc);
      System.out.print("\n");
      System.exit(-1);
    }
    expectToken(TOKEN_RBRACKET,"]");
    emit("]");
    expectToken(TOKEN_EQ,"=");
    emit("=");
    expr();
    emit(";\n");
  }


  private static void parseStartsWithVariable(boolean semi) {
    String variable;
    variable = lexTokenString;
    advanceParser();
    if (lexTokenType == TOKEN_EQ) {
      advanceParser();
      String[] exprBuffer;
      exprBuffer = new String[100];
      String[] oldEmitBuffer;
      oldEmitBuffer = emitBuffer;
      emitBuffer = exprBuffer;
      int oldBufferIndex;
      oldBufferIndex = bufferIndex;
      bufferIndex = 0;
      int exprType;
      exprType = expr();
      int count;
      count = bufferIndex;
      emitBuffer = oldEmitBuffer;
      bufferIndex = oldBufferIndex;
      int varType;
      varType = registerOrLookUpVariable(variable,exprType);
      emit(variable);
      emit(" = ");
      spoolBuffer(exprBuffer,0,count);
      if (semi) {
        emit(";\n");
      }
      if (varType != exprType) {
        System.out.print("ERROR: Type mismatch: '");
        System.out.print(variable);
        System.out.print("' is ");
        System.out.print(TYPE_NAMES[varType]);
        System.out.print(" but expression is ");
        System.out.print(TYPE_NAMES[exprType]);
        System.out.print("\n");
        System.out.print(" @ ");
        System.out.print(lexerLoc);
        System.out.print("\n");
        System.exit(-1);
      }
      return;
    }
    else if (lexTokenType == TOKEN_COLON) {
      advanceParser();
      if (lexTokenType == TOKEN_KEYWORD && lexTokenKw == KW_PROC) {
        parseProc(variable);
      }
      else {
        parseVarDecl(variable);
      }
      return;
    }
    else if (lexTokenType == TOKEN_LPAREN) {
      generateProcCall(variable);
      if (semi) {
        emit(";\n");
      }
      return;
    }
    else if (lexTokenType == TOKEN_LBRACKET) {
      generateArraySet(variable);
      return;
    }
    System.out.print("ERROR: expected one of '=' ':' '(' '[' but found: ");
    printToken();
    System.out.print(" @ ");
    System.out.print(lexerLoc);
    System.out.print("\n");
    System.exit(-1);
  }


  private static void indent() {
    int i;
    i = 0;
    for (; i < indentSize; i = i + 1) {
      emit("  ");
    }
  }


  private static void parseBlock(boolean emitBraces) {
    expectToken(TOKEN_LBRACE,"{");
    if (emitBraces) {
      emit("{\n");
    }
    indentSize = indentSize + 1;
    for (; lexTokenType != TOKEN_RBRACE && lexTokenType != TOKEN_EOF;) {
      indent();
      parseStmt(true);
    }
    expectToken(TOKEN_RBRACE,"}");
    indentSize = indentSize - 1;
    if (emitBraces) {
      indent();
      emit("}\n");
    }
  }


  private static void parseIf() {
    emit("if (");
    int condType;
    condType = expr();
    if (condType != TYPE_BOOL) {
      System.out.print("ERROR: Expected boolean condition in if but found ");
      System.out.print(TYPE_NAMES[condType]);
      System.out.print("\n");
      System.out.print(" @ ");
      System.out.print(lexerLoc);
      System.out.print("\n");
      System.exit(-1);
    }
    emit(") ");
    parseBlock(true);
    for (; lexTokenType == TOKEN_KEYWORD && lexTokenKw == KW_ELIF;) {
      indentSize = indentSize - 1;
      indent();
      indentSize = indentSize + 1;
      emit("  else if (");
      advanceParser();
      condType = expr();
      emit(") ");
      if (condType != TYPE_BOOL) {
        System.out.print("ERROR: Expected boolean condition in elif but found ");
        System.out.print(TYPE_NAMES[condType]);
        System.out.print("\n");
        System.out.print(" @ ");
        System.out.print(lexerLoc);
        System.out.print("\n");
        System.exit(-1);
      }
      parseBlock(true);
    }
    if (lexTokenType == TOKEN_KEYWORD && lexTokenKw == KW_ELSE) {
      indentSize = indentSize - 1;
      indent();
      indentSize = indentSize + 1;
      emit("  else ");
      advanceParser();
      parseBlock(true);
    }
  }


  private static void parseBreak() {
    if (numWhiles == 0) {
      System.out.print("ERROR: Cannot have break outside while loop\n");
      System.out.print(" @ ");
      System.out.print(lexerLoc);
      System.out.print("\n");
      System.exit(-1);
    }
    emit("break;\n");
  }


  private static void parseContinue() {
    if (numWhiles == 0) {
      System.out.print("ERROR: Cannot have continue outside while loop\n");
      System.out.print(" @ ");
      System.out.print(lexerLoc);
      System.out.print("\n");
      System.exit(-1);
    }
    emit("continue;\n");
  }


  private static void parseWhile() {
    emit("for (; ");
    int condType;
    condType = expr();
    if (condType != TYPE_BOOL) {
      System.out.print("ERROR: Expected boolean as 'while' condition, but found ");
      System.out.print(TYPE_NAMES[condType]);
      System.out.print("\n");
      System.out.print(" @ ");
      System.out.print(lexerLoc);
      System.out.print("\n");
      System.exit(-1);
    }
    emit(";");
    if (lexTokenType == TOKEN_KEYWORD && lexTokenKw == KW_DO) {
      advanceParser();
      emit(" ");
      parseStmt(false);
    }
    emit(") ");
    parseBlock(true);
    numWhiles = numWhiles - 1;
  }


  private static void parsePrint() {
    emit("System.out.print(");
    expr();
    emit(");\n");
  }


  private static void parseStmt(boolean semi) {
    if (lexTokenType == TOKEN_EOF) {
      return;
    }
    else if (lexTokenType == TOKEN_KEYWORD) {
      int kw;
      kw = lexTokenKw;
      advanceParser();
      if (kw == KW_PRINT) {
        parsePrint();
        return;
      }
      else if (kw == KW_EXIT) {
        emit("System.exit(-1);\n");
        return;
      }
      else if (kw == KW_IF) {
        parseIf();
        return;
      }
      else if (kw == KW_WHILE) {
        parseWhile();
        return;
      }
      else if (kw == KW_BREAK) {
        parseBreak();
        return;
      }
      else if (kw == KW_CONTINUE) {
        parseContinue();
        return;
      }
      else if (kw == KW_RETURN) {
        parseReturn();
        return;
      }
    }
    else if (lexTokenType == TOKEN_VARIABLE) {
      parseStartsWithVariable(semi);
      return;
    }
    System.out.print("ERROR: Cannot parse start of statement token: ");
    printToken();
    System.out.print(" @ ");
    System.out.print(lexerLoc);
    System.out.print("\n");
    System.exit(-1);
  }


  private static void parseProgram() {
    System.out.print("package d2j;\n\n");
    System.out.print("import java.io.BufferedReader;\n");
    System.out.print("import java.io.IOException;\n");
    System.out.print("import java.io.InputStreamReader;\n\n");
    System.out.print("public class D2Program {\n");
    indentSize = 1;
    for (; lexTokenType != TOKEN_EOF;) {
      indent();
      parseStmt(true);
    }
    int i;
    i = 0;
    for (; i < procBufferIndex; i = i + 1) {
      if (procBuffer[i] != null) {
        System.out.print(procBuffer[i]);
      }
    }
    if (needsInput) {
      outputInput();
    }
    System.out.print("\n  public static void main(String args[]) {\n");
    i = 0;
    for (; i < bufferIndex; i = i + 1) {
      if (globBuffer[i] != null) {
        System.out.print(globBuffer[i]);
      }
    }
    System.out.print("  }\n}\n");
  }


  private static void procFinder() {
    for (; lexTokenType != TOKEN_EOF;) {
      if (lexTokenType == TOKEN_VARIABLE) {
        String variable;
        variable = lexTokenString;
        advanceParser();
        if (lexTokenType == TOKEN_COLON) {
          advanceParser();
          if (lexTokenKw == KW_PROC) {
            advanceParser();
            parseProcSignature(variable);
            continue;
          }
        }
      }
      advanceParser();
    }
  }


  private static void initParser() {
    String text;
    text = __d2_input();
    newLexer(text);
    advanceParser();
  }


  private static String __d2_input() {
    String input = "";
    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    try {
      String line = reader.readLine();
      while (line != null) {
        input += line + "\n";
        line = reader.readLine();
      }
    } catch (IOException e) {
      throw new RuntimeException("Could not read standard in", e);
    }
    return input;
  }

  public static void main(String args[]) {
    debug = false;
    TOKEN_EOF = 0;
    TOKEN_PLUS = 1;
    TOKEN_MINUS = 2;
    TOKEN_MULT = 3;
    TOKEN_DIV = 6;
    TOKEN_MOD = 7;
    TOKEN_EQEQ = 8;
    TOKEN_NEQ = 9;
    TOKEN_LT = 10;
    TOKEN_GT = 11;
    TOKEN_LEQ = 12;
    TOKEN_GEQ = 13;
    TOKEN_BIT_NOT = 14;
    TOKEN_INT = 15;
    TOKEN_BOOL = 16;
    TOKEN_STRING = 17;
    TOKEN_VARIABLE = 18;
    TOKEN_EQ = 19;
    TOKEN_LPAREN = 20;
    TOKEN_RPAREN = 21;
    TOKEN_LBRACE = 22;
    TOKEN_RBRACE = 23;
    TOKEN_COLON = 24;
    TOKEN_COMMA = 25;
    TOKEN_KEYWORD = 26;
    TOKEN_LBRACKET = 27;
    TOKEN_RBRACKET = 28;
    KW_PRINT = 0;
    KW_IF = 1;
    KW_ELSE = 2;
    KW_ELIF = 3;
    KW_PROC = 4;
    KW_RETURN = 5;
    KW_WHILE = 6;
    KW_DO = 7;
    KW_BREAK = 8;
    KW_CONTINUE = 9;
    KW_INT = 10;
    KW_BOOL = 11;
    KW_STRING = 12;
    KW_NULL = 13;
    KW_INPUT = 14;
    KW_LENGTH = 15;
    KW_CHR = 16;
    KW_ASC = 17;
    KW_EXIT = 18;
    KW_AND = 19;
    KW_OR = 20;
    KW_NOT = 21;
    KW_RECORD = 22;
    KW_NEW = 23;
    KW_PRINTLN = 24;
    KEYWORDS = new String[25];
  KEYWORDS[KW_PRINT]="print";
  KEYWORDS[KW_IF]="if";
  KEYWORDS[KW_ELSE]="else";
  KEYWORDS[KW_ELIF]="elif";
  KEYWORDS[KW_PROC]="proc";
  KEYWORDS[KW_RETURN]="return";
  KEYWORDS[KW_WHILE]="while";
  KEYWORDS[KW_DO]="do";
  KEYWORDS[KW_BREAK]="break";
  KEYWORDS[KW_CONTINUE]="continue";
  KEYWORDS[KW_INT]="int";
  KEYWORDS[KW_BOOL]="bool";
  KEYWORDS[KW_STRING]="string";
  KEYWORDS[KW_NULL]="null";
  KEYWORDS[KW_INPUT]="input";
  KEYWORDS[KW_LENGTH]="length";
  KEYWORDS[KW_CHR]="chr";
  KEYWORDS[KW_ASC]="asc";
  KEYWORDS[KW_EXIT]="exit";
  KEYWORDS[KW_AND]="and";
  KEYWORDS[KW_OR]="or";
  KEYWORDS[KW_NOT]="not";
  KEYWORDS[KW_RECORD]="record";
  KEYWORDS[KW_NEW]="new";
  KEYWORDS[KW_PRINTLN]="println";
    lexerText = "";
    lexerLoc = 0;
    lexerCc = 0;
            lexTokenType = 0;
    lexTokenString = "";
    lexTokenInt = 0;
    lexTokenKw = 0;
    lexTokenBool = false;
                                        TYPE_UNKNOWN = 0;
    TYPE_INT = 1;
    TYPE_BOOL = 2;
    TYPE_STRING = 3;
    TYPE_ARRAY = 4;
    TYPE_INT_ARRAY = 5;
    TYPE_BOOL_ARRAY = 6;
    TYPE_STRING_ARRAY = 7;
    TYPE_VOID = 8;
    D_TYPE_NAMES = new String[10];
  D_TYPE_NAMES[TYPE_UNKNOWN]="unknown";
  D_TYPE_NAMES[TYPE_INT]="int";
  D_TYPE_NAMES[TYPE_BOOL]="bool";
  D_TYPE_NAMES[TYPE_STRING]="string";
  D_TYPE_NAMES[TYPE_INT_ARRAY]="int[]";
  D_TYPE_NAMES[TYPE_BOOL_ARRAY]="bool[]";
  D_TYPE_NAMES[TYPE_STRING_ARRAY]="string[]";
  D_TYPE_NAMES[TYPE_VOID]="void";
    TYPE_NAMES = new String[10];
  TYPE_NAMES[TYPE_UNKNOWN]="unknown";
  TYPE_NAMES[TYPE_INT]="int";
  TYPE_NAMES[TYPE_BOOL]="boolean";
  TYPE_NAMES[TYPE_STRING]="String";
  TYPE_NAMES[TYPE_INT_ARRAY]="int[]";
  TYPE_NAMES[TYPE_BOOL_ARRAY]="boolean[]";
  TYPE_NAMES[TYPE_STRING_ARRAY]="String[]";
  TYPE_NAMES[TYPE_VOID]="void";
          numGlobals = 0;
    MAX_GLOBALS = 200;
    globalNames = new String[MAX_GLOBALS];
    globalTypes = new int[MAX_GLOBALS];
        globBuffer = new String[2000];
    procBufferIndex = 0;
    procBuffer = new String[20000];
    bufferIndex = 0;
      emitBuffer = globBuffer;
    MAX_NUM_PROCS = 100;
    numProcs = 0;
    procNames = new String[MAX_NUM_PROCS];
    returnTypes = new int[MAX_NUM_PROCS];
    numParams = new int[MAX_NUM_PROCS];
    PARAMS_PER_PROC = 4;
    paramNames = new String[MAX_NUM_PROCS * PARAMS_PER_PROC];
    paramTypes = new int[MAX_NUM_PROCS * PARAMS_PER_PROC];
    numLocals = new int[MAX_NUM_PROCS];
    LOCALS_PER_PROC = 10;
    localNames = new String[MAX_NUM_PROCS * LOCALS_PER_PROC];
    localTypes = new int[MAX_NUM_PROCS * LOCALS_PER_PROC];
    currentProcNum = -1;
                                              needsInput = false;
                              indentSize = 0;
          numWhiles = 0;
                  initParser();
  procFinder();
  resetLexer();
  advanceParser();
  parseProgram();
  }
}
