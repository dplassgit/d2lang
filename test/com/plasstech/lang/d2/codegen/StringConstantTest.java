package com.plasstech.lang.d2.codegen;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class StringConstantTest {

  @Test
  public void dataEntry() {
    StringEntry entry = new StringConstant("name", "value");
    assertThat(entry.dataEntry()).isEqualTo("name: db \"value\", 0");
  }

  @Test
  public void dataEntrySlash() {
    StringEntry entry = new StringConstant("name", "\\value");
    assertThat(entry.dataEntry()).isEqualTo("name: db \"\\value\", 0");
  }

  @Test
  public void dataEntryTick() {
    StringEntry entry = new StringConstant("name", "'value'");
    assertThat(entry.dataEntry()).isEqualTo("name: db \"'value'\", 0");
  }

  @Test
  public void dataEntryQuote() {
    StringEntry entry = new StringConstant("name", "\"value");
    assertThat(entry.dataEntry()).isEqualTo("name: db 34, \"value\", 0");
  }

  @Test
  public void dataEntryLFEnd() {
    StringEntry entry = new StringConstant("name", "value\n");
    assertThat(entry.dataEntry()).isEqualTo("name: db \"value\", 10, 0");
  }

  @Test
  public void dataEntryLFBeginning() {
    StringEntry entry = new StringConstant("name", "\nvalue");
    assertThat(entry.dataEntry()).isEqualTo("name: db 10, \"value\", 0");
  }

  @Test
  public void dataEntryOneLF() {
    StringEntry entry = new StringConstant("name", "\n");
    assertThat(entry.dataEntry()).isEqualTo("name: db 10, 0");
  }

  @Test
  public void dataEntryOneQuote() {
    StringEntry entry = new StringConstant("name", "\"");
    assertThat(entry.dataEntry()).isEqualTo("name: db 34, 0");
  }

  @Test
  public void dataEntryTab() {
    StringEntry entry = new StringConstant("name", "\t");
    assertThat(entry.dataEntry()).isEqualTo("name: db 9, 0");
  }

  @Test
  public void dataEntryCR() {
    StringEntry entry = new StringConstant("name", "\r");
    assertThat(entry.dataEntry()).isEqualTo("name: db 13, 0");
  }

  @Test
  public void dataEntryMultipleLF() {
    StringEntry entry = new StringConstant("name", "\n\n");
    assertThat(entry.dataEntry()).isEqualTo("name: db 10, 10, 0");
  }

  @Test
  public void dataEntryMultiple() {
    StringEntry entry = new StringConstant("name", "\nhello\tworld\r\n");
    assertThat(entry.dataEntry()).isEqualTo("name: db 10, \"hello\", 9, \"world\", 13, 10, 0");
  }
}
