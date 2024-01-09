package com.plasstech.lang.d2.codegen.t100;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.plasstech.lang.d2.codegen.t100.Subroutine.Name;
import com.plasstech.lang.d2.common.TokenType;

class Subroutines {
  private static final String DIV32 = Name.D_div32.name();
  private static final String COPY32 = Name.D_copy32.name();
  private static final String PRINT32 = Name.D_print32.name();
  private static final String SUB32 = Name.D_sub32.name();
  private static final String COMP32 = Name.D_comp32.name();
  private static final String ADD32 = Name.D_add32.name();
  private static final String MULT8 = Name.D_mult8.name();
  private static final String MULT32 = Name.D_mult32.name();
  private static final String PRINT8 = Name.D_print8.name();
  private static final String SHIFT_LEFT8 = Name.D_shift_left8.name();
  private static final String SHIFT_RIGHT8 = Name.D_shift_right8.name();
  private static final String SHIFT_LEFT32 = Name.D_shift_left32.name();
  private static final String SHIFT_RIGHT32 = Name.D_shift_right32.name();
  private static final String INC32 = Name.D_inc32.name();
  private static final String DEC32 = Name.D_dec32.name();
  private static final String DIV8 = Name.D_div8.name();
  private static final String AND32 = Name.D_bitand32.name();
  private static final String OR32 = Name.D_bitor32.name();
  private static final String XOR32 = Name.D_bitxor32.name();
  private static final String NOT32 = Name.D_bitnot32.name();

  private static final List<String> PRINT32_CODE =
      ImmutableList.<String>builder()
          .add("\n; Prints the 4 bytes at BC")
          .add("; Destroys: A, B, C, D, E")
          .add("PLACES_1b:   db 0x00,0xca,0x9a,0x3b")
          .add("PLACES_100m: db 0x00,0xe1,0xf5,0x05")
          .add("PLACES_10m:  db 0x80,0x96,0x98,0x00")
          .add("PLACES_1m:   db 0x40,0x42,0x0f,0x00")
          .add("PLACES_100k: db 0xa0,0x86,0x01,0x00")
          .add("PLACES_10k:  db 0x10,0x27,0x00,0x00")
          .add("PLACES_1k:   db 0xe8,0x03,0x00,0x00")
          .add("PLACES_100:  db 0x64,0x00,0x00,0x00")
          .add("PLACES_10:   db 0x0a,0x00,0x00,0x00")
          .add("PLACES_1:    db 0x01,0x00,0x00,0x00")
          .add("LAST_PLACE:  db 0xff  ; sentinel")
          .add("TEMP:        db 0x00,0x00,0x00,0x00\n")
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
          .add("  mvi D, 0x04  ; copy 4 bytes")
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
          .add("  mvi D, 0x04  ; shift 4 bytes")
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
          .add("MULT_TEMP:   db 0x00,0x00,0x00,0x00")
          .add("LEFT_TEMP:   db 0x00,0x00,0x00,0x00")
          .add("RIGHT_TEMP:  db 0x00,0x00,0x00,0x00")
          .add(MULT32 + ":")
          .add("  push B")
          .add("  push H")
          .add("  lxi H, LEFT_TEMP")
          .add("  call " + COPY32 + "  ; copies from BC to HL (from old BC (left) to LEFT_TEMP)")
          // surely this can be done with fewer pushes & pops
          .add("  pop B		; BC = old HL")
          .add("  push B	; put HL back on the stack")
          .add("  lxi H, RIGHT_TEMP")
          .add("  call " + COPY32
              + "  ; copies from BC to HL (from old HL (right) to RIGHT_TEMP)")
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
          .add("; Destroys: A")
          .add(INC32 + ":")
          .add("  push B")
          .add("  push D")
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
          .add("  pop D")
          .add("  pop B")
          .add("  ret")
          .build();

  private static final ImmutableList<String> DEC32_CODE =
      ImmutableList.<String>builder()
          .add("\n; Decrement the 4 bytes at BC")
          .add("; Destroys A")
          .add(DEC32 + ":")
          .add("  push B")
          .add("  push D")
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
          .add("  pop D")
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

