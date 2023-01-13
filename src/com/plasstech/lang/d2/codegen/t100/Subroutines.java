package com.plasstech.lang.d2.codegen.t100;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.plasstech.lang.d2.common.TokenType;

class Subroutines {
  static final String PRINT32 = "D_print32";
  static final String COPY32 = "D_copy32";
  static final String SUB32 = "D_sub32";
  static final String COMP32 = "D_comp32";
  static final String ADD32 = "D_add32";
  static final String MULT8 = "D_mult8";
  static final String MULT32 = "D_mult32";
  static final String PRINT8 = "D_print8";
  static final String SHIFT_LEFT8 = "D_shift_left8";
  static final String SHIFT_RIGHT8 = "D_shift_right8";
  static final String SHIFT_LEFT32 = "D_shift_left32";
  static final String SHIFT_RIGHT32 = "D_shift_right32";
  static final String INC32 = "D_inc32";
  static final String DEC32 = "D_dec32";
  static final String DIV8 = "D_div8";
  static final String AND32 = "D_bitand32";
  static final String OR32 = "D_bitor32";
  static final String XOR32 = "D_bitxor32";
  static final String NOT32 = "D_bitnot32";

  private static final List<String> PRINT32_CODE =
      ImmutableList.<String>builder()
          .add("\n; Prints the 4 bytes at BC")
          .add("; Destroys: A, B, C, D, E")
          .add(PRINT32 + ":")
          .add("  push H")
          .add("  push B")
          .add("  inx B")
          .add("  inx B")
          .add("  inx B ; go to msb")
          .add("  ldax B ; get the msb")
          .add("  pop B")
          .add("  cpi 0x00")
          .add("  jp " + PRINT32 + "_not_negative  ; positive, just do it")
          .add("  ; else:")
          .add("  ; print -")
          .add("  mvi A, 0x2d  ; minus sign")
          .add("  call 0x0020  ; print a minus sign before the negative byte")
          .add("  ; negate bc in temp")
          .add("  lxi H, 0x0000")
          .add("  shld TEMP")
          .add("  shld TEMP + 0x02")
          .add("  push B")
          .add("  pop H ; h = original b, the thing to print")
          .add("  lxi B, TEMP")
          .add("  call " + SUB32 + "  ; bc = bc - hl; temp = temp - old bc == -old bc")
          .add("  ; BC points at TEMP now")
          // TODO: don't have to re-copy from bc to temp
          .add(PRINT32 + "_not_negative:")
          .add("  ; Copy vector from BC to TEMP")
          .add("  lxi H, TEMP")
          .add("  call " + COPY32)
          .add("  lxi B, TEMP")
          .add("  ; HL starts at 1 billions place.")
          .add("  lxi H, PLACES_1b")
          .add("  mvi E, 0x00  ; counter: how many digits in this place")
          .add("  mov D, E     ; flag: did we print anything yet?")
          .add("  ; while vector at BC >= vector at HL:")
          .add(PRINT32 + "_while:")
          .add("  call " + COMP32)
          .add("  jc " + PRINT32 + "_printone  ; break if BC < HL")
          .add("  inr E  ; Increase counter for this digit")
          .add("  inr D  ; Sets flag indicating we have a number")
          .add("  call " + SUB32 + "  ; BC=BC-HL")
          .add("  jmp " + PRINT32 + "_while")
          .add(PRINT32 + "_printone:")
          .add("  mov A, D")
          .add("  cpi 0x00  ; if D == 0, we have never printed, don't print this zero.")
          .add("  jz " + PRINT32 + "_skipped_leading_zero")
          .add("  ; Print digit in E (# of times we subtracted):")
          .add("  mov A, E")
          .add("  adi 0x30  ; convert from number to ascii digit")
          .add("  call 0x0020  ; print one digit in A")
          .add(PRINT32 + "_skipped_leading_zero:")
          .add("  mvi E, 0x00  ; clear counter for next digit")
          .add("  ; HL=HL+4")
          .add("  inx H  ; go to next 'place'")
          .add("  inx H")
          .add("  inx H")
          .add("  inx H")
          .add("  mov A, M")
          .add("  cpi 0xff  ; sentinel that indicates the end")
          .add("  jnz " + PRINT32 + "_while")
          .add("  mov A, D ; see if we have ever printed anything")
          .add("  cpi 0x00  ; if D == 0, we have never printed. print a zero")
          .add("  jnz " + PRINT32 + "_end")
          .add("  mvi A, 0x30  ; zero")
          .add("  call 0x0020  ; print one letter")
          .add(PRINT32 + "_end:")
          .add("  pop H")
          .add("  ret")
          .build();

