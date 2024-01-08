package com.plasstech.lang.d2.codegen;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;

@RunWith(TestParameterInjector.class)
public class TrimmersTest {

  @Test
  public void trimComments_leavesLeadingSpace(
      @TestParameter(
        {
            "  MOV",
            "  MOV ",
            "  MOV  ; hi",
            "  MOV ; hi",
            "  MOV; hi",
            "  MOV; hi\n \n"
        }
      ) String line) {
    assertThat(Trimmers.trimComment(line)).isEqualTo("  MOV");
  }

  @Test
  public void trimFully(
      @TestParameter(
        {
            "  MOV",
            "  MOV ",
            "  MOV  ; hi",
            "  MOV ; hi",
            "  MOV; hi",
            "  MOV; hi\n \n"
        }
      ) String line) {
    assertThat(Trimmers.trim(line)).isEqualTo("MOV");
  }

  @Test
  public void trimComments(@TestParameter({"  ; hi", "  ; hi", "; hi"}) String line) {
    assertThat(Trimmers.trimComment(line)).isEmpty();
  }

  @Test
  public void trimComments_retainsString() {
    String input =
        "  ARRAY_INDEX_NEGATIVE_ERR: db \"Invalid index err non-negative; was %d\", 10, 0";
    assertThat(Trimmers.trimComment(input)).isEqualTo(input);
    ;
  }
}