  private static final ImmutableList<String> DIV32_CODE =
      // Note: this routine uses regular d2 call semantics..., not BC/HL as input/output
      ImmutableList.<String>builder()
          .add("  DIV32_PARAM_num: db 0x00,0x00,0x00,0x00")
          .add("  DIV32_PARAM_denom: db 0x00,0x00,0x00,0x00")
          .add("  DIV32_RETURN_SLOT: db 0x00,0x00,0x00,0x00")
          .add("  DIV32_LOCAL_remainder: db 0x00,0x00,0x00,0x00")
          .add(DIV32 + ":")
          .add("  push B")
          .add("  push D")
          .add("  push H")
          .add("  ; answer = 0")
          .add("  lxi H, 0x0000")
          .add("  shld DIV32_RETURN_SLOT ; store low word (LSByte first)")
          .add("  shld DIV32_RETURN_SLOT + 0x02  ; store high word")
          .add("  ; remainder = 0")
          .add("  shld DIV32_LOCAL_remainder")
          .add("  shld DIV32_LOCAL_remainder + 0x02")
          .add("  mvi D, 0x21  ; D=33 / bit counter\n")
          .add(DIV32 + "_loop:")
          .add("  ; answer=answer << 1")
          .add("  lxi H, DIV32_RETURN_SLOT")
          .add("  stc")
          .add("  cmc  ; clear carry")
          .add("  call " + Name.D_shift_left32)
          .add("  ; SOURCE LINE 16: __temp16 = remainder: INT (13) > denom")
          .add("  lxi B, DIV32_LOCAL_remainder")
          .add("  lxi H, DIV32_PARAM_denom")
          .add("  call " + Name.D_comp32)
          .add("  ; if carry and zero are set: remainder > denom is false, jumps to elif_15")
          .add("  jz " + DIV32 + "_remainder_too_big")
          .add("  jc " + DIV32 + "_remainder_too_big")
          .add("  ; SOURCE LINE 18: answer: INT (4)++")
          .add("  lxi B, DIV32_RETURN_SLOT")
          .add("  call " + Name.D_inc32)
          .add("  ; remainder -= denom")
          .add("  lxi B, DIV32_LOCAL_remainder")
          .add("  lxi H, DIV32_PARAM_denom")
          .add("  call D_sub32  ; bc=bc-hl\n")
          .add(DIV32 + "_remainder_too_big:")
          .add("  ; SOURCE LINE 24: __temp20 = num & topbit: INT (9)")
          .add("  lda DIV32_PARAM_num + 0x03 ; high byte")
          .add("  ani 0x80")
          .add("  push PSW ; store hibit")
          .add("  ; num=num << 1")
          .add("  lxi H, DIV32_PARAM_num")
          .add("  stc")
          .add("  cmc  ; clear carry")
          .add("  call " + Name.D_shift_left32)
          .add("  ; remainder = remainder << 1")
          .add("  lxi H, DIV32_LOCAL_remainder")
          .add("  stc")
          .add("  cmc  ; clear carry")
          .add("  call " + Name.D_shift_left32)
          .add("  ; if hibit != 0")
          .add("  pop PSW")
          .add("  cpi 0x00")
          .add("  jz " + DIV32 + "_hi_bit_zero")
          .add("  ; remainder++")
          .add("  lxi B, DIV32_LOCAL_remainder")
          .add("  call " + Name.D_inc32)
          .add(DIV32 + "_hi_bit_zero:")
          .add("  dcr D")
          .add("  jnz D_div32_loop")
          .add("  pop H")
          .add("  pop D")
          .add("  pop B")
          .add("  ret")
          .build();

  /** Map between the name and the Subroutine. */
  private static final ImmutableSet<Subroutine> SUBROUTINE_SET =
      ImmutableSet.<Subroutine>builder()
          .add(new Subroutine(Name.D_div32, DIV32_CODE))
          .add(new Subroutine(Name.D_print32, PRINT32_CODE))
          .add(new Subroutine(Name.D_copy32, COPY32_CODE))
          .add(new Subroutine(Name.D_sub32, SUB32_CODE))
          .add(new Subroutine(Name.D_comp32, COMP32_CODE))
          .add(new Subroutine(Name.D_add32, ADD32_CODE))
          .add(new Subroutine(Name.D_mult8, MULT8_CODE))
          .add(new Subroutine(Name.D_mult32, MULT32_CODE))
          .add(new Subroutine(Name.D_print8, PRINT8_CODE))
          .add(new Subroutine(Name.D_shift_left8, SHIFT_LEFT8_CODE))
          .add(new Subroutine(Name.D_shift_right8, SHIFT_RIGHT8_CODE))
          .add(new Subroutine(Name.D_shift_left32, SHIFT_LEFT32_CODE))
          .add(new Subroutine(Name.D_shift_right32, SHIFT_RIGHT32_CODE))
          .add(new Subroutine(Name.D_inc32, INC32_CODE))
          .add(new Subroutine(Name.D_dec32, DEC32_CODE))
          .add(new Subroutine(Name.D_div8, DIV8_CODE))
          .add(new Subroutine(Name.D_bitand32, AND32_CODE))
          .add(new Subroutine(Name.D_bitor32, OR32_CODE))
          .add(new Subroutine(Name.D_bitxor32, XOR32_CODE))
          .add(new Subroutine(Name.D_bitnot32, NOT32_CODE))
          .build();

  /** Map between the name and the code. */
  private static final ImmutableMap<Name, Subroutine> SUBROUTINES =
      ImmutableMap.copyOf(SUBROUTINE_SET.stream()
          .collect(Collectors.toMap(
              Subroutine::name, Function.identity())));

  public static final Subroutine get(String name) {
    return SUBROUTINES.get(Name.valueOf(name));
  }

  public static final Subroutine get(Name name) {
    return SUBROUTINES.get(name);
  }

  private static final Map<TokenType, Name> SIMPLE_LOOKUP_BY_TYPE =
      ImmutableMap.of(
          TokenType.BIT_AND, Name.D_bitand32,
          TokenType.BIT_OR, Name.D_bitor32,
          TokenType.BIT_XOR, Name.D_bitxor32);

  public static Name lookupSimple(TokenType operator) {
    return SIMPLE_LOOKUP_BY_TYPE.get(operator);
  }
}