  private static final List<String> COPY32_CODE =
      ImmutableList.<String>builder()
          .add("\n; Copies 4 bytes from BC to HL: [HL] = [BC] ")
          .add("; Destroys A")
          .add(COPY32 + ":")
          .add("  push B")
          .add("  push D")
          .add("  push H")
          .add("  mvi D, 0x04  ; compare 4 bytes")
          .add(COPY32 + "_loop:")
          .add("  ldax B  ; read from source")
          .add("  mov M, A  ; write to dest")
          .add("  inx B   ; next byte")
          .add("  inx H")
          .add("  dcr D")
          .add("  jnz " + COPY32 + "_loop")
          .add("  pop H")
          .add("  pop D")
          .add("  pop B")
          .add("  ret")
          .build();

  private static final List<String> COMP32_CODE =
      ImmutableList.<String>builder()
          .add("\n; Compares the 4 bytes at BC to HL")
          .add("; Inputs: BC (left, unchanged), HL (right, unchanged)")
          .add("; Destroys: A")
          .add("; Updates: Z, C flags to reflect the comparison")
          .add(COMP32 + ":")
          .add("  push B")
          .add("  push D")
          .add("  push H")
          .add("  ; jump to the end of the 32-bits to compare from MSB to LSB")
          .add("  inx B")
          .add("  inx B")
          .add("  inx B")
          .add("  inx H")
          .add("  inx H")
          .add("  inx H")
          .add("  mvi D, 0x04  ; compare 4 bytes")
          .add(COMP32 + "_loop:")
          .add("  ldax B ; a <-[BC]")
          .add("  cmp M ; compare A with [HL], i.e., [BC] and [HL]")
          .add("  jc " + COMP32 + "_end  ; carry set: means BC<HL, done")
          .add(
              "  jnz "
                  + COMP32
                  + "_end  ; not BC<HL, and not zero: means BC>HL, otherwise, continue")
          .add("  dcx B ; next lower byte in LHS (goes right to left because little-endian)")
          .add("  dcx H ; next lower byte in RHS")
          .add("  dcr D ; decrement counter (# bytes to compare)")
          .add("  jnz " + COMP32 + "_loop")
          .add("  ; if we get here, it means everything was equal, z flag set.")
          .add(COMP32 + "_end:")
          .add("  pop H")
          .add("  pop D")
          .add("  pop B")
          .add("  ret")
          .build();

  private static final List<String> SUB32_CODE =
      ImmutableList.<String>builder()
          .add("\n; Subtracts 4 bytes BC=BC-HL")
          .add("; Destroys: A")
          .add(SUB32 + ":")
          .add("  ana A  ; clear carry")
          .add("  push B")
          .add("  push D")
          .add("  push H")
          .add("  mvi D, 0x04  ; subtract 4 bytes")
          .add(SUB32 + "_loop:  ldax B ; loads acc from [bc]")
          .add("  sbb M ; subtracts the contents at [hl] from acc, with borrow")
          .add("  stax B; stores acc into [bc]")
          .add("  inx H ; next higher byte")
          .add("  inx B ; next higher byte")
          .add("  dcr D ; index")
          .add("  jnz " + SUB32 + "_loop")
          .add("  pop H")
          .add("  pop D")
          .add("  pop B")
          .add("  ret")
          .build();

