package d2j.v1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class D2Program {
  private static boolean debug;
  private static int TOKEN_EOF;
  private static int TOKEN_PLUS;
  private static int TOKEN_MINUS;
  private static int TOKEN_MULT;
  private static int TOKEN_BIT_AND;
  private static int TOKEN_BIT_OR;
  private static int TOKEN_BIT_NOT;
  private static int TOKEN_BIT_XOR;
  private static int TOKEN_DIV;
  private static int TOKEN_MOD;
  private static int TOKEN_EQEQ;
  private static int TOKEN_NEQ;
  private static int TOKEN_LT;
  private static int TOKEN_GT;
  private static int TOKEN_LEQ;
  private static int TOKEN_GEQ;
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
  private static int TOKEN_DOT;
  private static int TOKEN_SHIFT_LEFT;
  private static int TOKEN_SHIFT_RIGHT;
  private static int TOKEN_LITERAL_CONSTANT;
  private static int TOKEN_PLUSPLUS;
  private static int TOKEN_MINUSMINUS;
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
  private static int KW_LONG;
  private static int KW_DOUBLE;
  private static int KW_XOR;
  private static String[] KEYWORDS;
  private static int MAX_RECORDS;
  private static int TYPE_UNKNOWN;
  private static int TYPE_INT;
  private static int TYPE_BOOL;
  private static int TYPE_STRING;
  private static int TYPE_DOUBLE;
  private static int TYPE_LONG;
  private static int TYPE_BYTE;
  private static int TYPE_NULL;
  private static int TYPE_RECORD_BASE;
  private static int TYPE_ARRAY;
  private static int TYPE_INT_ARRAY;
  private static int TYPE_BOOL_ARRAY;
  private static int TYPE_STRING_ARRAY;
  private static int TYPE_DOUBLE_ARRAY;
  private static int TYPE_LONG_ARRAY;
  private static int TYPE_BYTE_ARRAY;
  private static int LAST_TYPE;
  private static int TYPE_VOID;
  private static int numRecords;
  private static String[] recordNames;
  private static int[] numFields;
  private static int FIELDS_PER_RECORD;
  private static String[] fieldNames;
  private static int[] fieldTypes;
  private static String lexerText;
  private static int lexerLoc;
  private static int lexerCc;
  private static int lexTokenType;
  private static String lexTokenString;
  private static int lexTokenKw;
  private static int lexTokenVarType;
  private static String[] D_TYPE_NAMES;
  private static String[] TYPE_NAMES;
  private static int numGlobals;
  private static int MAX_GLOBALS;
  private static String[] globalNames;
  private static int[] globalTypes;
  private static String[] mainBuffer;
  private static int preMainBufferIndex;
  private static String[] preMainBuffer;
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
  private static int indentSize;
  private static boolean needsInput;
  private static int numWhiles;

  private static boolean isNumeric(int type) {
    return isIntegral(type) || type == TYPE_DOUBLE;
  }

  private static boolean isIntegral(int type) {
    return type == TYPE_INT || type == TYPE_LONG;
  }

  private static boolean isRecordType(int type) {
    return type >= TYPE_RECORD_BASE && type < TYPE_RECORD_BASE + numRecords;
  }

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
      } else if (isLetter(lexerCc)) {
        return makeTextToken();
      } else {
        return makeSymbolToken();
      }
    }
    return Token(TOKEN_EOF, "");
  }

  private static void advanceLex() {
    if (lexerLoc < lexerText.length()) {
      lexerCc = lexerText.substring(lexerLoc, lexerLoc + 1).charAt(0);
    } else {
      lexerCc = 0;
    }
    lexerLoc = lexerLoc + 1;
  }

  private static String Token(int type, String value) {
    lexTokenType = type;
    lexTokenString = value;
    lexTokenKw = -1;
    return "t " + toString(type) + " " + value;
  }

  private static String LiteralToken(String prefix, int varType, String value) {
    lexTokenType = TOKEN_LITERAL_CONSTANT;
    lexTokenVarType = varType;
    lexTokenString = value;
    lexTokenKw = -1;
    return prefix + value;
  }

  private static String KeywordToken(int value, String valueAsString) {
    lexTokenType = TOKEN_KEYWORD;
    lexTokenString = valueAsString;
    lexTokenKw = value;
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
      return LiteralToken("b ", TYPE_BOOL, value);
    }
    int i;
    i = 0;
    for (; i < KEYWORDS.length; i = i + 1) {
      if (value.compareTo(KEYWORDS[i]) == 0) {
        return KeywordToken(i, value);
      }
    }
    return Token(TOKEN_VARIABLE, value);
  }

  private static String makeIntToken() {
    String value;
    value = "";
    for (; isDigit(lexerCc); advanceLex()) {
      value = value + Character.toString(lexerCc);
    }
    if (lexerCc == "L".charAt(0)) {
      advanceLex();
      return LiteralToken("l ", TYPE_LONG, value);
    }
    if (lexerCc == 46) {
      value = value + Character.toString(lexerCc);
      advanceLex();
      for (; isDigit(lexerCc); advanceLex()) {
        value = value + Character.toString(lexerCc);
      }
      return LiteralToken("d ", TYPE_DOUBLE, value);
    }
    return LiteralToken("i ", TYPE_INT, value);
  }

  private static String startsWithSlash() {
    advanceLex();
    if (lexerCc == 47) {
      advanceLex();
      for (; lexerCc != 10 && lexerCc != 0; advanceLex()) {}
      if (lexerCc != 0) {
        advanceLex();
      }
      return nextToken();
    }
    return Token(TOKEN_DIV, "/");
  }

  private static String startsWithBang() {
    advanceLex();
    if (lexerCc == 61) {
      advanceLex();
      return Token(TOKEN_NEQ, "!=");
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

  private static String startsWithGt() {
    advanceLex();
    if (lexerCc == 61) {
      advanceLex();
      return Token(TOKEN_GEQ, ">=");
    } else if (lexerCc == 62) {
      advanceLex();
      return Token(TOKEN_SHIFT_RIGHT, ">>");
    }
    return Token(TOKEN_GT, ">");
  }

  private static String startsWithLt() {
    advanceLex();
    if (lexerCc == 61) {
      advanceLex();
      return Token(TOKEN_LEQ, "<=");
    } else if (lexerCc == 60) {
      advanceLex();
      return Token(TOKEN_SHIFT_LEFT, "<<");
    }
    return Token(TOKEN_LT, "<");
  }

  private static String startsWithEq() {
    advanceLex();
    if (lexerCc == 61) {
      advanceLex();
      return Token(TOKEN_EQEQ, "==");
    }
    return Token(TOKEN_EQ, "=");
  }

  private static String startsWithPlus() {
    advanceLex();
    if (lexerCc == 43) {
      advanceLex();
      return Token(TOKEN_PLUSPLUS, "++");
    }
    return Token(TOKEN_PLUS, "+");
  }

  private static String startsWithMinus() {
    advanceLex();
    if (lexerCc == 45) {
      advanceLex();
      return Token(TOKEN_MINUSMINUS, "--");
    }
    return Token(TOKEN_MINUS, "-");
  }

  private static String makeStringLiteralToken(int firstQuote) {
    advanceLex();
    String value;
    value = "";
    for (; lexerCc != firstQuote && lexerCc != 0;) {
      if (lexerCc == 92) {
        advanceLex();
        if (lexerCc == 110) {
          value = value + Character.toString(10);
        } else if (lexerCc == 92) {
          value = value + Character.toString(92);
        }
      } else {
        value = value + Character.toString(lexerCc);
      }
      advanceLex();
    }
    if (lexerCc == 0) {
      System.out.print("ERROR: Unclosed string literal ");
      System.out.print(value);
      System.out.print("\n");
      System.out.print(" @ ");
      System.out.print(lexerLoc);
      System.out.print("\n");
      System.exit(-1);
    }
    advanceLex();
    return LiteralToken("s ", TYPE_STRING, value);
  }

  private static String makeSymbolToken() {
    int oc;
    oc = lexerCc;
    if (oc == 61) {
      return startsWithEq();
    } else if (oc == 60) {
      return startsWithLt();
    } else if (oc == 62) {
      return startsWithGt();
    } else if (oc == 43) {
      return startsWithPlus();
    } else if (oc == 45) {
      return startsWithMinus();
    } else if (oc == 40) {
      advanceLex();
      return Token(TOKEN_LPAREN, "(");
    } else if (oc == 41) {
      advanceLex();
      return Token(TOKEN_RPAREN, ")");
    } else if (oc == 42) {
      advanceLex();
      return Token(TOKEN_MULT, "*");
    } else if (oc == 47) {
      return startsWithSlash();
    } else if (oc == 37) {
      advanceLex();
      return Token(TOKEN_MOD, "%");
    } else if (oc == 38) {
      advanceLex();
      return Token(TOKEN_BIT_AND, "&");
    } else if (oc == 124) {
      advanceLex();
      return Token(TOKEN_BIT_OR, "|");
    } else if (oc == 94) {
      advanceLex();
      return Token(TOKEN_BIT_XOR, "^");
    } else if (oc == 33) {
      return startsWithBang();
    } else if (oc == 123) {
      advanceLex();
      return Token(TOKEN_LBRACE, "{");
    } else if (oc == 125) {
      advanceLex();
      return Token(TOKEN_RBRACE, "}");
    } else if (oc == 91) {
      advanceLex();
      return Token(TOKEN_LBRACKET, "[");
    } else if (oc == 93) {
      advanceLex();
      return Token(TOKEN_RBRACKET, "]");
    } else if (oc == 58) {
      advanceLex();
      return Token(TOKEN_COLON, ":");
    } else if (oc == 34 || oc == 39) {
      return makeStringLiteralToken(oc);
    } else if (oc == 44) {
      advanceLex();
      return Token(TOKEN_COMMA, ",");
    } else if (oc == 46) {
      advanceLex();
      return Token(TOKEN_DOT, ".");
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
    } else if (lexTokenType == TOKEN_LITERAL_CONSTANT) {
      if (lexTokenVarType == TYPE_INT) {
        System.out.print("Int token: ");
        System.out.print(lexTokenString);
        System.out.print("\n");
      }
      if (lexTokenVarType == TYPE_BOOL) {
        System.out.print("Bool token: ");
        System.out.print(lexTokenString);
        System.out.print("\n");
      }
      if (lexTokenVarType == TYPE_STRING) {
        System.out.print("String token: ");
        System.out.print(lexTokenString);
        System.out.print("\n");
      }
    } else if (lexTokenType == TOKEN_KEYWORD) {
      System.out.print("Keyword token: ");
      System.out.print(lexTokenString);
      System.out.print("\n");
    } else if (lexTokenType == TOKEN_VARIABLE) {
      System.out.print("Variable: ");
      System.out.print(lexTokenString);
      System.out.print("\n");
    } else {
      System.out.print("Token: ");
      System.out.print(lexTokenString);
      System.out.print(" type: ");
      System.out.print(lexTokenType);
      System.out.print("\n");
    }
  }

  private static String typeName(int type) {
    if (isRecordType(type)) {
      return recordNames[type - TYPE_RECORD_BASE];
    }
    return TYPE_NAMES[type];
  }

  private static void checkTypes(int leftType, int rightType) {
    if (leftType != rightType) {
      System.out.print("ERROR: Type mismatch. Left operand is ");
      System.out.print(typeName(leftType));
      System.out.print(", but right operand is ");
      System.out.print(typeName(rightType));
      System.out.print(" @ ");
      System.out.print(lexerLoc);
      System.out.print("\n");
      System.exit(-1);
    }
  }

  private static void advanceParser() {
    nextToken();
  }

  private static void expectToken(int expectedTokenType, String tokenStr) {
    if (lexTokenType != expectedTokenType) {
      System.out.print("ERROR: expected " + tokenStr + "; saw: ");
      printToken();
      System.out.print("@ ");
      System.out.print(lexerLoc);
      System.exit(-1);
    }
    advanceParser();
  }

  private static void expectKeyword(int expectedKwType, String tokenStr) {
    if (lexTokenType != TOKEN_KEYWORD || lexTokenKw != expectedKwType) {
      System.out.print("ERROR: expected ");
      System.out.print(tokenStr);
      System.out.print("; saw: ");
      printToken();
      System.out.print("@ ");
      System.out.print(lexerLoc);
      System.exit(-1);
    }
    advanceParser();
  }

  private static void registerGlobal(String name, int type) {
    if (type == TYPE_UNKNOWN) {
      System.out.print("Internal ERROR: Cannot register global '");
      System.out.print(name);
      System.out.print("' with UNKNOWN type\n");
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
    globalNames[numGlobals] = name;
    globalTypes[numGlobals] = type;
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
    emitBuffer[bufferIndex] = line;
    bufferIndex = bufferIndex + 1;
    if (debug) {
      System.out.print("// ");
      System.out.print(line);
      System.out.print("\n");
    }
  }

  private static void spoolBuffer(String[] buffer, int len) {
    int i;
    i = 0;
    for (; i < len; i = i + 1) {
      emit(buffer[i]);
    }
  }

  private static void registerProc(String name, int returnType) {
    if (returnType == TYPE_UNKNOWN) {
      System.out.print("INTERNAL ERROR: Cannot have UNKNOWN PROC return type\n");
      System.out.print(" @ ");
      System.out.print(lexerLoc);
      System.out.print("\n");
      System.exit(-1);
    }
    procNames[numProcs] = name;
    returnTypes[numProcs] = returnType;
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
    System.out.print("INTERNAL ERROR: Cannot set current proc num for proc '");
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
    System.out.print("INTERNAL ERROR: Cannot find PROC '");
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
      System.out.print("INTERNAL ERROR: Cannot look up parameter ");
      System.out.print(name);
      System.out.print(" because not in a PROC");
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
      System.out.print("INTERNAL ERROR: Cannot lookup local ");
      System.out.print(name);
      System.out.print(" because not in a PROC");
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

  private static void indent() {
    int i;
    i = 0;
    for (; i < indentSize; i = i + 1) {
      emit("  ");
    }
  }

  private static int expr() {
    return boolOr();
  }

  private static int boolOr() {
    int leftType;
    leftType = boolXor();
    int rightType;
    if (leftType == TYPE_BOOL) {
      for (; lexTokenKw == KW_OR;) {
        advanceParser();
        emit(" || ");
        rightType = boolXor();
        checkTypes(leftType, rightType);
      }
    }
    if (isIntegral(leftType)) {
      for (; lexTokenType == TOKEN_BIT_OR;) {
        advanceParser();
        emit(" | ");
        rightType = boolXor();
        checkTypes(leftType, rightType);
      }
    }
    return leftType;
  }

  private static int boolXor() {
    int leftType;
    leftType = boolAnd();
    int rightType;
    if (leftType == TYPE_BOOL) {
      for (; lexTokenKw == KW_XOR;) {
        advanceParser();
        emit(" ^ ");
        rightType = boolAnd();
        checkTypes(leftType, rightType);
      }
      return leftType;
    }
    if (isIntegral(leftType)) {
      for (; lexTokenType == TOKEN_BIT_XOR;) {
        advanceParser();
        emit(" ^ ");
        rightType = boolAnd();
        checkTypes(leftType, rightType);
      }
    }
    return leftType;
  }

  private static int boolAnd() {
    int leftType;
    leftType = compare();
    int rightType;
    if (leftType == TYPE_BOOL) {
      for (; lexTokenKw == KW_AND;) {
        advanceParser();
        emit(" && ");
        rightType = compare();
        checkTypes(leftType, rightType);
      }
    }
    if (isIntegral(leftType)) {
      for (; lexTokenType == TOKEN_BIT_AND;) {
        advanceParser();
        emit(" & ");
        rightType = compare();
        checkTypes(leftType, rightType);
      }
    }
    return leftType;
  }

  private static int compare() {
    int leftType;
    leftType = shift();
    String opstring;
    opstring = lexTokenString;
    int op;
    op = lexTokenType;
    int rightType;
    String[] compareBuffer;
    compareBuffer = new String[100];
    String[] oldEmitBuffer;
    int oldBufferIndex;
    int count;
    if (isNumeric(leftType) && (lexTokenType >= TOKEN_EQEQ && lexTokenType <= TOKEN_GEQ)) {
      advanceParser();
      emit(" ");
      emit(opstring);
      emit(" ");
      rightType = shift();
      checkTypes(leftType, rightType);
      return TYPE_BOOL;
    }
    if (leftType == TYPE_BOOL && (lexTokenType == TOKEN_EQEQ || lexTokenType == TOKEN_NEQ)) {
      advanceParser();
      emit(" ");
      emit(opstring);
      emit(" ");
      rightType = shift();
      checkTypes(leftType, rightType);
      return TYPE_BOOL;
    }
    if (isRecordType(leftType) && (lexTokenType == TOKEN_EQEQ || lexTokenType == TOKEN_NEQ)) {
      op = lexTokenType;
      advanceParser();
      oldEmitBuffer = emitBuffer;
      emitBuffer = compareBuffer;
      oldBufferIndex = bufferIndex;
      bufferIndex = 0;
      rightType = shift();
      if (rightType != TYPE_NULL && rightType != leftType) {
        System.out.print("ERROR: Type mismatch. Left operand is ");
        System.out.print(typeName(leftType));
        System.out.print(", but right operand is ");
        System.out.print(typeName(rightType));
        System.out.print(" @ ");
        System.out.print(lexerLoc);
        System.out.print("\n");
        System.exit(-1);
      }
      count = bufferIndex;
      emitBuffer = oldEmitBuffer;
      bufferIndex = oldBufferIndex;
      if (rightType == TYPE_NULL) {
        emit(" ");
        emit(opstring);
        emit(" null");
        return TYPE_BOOL;
      }
      emit(".equals(");
      spoolBuffer(compareBuffer, count);
      if (op == TOKEN_NEQ) {
        emit(" == false");
      }
      return TYPE_BOOL;
    }
    if (leftType == TYPE_STRING && (lexTokenType >= TOKEN_EQEQ && lexTokenType <= TOKEN_GEQ)) {
      advanceParser();
      oldEmitBuffer = emitBuffer;
      emitBuffer = compareBuffer;
      oldBufferIndex = bufferIndex;
      bufferIndex = 0;
      rightType = shift();
      if ((rightType != TYPE_STRING && rightType != TYPE_NULL)) {
        System.out.print("ERROR: Type mismatch. Left operand is ");
        System.out.print(typeName(leftType));
        System.out.print(", but right operand is ");
        System.out.print(typeName(rightType));
        System.out.print(" @ ");
        System.out.print(lexerLoc);
        System.out.print("\n");
        System.exit(-1);
      }
      count = bufferIndex;
      emitBuffer = oldEmitBuffer;
      bufferIndex = oldBufferIndex;
      if (rightType == TYPE_NULL) {
        emit(" ");
        emit(opstring);
        emit(" null");
      } else {
        emit(".compareTo(");
        spoolBuffer(compareBuffer, count);
        emit(") ");
        emit(opstring);
        emit(" 0");
      }
      return TYPE_BOOL;
    }
    return leftType;
  }

  private static int shift() {
    int leftType;
    leftType = addSub();
    for (; isIntegral(leftType)
        && (lexTokenType == TOKEN_SHIFT_LEFT || lexTokenType == TOKEN_SHIFT_RIGHT);) {
      int op;
      op = lexTokenType;
      advanceParser();
      if (op == TOKEN_SHIFT_LEFT) {
        emit(" << ");
      } else {
        emit(" >> ");
      }
      int rightType;
      rightType = addSub();
      checkTypes(leftType, rightType);
    }
    return leftType;
  }

  private static int addSub() {
    int leftType;
    leftType = mulDiv();
    if (leftType == TYPE_STRING || isNumeric(leftType)) {
      for (; lexTokenType == TOKEN_PLUS || lexTokenType == TOKEN_MINUS;) {
        if (leftType == TYPE_BOOL) {
          System.out.print("ERROR: Cannot add or subtract booleans");
          System.out.print(" @ ");
          System.out.print(lexerLoc);
          System.out.print("\n");
          System.exit(-1);
        }
        if (leftType == TYPE_STRING && lexTokenType == TOKEN_MINUS) {
          System.out.print("ERROR: Cannot subtract strings");
          System.out.print(" @ ");
          System.out.print(lexerLoc);
          System.out.print("\n");
          System.exit(-1);
        }
        String opstring;
        opstring = lexTokenString;
        advanceParser();
        emit(" ");
        emit(opstring);
        emit(" ");
        int rightType;
        rightType = mulDiv();
        checkTypes(leftType, rightType);
      }
    }
    return leftType;
  }

  private static int mulDiv() {
    int leftType;
    leftType = unary();
    for (; isNumeric(leftType) && (lexTokenType == TOKEN_MULT || lexTokenType == TOKEN_DIV
        || lexTokenType == TOKEN_MOD);) {
      if (leftType == TYPE_DOUBLE && lexTokenType == TOKEN_MOD) {
        System.out.print("ERROR: Cannot take MOD of doubles");
        System.out.print(" @ ");
        System.out.print(lexerLoc);
        System.out.print("\n");
        System.exit(-1);
      }
      String opstring;
      opstring = lexTokenString;
      advanceParser();
      emit(" ");
      emit(opstring);
      emit(" ");
      int rightType;
      rightType = unary();
      checkTypes(leftType, rightType);
    }
    return leftType;
  }

  private static int unary() {
    int type;
    if (lexTokenType == TOKEN_PLUS) {
      advanceParser();
      return unary();
    } else if (lexTokenType == TOKEN_MINUS) {
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
      if (isNumeric(type)) {
        emit("-");
        spoolBuffer(unaryBuffer, count);
        return type;
      }
      System.out.print("ERROR: cannot unary minus STRINGs");
      System.out.print(" @ ");
      System.out.print(lexerLoc);
      System.out.print("\n");
      System.exit(-1);
    } else if (lexTokenType == TOKEN_KEYWORD && lexTokenKw == KW_LENGTH) {
      advanceParser();
      expectToken(TOKEN_LPAREN, "(");
      type = expr();
      expectToken(TOKEN_RPAREN, ")");
      if (type == TYPE_STRING) {
        emit(".length()");
      } else if (isArrayType(type)) {
        emit(".length");
      } else {
        System.out.print("ERROR: Cannot take LENGTH of ");
        System.out.print(typeName(type));
        System.out.print("\n");
        System.out.print(" @ ");
        System.out.print(lexerLoc);
        System.out.print("\n");
        System.exit(-1);
      }
      return TYPE_INT;
    } else if (lexTokenType == TOKEN_KEYWORD && lexTokenKw == KW_ASC) {
      advanceParser();
      expectToken(TOKEN_LPAREN, "(");
      type = expr();
      emit(".charAt(0)");
      expectToken(TOKEN_RPAREN, ")");
      if (type != TYPE_STRING) {
        System.out.print("ERROR: Cannot take ASC of ");
        System.out.print(typeName(type));
        System.out.print("\n");
        System.out.print(" @ ");
        System.out.print(lexerLoc);
        System.out.print("\n");
        System.exit(-1);
      }
      return TYPE_INT;
    } else if (lexTokenType == TOKEN_KEYWORD && lexTokenKw == KW_CHR) {
      advanceParser();
      expectToken(TOKEN_LPAREN, "(");
      emit("Character.toString(");
      type = expr();
      emit(")");
      expectToken(TOKEN_RPAREN, ")");
      if (type != TYPE_INT) {
        System.out.print("ERROR: Cannot take CHR of ");
        System.out.print(typeName(type));
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
    return type > TYPE_ARRAY && type < TYPE_VOID;
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
      System.out.print("ERROR: ARRAY index must be INT; was ");
      System.out.print(typeName(indexType));
      System.out.print("\n");
      System.out.print(" @ ");
      System.out.print(lexerLoc);
      System.out.print("\n");
      System.exit(-1);
    }
    expectToken(TOKEN_RBRACKET, "]");
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
    expectToken(TOKEN_RBRACKET, "]");
    int count;
    count = bufferIndex;
    emitBuffer = oldEmitBuffer;
    bufferIndex = oldBufferIndex;
    if (indexType != TYPE_INT) {
      System.out.print("ERROR: String index must be int; was ");
      System.out.print(typeName(indexType));
      System.out.print("\n");
      System.out.print(" @ ");
      System.out.print(lexerLoc);
      System.out.print("\n");
      System.exit(-1);
    }
    spoolBuffer(exprBuffer, count);
    emit(", ");
    spoolBuffer(exprBuffer, count);
    emit(" + 1)");
  }

  private static int composite() {
    int leftType;
    leftType = atom();
    for (; lexTokenType == TOKEN_LBRACKET || lexTokenType == TOKEN_DOT;) {
      if (lexTokenType == TOKEN_LBRACKET) {
        expectToken(TOKEN_LBRACKET, "[");
        if (isArrayType(leftType)) {
          return generateArrayIndex(leftType);
        } else if (leftType == TYPE_STRING) {
          generateStringIndex();
          return leftType;
        }
        System.out.print("ERROR: Cannot take index of ");
        System.out.print(typeName(leftType));
        System.out.print("\n");
        System.out.print(" @ ");
        System.out.print(lexerLoc);
        System.out.print("\n");
        System.exit(-1);
      } else {
        expectToken(TOKEN_DOT, ".");
        if (isRecordType(leftType)) {
          emit(".");
          String fieldName;
          fieldName = lexTokenString;
          expectToken(TOKEN_VARIABLE, "field name");
          int recordIndex;
          recordIndex = leftType - TYPE_RECORD_BASE;
          int fieldIndex;
          fieldIndex = lookupField(recordIndex, fieldName);
          if (fieldIndex == -1) {
            System.out.print("ERROR: Unknown field ");
            System.out.print(fieldName);
            System.out.print(" of record type ");
            System.out.print(recordNames[recordIndex]);
            System.out.print("\n @ ");
            System.out.print(lexerLoc);
            System.out.print("\n");
            System.exit(-1);
            return -1;
          }
          emit(fieldName);
          int fieldType;
          fieldType = fieldTypes[fieldIndex];
          leftType = fieldType;
        } else {
          System.out.print("ERROR: Cannot reference field of non-record type");
          System.out.print(typeName(leftType));
          System.out.print("\n @ ");
          System.out.print(lexerLoc);
          System.out.print("\n");
          System.exit(-1);
          return -1;
        }
      }
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
    expectToken(TOKEN_LPAREN, "(");
    emit("(");
    int numArgs;
    numArgs = 0;
    for (; lexTokenType != TOKEN_RPAREN && lexTokenType != TOKEN_EOF;) {
      numArgs = numArgs + 1;
      int argType;
      argType = expr();
      if (lexTokenType == TOKEN_COMMA) {
        emit(", ");
        advanceParser();
      }
    }
    expectToken(TOKEN_RPAREN, ")");
    emit(")");
  }

  private static void generateInput() {
    emit("__d2_input()");
    needsInput = true;
  }

  private static void outputInput() {
    System.out.print(
        "\n  private static String __d2_input() {\n    String input = \"\";\n    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));\n    try {\n      String line = reader.readLine();\n      while (line != null) {\n        input += line + \"\\n\";\n        line = reader.readLine();\n      }\n    } catch (IOException e) {\n      throw new RuntimeException(\"Could not read standard in\", e);\n    }\n    return input;\n  }\n");
  }

  private static int atom() {
    if (lexTokenType == TOKEN_LITERAL_CONSTANT) {
      if (lexTokenVarType == TYPE_STRING) {
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
          } else if (c.compareTo("") == 0) {
            emit("\\r");
            continue;
          } else if (c.compareTo("\"") == 0) {
            emit("\\\"");
            continue;
          } else if (c.compareTo("\\") == 0) {
            emit("\\\\");
            continue;
          }
          emit(c);
        }
        emit("\"");
        advanceParser();
        return lexTokenVarType;
      } else if (isNumeric(lexTokenVarType)) {
        boolean longConstant;
        longConstant = lexTokenVarType == TYPE_LONG;
        emit(lexTokenString);
        advanceParser();
        if (longConstant) {
          emit("L");
        }
        return lexTokenVarType;
      } else if (lexTokenVarType == TYPE_BOOL) {
        emit(lexTokenString);
        advanceParser();
        return lexTokenVarType;
      }
    } else if (lexTokenType == TOKEN_VARIABLE) {
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
    } else if (lexTokenType == TOKEN_LPAREN) {
      expectToken(TOKEN_LPAREN, "(");
      emit("(");
      int exprType;
      exprType = expr();
      expectToken(TOKEN_RPAREN, ")");
      emit(")");
      return exprType;
    } else if (lexTokenType == TOKEN_KEYWORD && lexTokenKw == KW_INPUT) {
      advanceParser();
      generateInput();
      return TYPE_STRING;
    } else if (lexTokenType == TOKEN_KEYWORD && lexTokenKw == KW_NULL) {
      advanceParser();
      emit("null");
      return TYPE_NULL;
    } else if (lexTokenType == TOKEN_KEYWORD && lexTokenKw == KW_NEW) {
      advanceParser();
      if (lexTokenType != TOKEN_VARIABLE) {
        System.out.print("Expected variable after NEW; saw ");
        System.out.print(lexTokenString);
        System.out.print(" @ ");
        System.out.print(lexerLoc);
        System.out.print("\n");
        System.exit(-1);
      }
      String recordName;
      recordName = lexTokenString;
      int recordIndex;
      recordIndex = lookupRecord(recordName);
      if (recordIndex == -1) {
        System.out.print("Unknown record ");
        System.out.print(lexTokenString);
        System.out.print(" @ ");
        System.out.print(lexerLoc);
        System.out.print("\n");
        System.exit(-1);
      }
      emit("new ");
      emit(recordName);
      emit("()");
      advanceParser();
      return TYPE_RECORD_BASE + recordIndex;
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
    i = TYPE_INT;
    for (; i <= LAST_TYPE; i = i + 1) {
      if (D_TYPE_NAMES[i] != null && D_TYPE_NAMES[i].compareTo(lexTokenString) == 0) {
        advanceParser();
        return i;
      }
    }
    int recordIndex;
    recordIndex = lookupRecord(lexTokenString);
    if (recordIndex != -1) {
      advanceParser();
      return TYPE_RECORD_BASE + recordIndex;
    }
    System.out.print("ERROR: Unknown type ");
    printToken();
    System.out.print(" @ ");
    System.out.print(lexerLoc);
    System.out.print("\n");
    System.exit(-1);
    return -1;
  }

  private static void skipType() {
    int i;
    i = TYPE_INT;
    for (; i <= LAST_TYPE; i = i + 1) {
      if (D_TYPE_NAMES[i] != null && D_TYPE_NAMES[i].compareTo(lexTokenString) == 0) {
        advanceParser();
        return;
      }
    }
    expectToken(TOKEN_VARIABLE, "record type");
  }

  private static void parseVarDecl(String variable) {
    int baseType;
    baseType = parseType();
    if (lexTokenType != TOKEN_LBRACKET) {
      registerOrLookUpVariable(variable, baseType);
      return;
    }
    int arrayType;
    arrayType = baseType + TYPE_ARRAY;
    expectToken(TOKEN_LBRACKET, "[");
    if (lexTokenType == TOKEN_RBRACKET) {
      expectToken(TOKEN_RBRACKET, "]");
      registerOrLookUpVariable(variable, arrayType);
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
      System.out.print("ARRAY size must be INT; was ");
      System.out.print(typeName(sizeType));
      System.out.print("\n");
      System.out.print(" @ ");
      System.out.print(lexerLoc);
      System.out.print("\n");
      System.exit(-1);
    }
    expectToken(TOKEN_RBRACKET, "]");
    registerOrLookUpVariable(variable, arrayType);
    emit(variable);
    emit(" = new ");
    emit(typeName(baseType));
    emit("[");
    spoolBuffer(exprBuffer, count);
    emit("];\n");
  }

  private static int lookupRecord(String name) {
    int i;
    i = 0;
    for (; i < numRecords; i = i + 1) {
      if (recordNames[i].compareTo(name) == 0) {
        return i;
      }
    }
    return -1;
  }

  private static int lookupField(int recordIndex, String fieldName) {
    int i;
    i = recordIndex * FIELDS_PER_RECORD;
    int j;
    j = 0;
    for (; j < numFields[recordIndex]; j = j + 1) {
      if (fieldNames[i].compareTo(fieldName) == 0) {
        return i;
      }
      i = i + 1;
    }
    return -1;
  }

  private static int registerRecord(String name) {
    if (numRecords == MAX_RECORDS) {
      System.out.print("Max records already defined. Cannot add ");
      System.out.print(name);
      System.out.print(" @ ");
      System.out.print(lexerLoc);
      System.out.print("\n");
      System.exit(-1);
    }
    int i;
    i = 0;
    for (; i < numRecords; i = i + 1) {
      if (recordNames[i].compareTo(name) == 0) {
        System.out.print("Record ");
        System.out.print(name);
        System.out.print(" already declared\n");
        System.out.print(" @ ");
        System.out.print(lexerLoc);
        System.out.print("\n");
        System.exit(-1);
      }
    }
    recordNames[numRecords] = name;
    numRecords = numRecords + 1;
    return numRecords - 1;
  }

  private static void registerRecordName(String recordName) {
    registerRecord(recordName);
    expectKeyword(KW_RECORD, "RECORD");
    expectToken(TOKEN_LBRACE, "{");
    for (; lexTokenType != TOKEN_RBRACE && lexTokenType != TOKEN_EOF;) {
      expectToken(TOKEN_VARIABLE, "field");
      expectToken(TOKEN_COLON, ":");
      skipType();
      if (lexTokenType == TOKEN_LBRACKET) {
        expectToken(TOKEN_LBRACKET, "[");
        expr();
        expectToken(TOKEN_RBRACKET, "]");
      }
    }
    expectToken(TOKEN_RBRACE, "}");
  }

  private static void parseRecordDecl(String recordName) {
    int oldBufferIndex;
    oldBufferIndex = bufferIndex;
    String[] oldBuffer;
    oldBuffer = emitBuffer;
    emitBuffer = preMainBuffer;
    bufferIndex = preMainBufferIndex;
    int recIndex;
    recIndex = lookupRecord(recordName);
    expectKeyword(KW_RECORD, "RECORD");
    expectToken(TOKEN_LBRACE, "{");
    emit("  private static class ");
    emit(recordName);
    emit(" {\n");
    indentSize = indentSize + 1;
    int fieldIndex;
    fieldIndex = recIndex * FIELDS_PER_RECORD;
    for (; lexTokenType != TOKEN_RBRACE && lexTokenType != TOKEN_EOF;) {
      if (numFields[recIndex] == FIELDS_PER_RECORD) {
        System.out.print("More than 20 parameters declared for record ");
        System.out.print(recordName);
        System.out.print("\n @ ");
        System.out.print(lexerLoc);
        System.out.print("\n");
        System.exit(-1);
      }
      String fieldName;
      fieldName = lexTokenString;
      expectToken(TOKEN_VARIABLE, "variable");
      expectToken(TOKEN_COLON, ":");
      int type;
      type = parseType();
      indent();
      emit(typeName(type));
      emit(" ");
      emit(fieldName);
      emit(";\n");
      fieldNames[fieldIndex] = fieldName;
      fieldTypes[fieldIndex] = type;
      fieldIndex = fieldIndex + 1;
      numFields[recIndex] = numFields[recIndex] + 1;
    }
    indentSize = indentSize - 1;
    indent();
    emit("}\n");
    expectToken(TOKEN_RBRACE, "}");
    if (debug) {
      System.out.print("; # records: ");
      System.out.print(numRecords);
      System.out.print("\n");
      System.out.print("; record name: ");
      System.out.print(recordNames[recIndex]);
      System.out.print("\n");
      System.out.print("; numFields: ");
      System.out.print(numFields[recIndex]);
      System.out.print("\n");
      System.out.print("; fields: ");
      int i;
      i = 0;
      for (; i < numFields[recIndex]; i = i + 1) {
        System.out.print(fieldNames[recIndex * FIELDS_PER_RECORD + i]);
        System.out.print(" ");
      }
      System.out.print("\n");
      System.out.print("\n");
      System.out.print("\n");
    }
    preMainBufferIndex = bufferIndex;
    emitBuffer = oldBuffer;
    bufferIndex = oldBufferIndex;
  }

  private static void parseProc(String procName) {
    expectKeyword(KW_PROC, "PROC");
    if (currentProcNum != -1) {
      System.out.print("Cannot define nested PROCs\n");
      System.out.print(" @ ");
      System.out.print(lexerLoc);
      System.out.print("\n");
      System.exit(-1);
    }
    setCurrentProcNum(procName);
    expectToken(TOKEN_LPAREN, "(");
    for (; lexTokenType != TOKEN_RPAREN;) {
      expectToken(TOKEN_VARIABLE, "variable");
      expectToken(TOKEN_COLON, ":");
      parseType();
      if (lexTokenType == TOKEN_LBRACKET) {
        expectToken(TOKEN_LBRACKET, "[");
        expectToken(TOKEN_RBRACKET, "]");
      }
      if (lexTokenType == TOKEN_COMMA) {
        advanceParser();
      } else {
        break;
      }
    }
    expectToken(TOKEN_RPAREN, ")");
    int oldBufferIndex;
    oldBufferIndex = bufferIndex;
    String[] oldBuffer;
    oldBuffer = emitBuffer;
    emitBuffer = preMainBuffer;
    bufferIndex = preMainBufferIndex;
    int returnType;
    returnType = TYPE_VOID;
    if (lexTokenType == TOKEN_COLON) {
      advanceParser();
      returnType = parseType();
    }
    emit("\n  private static ");
    emit(typeName(returnType));
    emit(" ");
    emit(procName);
    emit("(");
    int i;
    i = 0;
    for (; i < numParams[currentProcNum]; i = i + 1) {
      emit(typeName(paramTypes[currentProcNum * 4 + i]));
      emit(" ");
      emit(paramNames[currentProcNum * 4 + i]);
      if (i < numParams[currentProcNum] - 1) {
        emit(", ");
      }
    }
    emit(") ");
    parseBlock(true);
    currentProcNum = -1;
    preMainBufferIndex = bufferIndex;
    emitBuffer = oldBuffer;
    bufferIndex = oldBufferIndex;
  }

  private static void parseProcSignature(String procName) {
    expectToken(TOKEN_LPAREN, "(");
    int myProcNum;
    myProcNum = numProcs;
    int paramIndex;
    paramIndex = myProcNum * PARAMS_PER_PROC;
    int index;
    index = 0;
    for (; lexTokenType != TOKEN_RPAREN;) {
      if (lexTokenType != TOKEN_VARIABLE) {
        System.out.print("Expected variable but found: ");
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
      expectToken(TOKEN_COLON, ":");
      int type;
      type = parseType();
      if (lexTokenType == TOKEN_LBRACKET) {
        expectToken(TOKEN_LBRACKET, "[");
        expectToken(TOKEN_RBRACKET, "]");
        type = type + TYPE_ARRAY;
      }
      paramNames[paramIndex] = paramName;
      paramTypes[paramIndex] = type;
      paramIndex = paramIndex + 1;
      index = index + 1;
      numParams[myProcNum] = numParams[myProcNum] + 1;
      if (lexTokenType == TOKEN_COMMA) {
        advanceParser();
      } else {
        break;
      }
    }
    expectToken(TOKEN_RPAREN, ")");
    int returnType;
    returnType = TYPE_VOID;
    if (lexTokenType == TOKEN_COLON) {
      advanceParser();
      returnType = parseType();
    }
    registerProc(procName, returnType);
  }

  private static boolean isAtStartOfExpression() {
    if (lexTokenType == TOKEN_KEYWORD) {
      return lexTokenKw == KW_ASC || lexTokenKw == KW_CHR || lexTokenKw == KW_INPUT
          || lexTokenKw == KW_LENGTH || lexTokenKw == KW_NEW || lexTokenKw == KW_NOT;
    }
    return lexTokenType == TOKEN_LITERAL_CONSTANT || lexTokenType == TOKEN_BIT_NOT
        || lexTokenType == TOKEN_LPAREN || lexTokenType == TOKEN_MINUS || lexTokenType == TOKEN_PLUS
        || lexTokenType == TOKEN_VARIABLE;
  }

  private static void parseReturn() {
    if (currentProcNum == -1) {
      System.out.print("ERROR: Cannot return outside proc\n");
      System.out.print(" @ ");
      System.out.print(lexerLoc);
      System.out.print("\n");
      System.exit(-1);
    }
    emit("return");
    if (isAtStartOfExpression()) {
      emit(" ");
      int actualType;
      actualType = expr();
      int expectedType;
      expectedType = returnTypes[currentProcNum];
      checkTypes(expectedType, actualType);
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
    localNames[base] = name;
    localTypes[base] = type;
    numLocals[currentProcNum] = myLocalCount + 1;
  }

  private static int registerOrLookUpVariable(String variable, int exprType) {
    int varType;
    varType = lookupGlobal(variable);
    boolean isGlobal;
    isGlobal = varType != TYPE_UNKNOWN || currentProcNum == -1;
    if (isGlobal) {
      if (varType == TYPE_UNKNOWN) {
        registerGlobal(variable, exprType);
        varType = exprType;
        int oldBufferIndex;
        oldBufferIndex = bufferIndex;
        String[] oldBuffer;
        oldBuffer = emitBuffer;
        emitBuffer = preMainBuffer;
        bufferIndex = preMainBufferIndex;
        emit("  private static ");
        emit(typeName(varType));
        emit(" ");
        emit(variable);
        emit(";\n");
        preMainBufferIndex = bufferIndex;
        emitBuffer = oldBuffer;
        bufferIndex = oldBufferIndex;
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
    } else {
      registerLocal(variable, exprType);
      varType = exprType;
      emit(typeName(varType));
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
    expectToken(TOKEN_LBRACKET, "[");
    int indexType;
    indexType = expr();
    if (indexType != TYPE_INT) {
      System.out.print("ERROR: Array index must be int; was ");
      System.out.print(typeName(indexType));
      System.out.print("\n");
      System.out.print(" @ ");
      System.out.print(lexerLoc);
      System.out.print("\n");
      System.exit(-1);
    }
    expectToken(TOKEN_RBRACKET, "]");
    emit("]");
    expectToken(TOKEN_EQ, "=");
    emit(" = ");
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
      varType = registerOrLookUpVariable(variable, exprType);
      checkTypes(varType, exprType);
      emit(variable);
      emit(" = ");
      spoolBuffer(exprBuffer, count);
      if (semi) {
        emit(";\n");
      }
      return;
    } else if (lexTokenType == TOKEN_COLON) {
      advanceParser();
      if (lexTokenType == TOKEN_KEYWORD && lexTokenKw == KW_PROC) {
        parseProc(variable);
      } else if (lexTokenType == TOKEN_KEYWORD && lexTokenKw == KW_RECORD) {
        parseRecordDecl(variable);
      } else {
        parseVarDecl(variable);
      }
      return;
    } else if (lexTokenType == TOKEN_LPAREN) {
      generateProcCall(variable);
      if (semi) {
        emit(";\n");
      }
      return;
    } else if (lexTokenType == TOKEN_LBRACKET) {
      generateArraySet(variable);
      return;
    } else if (lexTokenType == TOKEN_PLUSPLUS) {
      advanceParser();
      emit(variable);
      emit("++");
      if (semi) {
        emit(";\n");
      }
      return;
    } else if (lexTokenType == TOKEN_MINUSMINUS) {
      advanceParser();
      emit(variable);
      emit("--");
      return;
    } else if (lexTokenType == TOKEN_DOT) {
      generateFieldSet(variable);
      if (semi) {
        emit(";\n");
      }
      return;
    }
    System.out.print("ERROR: expected one of '=' ':' '(' '[' '--' '++' but found: ");
    printToken();
    System.out.print(" @ ");
    System.out.print(lexerLoc);
    System.out.print("\n");
    System.exit(-1);
  }

  private static void generateFieldSet(String variable) {
    int varType;
    varType = generateGetVariable(variable);
    if (isRecordType(varType)) {
      expectToken(TOKEN_DOT, ".");
      emit(".");
      String fieldName;
      fieldName = lexTokenString;
      int recordIndex;
      recordIndex = varType - TYPE_RECORD_BASE;
      expectToken(TOKEN_VARIABLE, "field name");
      int fieldIndex;
      fieldIndex = lookupField(recordIndex, fieldName);
      if (fieldIndex == -1) {
        System.out.print("ERROR: Unknown field ");
        System.out.print(fieldName);
        System.out.print(" of record ");
        System.out.print(variable);
        System.out.print("\n @ ");
        System.out.print(lexerLoc);
        System.out.print("\n");
        System.exit(-1);
      }
      int fieldType;
      fieldType = fieldTypes[fieldIndex];
      emit(fieldName);
      expectToken(TOKEN_EQ, "=");
      emit(" = ");
      int exprType;
      exprType = expr();
      checkTypes(fieldType, exprType);
    } else {
      System.out.print("ERROR: variable ");
      System.out.print(variable);
      System.out.print(" is not record type.");
      System.out.print(" @ ");
      System.out.print(lexerLoc);
      System.out.print("\n");
      System.exit(-1);
    }
  }

  private static void parseBlock(boolean emitBraces) {
    expectToken(TOKEN_LBRACE, "{");
    if (emitBraces) {
      emit("{\n");
    }
    indentSize = indentSize + 1;
    for (; lexTokenType != TOKEN_RBRACE && lexTokenType != TOKEN_EOF;) {
      indent();
      parseStmt(true);
    }
    expectToken(TOKEN_RBRACE, "}");
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
      System.out.print(typeName(condType));
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
        System.out.print(typeName(condType));
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
    numWhiles = numWhiles + 1;
    emit("for (; ");
    int condType;
    condType = expr();
    if (condType != TYPE_BOOL) {
      System.out.print("ERROR: Expected boolean as 'while' condition, but found ");
      System.out.print(typeName(condType));
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

  private static void parsePrint(int kw) {
    emit("System.out.print");
    if (kw == KW_PRINTLN) {
      emit("ln");
    }
    emit("(");
    expr();
    emit(");\n");
  }

  private static void parseStmt(boolean semi) {
    if (lexTokenType == TOKEN_EOF) {
      return;
    } else if (lexTokenType == TOKEN_KEYWORD) {
      int kw;
      kw = lexTokenKw;
      advanceParser();
      if (kw == KW_PRINT || kw == KW_PRINTLN) {
        parsePrint(kw);
        return;
      } else if (kw == KW_EXIT) {
        emit("System.exit(-1);\n");
        return;
      } else if (kw == KW_IF) {
        parseIf();
        return;
      } else if (kw == KW_WHILE) {
        parseWhile();
        return;
      } else if (kw == KW_BREAK) {
        parseBreak();
        return;
      } else if (kw == KW_CONTINUE) {
        parseContinue();
        return;
      } else if (kw == KW_RETURN) {
        parseReturn();
        return;
      }
    } else if (lexTokenType == TOKEN_VARIABLE) {
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
    for (; i < preMainBufferIndex; i = i + 1) {
      if (preMainBuffer[i] != null) {
        System.out.print(preMainBuffer[i]);
      }
    }
    if (needsInput) {
      outputInput();
    }
    System.out.print("\n  public static void main(String args[]) {\n");
    i = 0;
    for (; i < bufferIndex; i = i + 1) {
      if (mainBuffer[i] != null) {
        System.out.print(mainBuffer[i]);
      }
    }
    if (debug) {
      System.out.print("  // preMainBufferIndex");
      System.out.print(preMainBufferIndex);
      System.out.print("\n");
      System.out.print("  // bufferIndex");
      System.out.print(bufferIndex);
      System.out.print("\n");
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
    resetLexer();
    advanceParser();
  }

  private static void recordFinder() {
    for (; lexTokenType != TOKEN_EOF;) {
      if (lexTokenType == TOKEN_VARIABLE) {
        String variable;
        variable = lexTokenString;
        advanceParser();
        if (lexTokenType == TOKEN_COLON) {
          advanceParser();
          if (lexTokenType == TOKEN_KEYWORD && lexTokenKw == KW_RECORD) {
            registerRecordName(variable);
          }
          continue;
        }
        continue;
      }
      advanceParser();
    }
    resetLexer();
    advanceParser();
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
    TOKEN_BIT_AND = 4;
    TOKEN_BIT_OR = 5;
    TOKEN_BIT_NOT = 6;
    TOKEN_BIT_XOR = 7;
    TOKEN_DIV = 8;
    TOKEN_MOD = 9;
    TOKEN_EQEQ = 10;
    TOKEN_NEQ = 11;
    TOKEN_LT = 12;
    TOKEN_GT = 13;
    TOKEN_LEQ = 14;
    TOKEN_GEQ = 15;
    TOKEN_VARIABLE = 16;
    TOKEN_EQ = 17;
    TOKEN_LPAREN = 18;
    TOKEN_RPAREN = 19;
    TOKEN_LBRACE = 20;
    TOKEN_RBRACE = 21;
    TOKEN_COLON = 22;
    TOKEN_COMMA = 23;
    TOKEN_KEYWORD = 24;
    TOKEN_LBRACKET = 25;
    TOKEN_RBRACKET = 26;
    TOKEN_DOT = 27;
    TOKEN_SHIFT_LEFT = 28;
    TOKEN_SHIFT_RIGHT = 29;
    TOKEN_LITERAL_CONSTANT = 30;
    TOKEN_PLUSPLUS = 31;
    TOKEN_MINUSMINUS = 32;
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
    KW_LONG = 25;
    KW_DOUBLE = 26;
    KW_XOR = 27;
    KEYWORDS = new String[KW_XOR + 1];
    KEYWORDS[KW_PRINT] = "print";
    KEYWORDS[KW_IF] = "if";
    KEYWORDS[KW_ELSE] = "else";
    KEYWORDS[KW_ELIF] = "elif";
    KEYWORDS[KW_PROC] = "proc";
    KEYWORDS[KW_RETURN] = "return";
    KEYWORDS[KW_WHILE] = "while";
    KEYWORDS[KW_DO] = "do";
    KEYWORDS[KW_BREAK] = "break";
    KEYWORDS[KW_CONTINUE] = "continue";
    KEYWORDS[KW_INT] = "int";
    KEYWORDS[KW_BOOL] = "bool";
    KEYWORDS[KW_STRING] = "string";
    KEYWORDS[KW_LONG] = "long";
    KEYWORDS[KW_DOUBLE] = "double";
    KEYWORDS[KW_NULL] = "null";
    KEYWORDS[KW_INPUT] = "input";
    KEYWORDS[KW_LENGTH] = "length";
    KEYWORDS[KW_CHR] = "chr";
    KEYWORDS[KW_ASC] = "asc";
    KEYWORDS[KW_EXIT] = "exit";
    KEYWORDS[KW_AND] = "and";
    KEYWORDS[KW_OR] = "or";
    KEYWORDS[KW_NOT] = "not";
    KEYWORDS[KW_XOR] = "xor";
    KEYWORDS[KW_RECORD] = "record";
    KEYWORDS[KW_NEW] = "new";
    KEYWORDS[KW_PRINTLN] = "println";
    MAX_RECORDS = 20;
    TYPE_UNKNOWN = 0;
    TYPE_INT = 1;
    TYPE_BOOL = 2;
    TYPE_STRING = 3;
    TYPE_DOUBLE = 4;
    TYPE_LONG = 5;
    TYPE_BYTE = 6;
    TYPE_NULL = 7;
    TYPE_RECORD_BASE = 8;
    TYPE_ARRAY = TYPE_RECORD_BASE + MAX_RECORDS + 1;
    TYPE_INT_ARRAY = TYPE_ARRAY + TYPE_INT;
    TYPE_BOOL_ARRAY = TYPE_ARRAY + TYPE_BOOL;
    TYPE_STRING_ARRAY = TYPE_ARRAY + TYPE_STRING;
    TYPE_DOUBLE_ARRAY = TYPE_ARRAY + TYPE_DOUBLE;
    TYPE_LONG_ARRAY = TYPE_ARRAY + TYPE_LONG;
    TYPE_BYTE_ARRAY = TYPE_ARRAY + TYPE_BYTE;
    LAST_TYPE = TYPE_BYTE_ARRAY + 1;
    TYPE_VOID = LAST_TYPE + 1;
    numRecords = 0;
    recordNames = new String[MAX_RECORDS];
    numFields = new int[MAX_RECORDS];
    FIELDS_PER_RECORD = 20;
    fieldNames = new String[MAX_RECORDS * FIELDS_PER_RECORD];
    fieldTypes = new int[MAX_RECORDS * FIELDS_PER_RECORD];
    lexerText = "";
    lexerLoc = 0;
    lexerCc = 0;
    lexTokenType = 0;
    lexTokenString = "";
    lexTokenKw = 0;
    lexTokenVarType = -1;
    D_TYPE_NAMES = new String[TYPE_VOID + 1];
    D_TYPE_NAMES[TYPE_UNKNOWN] = "unknown";
    D_TYPE_NAMES[TYPE_INT] = "int";
    D_TYPE_NAMES[TYPE_BOOL] = "bool";
    D_TYPE_NAMES[TYPE_BYTE] = "byte";
    D_TYPE_NAMES[TYPE_STRING] = "string";
    D_TYPE_NAMES[TYPE_INT_ARRAY] = "int[]";
    D_TYPE_NAMES[TYPE_BOOL_ARRAY] = "bool[]";
    D_TYPE_NAMES[TYPE_STRING_ARRAY] = "string[]";
    D_TYPE_NAMES[TYPE_VOID] = "void";
    D_TYPE_NAMES[TYPE_NULL] = "null";
    D_TYPE_NAMES[TYPE_DOUBLE] = "double";
    D_TYPE_NAMES[TYPE_LONG] = "long";
    D_TYPE_NAMES[TYPE_LONG_ARRAY] = "long[]";
    D_TYPE_NAMES[TYPE_DOUBLE_ARRAY] = "double[]";
    D_TYPE_NAMES[TYPE_BYTE_ARRAY] = "byte[]";
    TYPE_NAMES = new String[TYPE_VOID + 1];
    TYPE_NAMES[TYPE_UNKNOWN] = "unknown";
    TYPE_NAMES[TYPE_INT] = "int";
    TYPE_NAMES[TYPE_BOOL] = "boolean";
    TYPE_NAMES[TYPE_STRING] = "String";
    TYPE_NAMES[TYPE_INT_ARRAY] = "int[]";
    TYPE_NAMES[TYPE_BOOL_ARRAY] = "boolean[]";
    TYPE_NAMES[TYPE_STRING_ARRAY] = "String[]";
    TYPE_NAMES[TYPE_VOID] = "void";
    TYPE_NAMES[TYPE_NULL] = "null";
    TYPE_NAMES[TYPE_DOUBLE] = "double";
    TYPE_NAMES[TYPE_LONG] = "long";
    TYPE_NAMES[TYPE_LONG_ARRAY] = "long[]";
    TYPE_NAMES[TYPE_DOUBLE_ARRAY] = "double[]";
    numGlobals = 0;
    MAX_GLOBALS = 200;
    globalNames = new String[MAX_GLOBALS];
    globalTypes = new int[MAX_GLOBALS];
    mainBuffer = new String[2000];
    preMainBufferIndex = 0;
    preMainBuffer = new String[30000];
    bufferIndex = 0;
    emitBuffer = mainBuffer;
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
    indentSize = 0;
    needsInput = false;
    numWhiles = 0;
    initParser();
    recordFinder();
    procFinder();
    parseProgram();
  }
}