  private static final List<String> ADD32_CODE =
      ImmutableList.<String>builder()
          .add("\n; Adds 4 bytes BC=BC+HL")
          .add("; Destroys: A")
          .add(ADD32 + ":")
          .add("  ana A  ; clear carry")
          .add("  push B")
          .add("  push D")
          .add("  push H")
          .add("  mvi D, 0x04  ; add 4 bytes")
          .add(ADD32 + "_loop:  ldax B ; loads acc from [BC]")
          .add("  adc M ; adds the contents at [HL] with from acc, with borrow")
          .add("  stax B; stores acc into [BC]")
          .add("  inx H ; next higher byte")
          .add("  inx B ; next higher byte")
          .add("  dcr D ; index")
          .add("  jnz " + ADD32 + "_loop")
          .add("  pop H")
          .add("  pop D")
          .add("  pop B")
          .add("  ret")
          .build();

  private static ImmutableList<String> makeSimple32Bit(String name, String instr) {
    return ImmutableList.<String>builder()
        .add("\n; " + instr + " 4 bytes BC=BC " + instr + " HL")
        .add("; Destroys: A")
        .add(name + ":")
        .add("  push B")
        .add("  push D")
        .add("  push H")
        .add("  mvi D, 0x04  ; counter (4 bytes)")
        .add(name + "_loop:  ldax B ; load acc from [BC]")
        .add("  " + instr + " M  ; mutate BC[d]=BC[d] (op) HL[d]")
        .add("  stax B; store acc into [BC]")
        .add("  inx H ; next higher byte")
        .add("  inx B ; next higher byte")
        .add("  dcr D ; counter")
        .add("  jnz " + name + "_loop")
        .add("  pop H")
        .add("  pop D")
        .add("  pop B")
        .add("  ret")
        .build();
  }

  private static final List<String> AND32_CODE =
      makeSimple32Bit(AND32, "ana");
  private static final List<String> OR32_CODE =
      makeSimple32Bit(OR32, "ora");
  private static final List<String> XOR32_CODE =
      makeSimple32Bit(XOR32, "xra");
  private static final List<String> NOT32_CODE =
      ImmutableList.<String>builder()
          .add("\n; Nots 4 bytes BC=~HL")
          .add("; Destroys: A")
          .add(NOT32 + ":")
          .add("  push B")
          .add("  push D")
          .add("  push H")
          .add("  mvi D, 0x04  ; 4 bytes")
          .add(NOT32 + "_loop:  mov A, M ; loads acc from [HL]")
          .add("  cma   ; 1-s complement")
          .add("  stax B; stores acc into [BC]")
          .add("  inx H ; next higher byte")
          .add("  inx B ; next higher byte")
          .add("  dcr D ; index")
          .add("  jnz " + NOT32 + "_loop")
          .add("  pop H")
          .add("  pop D")
          .add("  pop B")
          .add("  ret")
          .build();

  private static final ImmutableList<String> MULT8_CODE =
      ImmutableList.<String>builder()
          .add("\n; A=C*D")
          .add("; C has first number (shifting left), destroyed")
          .add("; D has second number (shifting right), destroyed")
          .add("; Destroys: A")
          .add(MULT8 + ":")
          .add("  push D")
          .add("  push B")
          .add("  mvi B, 0x08  ; index/counter/number of bits")
          .add("  mvi E, 0x00  ; result")
          .add(MULT8 + "_loop: mov A, D  ; shifting D right")
          .add("  ana A  ; clear carry")
          .add("  rar   ; shift right")
          .add("  mov D, A ; store it")
          .add("  jnc " + MULT8 + "_skipadd")
          .add("  ; bit was 1, so we add")
          .add("  cmc  ; clear carry (we know it was set)")
          .add("  mov A, C  ; get first number")
          .add("  add E   ; running total in e")
          .add("  mov E, A")
          .add(MULT8 + "_skipadd:  ; we didn't have to add. ")
          .add("  ; shift c left")
          .add("  mov A, C")
          .add("  ral")
          .add("  mov C, A")
          .add("  dcr B ; dcr sets z")
          .add("  jnz " + MULT8 + "_loop")
          .add("  mov A, E")
          .add("  pop B")
          .add("  pop D")
          .add("  ret")
          .build();

  private static final ImmutableList<String> PRINT8_CODE =
      ImmutableList.<String>builder()
          .add("\n; Print the byte in A")
          .add("; Destroys: A")
          .add(PRINT8 + ":")
          .add("  push H")
          .add("  mvi H, 0x00")
          .add("  mov L, A")
          .add("  cpi 0x00  ; check for negative")
          // if negative, fix A
          .add("  jp " + PRINT8 + "_positive")
          .add("  ; negates A")
          // negate it and recopy to L
          .add("  cma")
          .add("  inr A")
          .add("  mov L, A")
          .add("  mvi A, 0x2d  ; minus sign")
          .add("  call 0x0020  ; print a minus sign before the negative byte")
          .add(PRINT8 + "_positive:")
          .add("  call 0x39D4  ; print the ASCII value of the number in HL (destroys all)")
          .add("  pop H")
          .add("  ret")
          .build();

  private static final ImmutableList<String> SHIFT_LEFT8_CODE =
      ImmutableList.<String>builder()
          .add("\n; Shift A left D times. (Shifts in a 0)")
          .add("; Destroys: D")
          .add("; Sets: Carry if carried out")
          .add(SHIFT_LEFT8 + ":")
          .add("  add A  ; sets carry")
          .add("  dcr D")
          .add("  jnz " + SHIFT_LEFT8)
          .add("  ret")
          .build();

  private static final ImmutableList<String> SHIFT_LEFT32_CODE =
      ImmutableList.<String>builder()
          .add("\n; Shifts HL left once.")
          .add("; Destroys: A")
          .add("; Sets: Carry if carried out")
          .add("; ALWAYS SHIFTS *IN* CARRY. It is the responsibility of the caller")
          .add("; to clear or set the carry as needed.")
          .add(SHIFT_LEFT32 + ":")
          .add("  push H")
          .add("  push D")
          .add("  mvi D, 0x04 ; # of bytes to shift")
          .add(SHIFT_LEFT32 + "_loop:")
          .add("  mov A, M")
          .add("  ral ; shifts in carry, shifts out carry")
          .add("  mov M, A")
          .add("  inx H")
          .add("  dcr D")
          .add("  jnz " + SHIFT_LEFT32 + "_loop")
          .add("  pop D")
          .add("  pop H")
          .add("  ret")
          .build();

  private static final ImmutableList<String> SHIFT_RIGHT8_CODE =
      ImmutableList.<String>builder()
          .add("\n; Shift A right D times. Shifts in a 1 for negative numbers, 0 for positive.")
          .add("; Destroys: D")
          .add("; Sets: Carry if carried out")
          .add(SHIFT_RIGHT8 + ":")
          .add("  cpi 0x00")
          .add("  jm " + SHIFT_RIGHT8 + "_negative")
          // shift right "d" times
          .add(SHIFT_RIGHT8 + "_positive:")
          // positive: always clear carry
          .add("  ana A  ; clear carry")
          .add("  rar")
          .add("  dcr D")
          .add("  rz   ; return when D is 0")
          .add("  jmp " + SHIFT_RIGHT8 + "_positive")
          .add(SHIFT_RIGHT8 + "_negative:")
          // negative: always set carry
          .add("  stc")
          .add("  rar")
          .add("  dcr D")
          .add("  rz   ; return when D is 0")
          .add("  jmp " + SHIFT_RIGHT8 + "_negative")
          .build();

  private static final ImmutableList<String> SHIFT_RIGHT32_CODE =
      ImmutableList.<String>builder()
          .add("\n; Shifts the 32 bits pointed at HL one bit right. If HL points to a negative")
          .add("; number, it shifts in a 1")
          .add("; Destroys: A")
          .add("; Sets: carry if carried out, clears carry if no carry out")
          .add(SHIFT_RIGHT32 + ":")
          .add("  push D")
          .add("  push H")
          .add("  mvi D, 0x04")
          .add("  inx H")
          .add("  inx H")
          .add("  inx H ; move to highest byte")
          .add("  stc ; shift in a 1 to retain the negativeness")
          .add("  mov A, M   ; get high byte")
          .add("  cpi 0x00")
          .add("  jm " + SHIFT_RIGHT32 + "_loop")
          .add("  cmc ; it's positive, so clear carry so we shift in a 0.")
          .add(SHIFT_RIGHT32 + "_loop:")
          .add("  mov A, M")
          .add("  rar  ; shifts in carry, shifts out carry")
          .add("  mov M, A")
          .add("  dcx H")
          .add("  dcr D")
          .add("  ; dcr doesn't affect carry so the carry out is retained from rar")
          .add("  jnz " + SHIFT_RIGHT32 + "_loop")
          .add("  pop H")
          .add("  pop D")
          .add("  ret")
          .build();

  private static final ImmutableList<String> MULT32_CODE =
      ImmutableList.<String>builder()
          .add("; BC=BC*HL")
          .add("; Destroys: A")
          .add("; Temps:")
          .add("; 1. LEFT_TEMP: For shifting BC left")
          .add("; 2. RIGHT_TEMP: For shifting HL right")
          .add("; 3. MULT_TEMP: For adding/intermediate result")
          .add(MULT32 + ":")
          .add("  push B")
          .add("  push H")
          .add("  lxi H, LEFT_TEMP")
          .add("  call " + COPY32 + "  ; copies from BC to HL (from old BC (left) to LEFT_TEMP)")
          // surely this can be done with fewer pushes & pops
          .add("  pop B		; BC = old HL")
          .add("  push B	; put HL back on the stack")
          .add("  lxi H, RIGHT_TEMP")
          .add("  call " + COPY32 + "  ; copies from BC to HL (from old HL (right) to RIGHT_TEMP)")
          .add("  mvi D, 0x20	; index/number of bits (32)")
          .add("  lxi H, 0x0000")
          .add("  shld MULT_TEMP  ; clear mult temp")
          .add("  shld MULT_TEMP + 0x02")
          .add(MULT32 + "_loop:")
          .add("	lxi H, RIGHT_TEMP")
          .add("  call " + SHIFT_RIGHT32 + " ; shift 'right' 1 bit right. Always shifts in 0")
          .add("  jnc " + MULT32 + "_skipadd")
          .add("  ; bit was 1, so we add: temp=temp+left")
          .add("  lxi B, MULT_TEMP")
          .add("  lxi H, LEFT_TEMP")
          .add("  call " + ADD32 + "  ; bc=bc+hl / temp=temp+left")
          .add(MULT32 + "_skipadd:")
          .add("  ana A  ; clear carry so we shift in a 0")
          .add("  ; shift left")
          .add("  lxi H, LEFT_TEMP")
          .add("  call " + SHIFT_LEFT32)
          .add("  dcr D	 ; dcr sets z")
          .add("  jnz " + MULT32 + "_loop")
          // set BC=TEMP, HL to original BC
          // surely this can be done with fewer pushes & pops
          .add("  pop B  ; BC = old HL")
          .add("  pop H  ; HL = old BC")
          .add("  push H  ; push old BC")
          .add("  push B  ; push old HL")
          .add("  lxi B, MULT_TEMP  ; source")
          // copy from TEMP to (old) BC
          .add("  call " + COPY32 + "  ; copy from TEMP to original BC. ")
          .add("  pop B")
          .add("  pop H")
          .add("  ret")
          .build();

  private static final ImmutableList<String> INC32_CODE =
      ImmutableList.<String>builder()
          .add("\n; Increment the 4 bytes at BC")
          .add("; Destroys: A, D")
          .add(INC32 + ":")
          .add("  push B")
          .add("  stc")
          .add("  mvi D, 0x04  ; counter")
          .add(INC32 + "_loop:")
          .add("  ldax B")
          .add("  aci 0x00 ; adds with carry, sets carry ")
          .add("  stax B")
          // if there's no carry anymore we can stop now
          .add("  jnc " + INC32 + "_end")
          .add("  inx B")
          .add("  dcr D")
          .add("  jnz " + INC32 + "_loop")
          .add(INC32 + "_end:")
          .add("  pop B")
          .add("  ret")
          .build();

  private static final ImmutableList<String> DEC32_CODE =
      ImmutableList.<String>builder()
          .add("\n; Decrement the 4 bytes at BC")
          .add("; Destroys A, D")
          .add(DEC32 + ":")
          .add("  push B")
          .add("  stc")
          .add("  mvi D, 0x04  ; counter")
          .add(DEC32 + "_loop:")
          .add("  ldax B")
          .add("  sbi 0x00 ; subtract with carry, sets carry")
          .add("  stax B")
          // if there's still a carry we can stop now
          .add("  jnc " + DEC32 + "_end")
          .add("  inx B")
          .add("  dcr D")
          .add("  jnz " + DEC32 + "_loop")
          .add(DEC32 + "_end:")
          .add("  pop B")
          .add("  ret")
          .build();

  // This is ridiculously long
  private static final ImmutableList<String> DIV8_CODE =
      ImmutableList.<String>builder()
          .add("\n; A=C/D")
          .add("; B has bit counter")
          .add("; E stores running total")
          .add("; H stores shifted version of C (not destroyed)")
          .add("; Destroys: A, B, C, E")
          .add(DIV8 + ":")
          .add("  push H")
          .add("  mvi B, 0x09	 ; number of bits, plus 1 for setup")
          .add("  mvi E, 0x00	 ; result")
          .add("  mov H, E     ; shifted version of C")
          .add(DIV8 + "_loop:")
          .add("  mov A, H")
          .add("  cmp D  ; compare H and D")
          .add("  jnc " + DIV8 + "_subtract") // TODO: switch this to jc _nextbit?
          .add("  ; h < d: shift an 0 into e (answer)")
          .add("  ana A  ; clear carry; shift an 0 into e (answer)")
          .add("  jmp " + DIV8 + "_nextbit")
          .add(DIV8 + "_subtract:")
          .add("  ; H >= D; subtract and shift a 1")
          .add("  sub D")
          .add("  mov H, A  ; H = H - D")
          .add("  stc  ;	Shift a 1 into E")
          .add(DIV8 + "_nextbit:")
          .add("  mov A, E")
          .add("  ral	  ;	Shift possible carry into e (answer)")
          .add("  mov E, A")
          .add("  ana A  ; clear carry")
          .add("  ; Shift C left by 1 bit")
          .add("  mov A, C")
          .add("  ral")
          .add("  mov C, A")
          .add("  ; Shift carry into H")
          .add("  mov A, H")
          .add("  ral")
          .add("  mov H, A")
          .add("  dcr B")
          .add("  jnz " + DIV8 + "_loop")
          .add("  mov A, E")
          .add("  pop H")
          .add("  ret")
          .build();

  /** Map between the name and the code. */
  public static final ImmutableMap<String, List<String>> routines =
      ImmutableMap.<String, List<String>>builder()
          .put(PRINT32, PRINT32_CODE)
          .put(PRINT8, PRINT8_CODE)
          .put(COPY32, COPY32_CODE)
          .put(SUB32, SUB32_CODE)
          .put(COMP32, COMP32_CODE)
          .put(ADD32, ADD32_CODE)
          .put(MULT8, MULT8_CODE)
          .put(MULT32, MULT32_CODE)
          .put(SHIFT_LEFT8, SHIFT_LEFT8_CODE)
          .put(SHIFT_RIGHT8, SHIFT_RIGHT8_CODE)
          .put(SHIFT_LEFT32, SHIFT_LEFT32_CODE)
          .put(SHIFT_RIGHT32, SHIFT_RIGHT32_CODE)
          .put(INC32, INC32_CODE)
          .put(DEC32, DEC32_CODE)
          .put(DIV8, DIV8_CODE)
          .put(AND32, AND32_CODE)
          .put(OR32, OR32_CODE)
          .put(XOR32, XOR32_CODE)
          .put(NOT32, NOT32_CODE)
          .build();

  private static final Map<TokenType, String> LOOKUP_BY_TYPE =
      ImmutableMap.of(
          TokenType.BIT_AND, AND32,
          TokenType.BIT_OR, OR32,
          TokenType.BIT_XOR, XOR32);

  public static String lookupSimple(TokenType operator) {
    return LOOKUP_BY_TYPE.get(operator);
  }
}
