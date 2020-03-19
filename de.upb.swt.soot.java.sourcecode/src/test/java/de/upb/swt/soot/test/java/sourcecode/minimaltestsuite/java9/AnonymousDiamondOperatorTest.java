package de.upb.swt.soot.test.java.sourcecode.minimaltestsuite.java9;

import de.upb.swt.soot.core.signatures.MethodSignature;
import de.upb.swt.soot.test.java.sourcecode.minimaltestsuite.MinimalSourceTestSuiteBase;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Ignore;
import org.junit.Test;

/** @author Kaustubh Kelkar */
public class AnonymousDiamondOperatorTest extends MinimalSourceTestSuiteBase {

  @Override
  public MethodSignature getMethodSignature() {
    return identifierFactory.getMethodSignature(
        "innerClassDiamond", getDeclaredClassSignature(), "void", Collections.emptyList());
  }

  @Test
  public void defaultTest() {}

  @Ignore
  public void ignoreTest() {
    /** TODO Check for anonymous declarations once Java 9 is supported in WALA */
  }

  @Override
  public List<String> expectedBodyStmts() {
    return Stream.of(
            "r0 := @this: AnonymousDiamondOperator",
            "$r1 = new java.io.BufferedReader",
            "$r2 = new java.io.FileReader",
            "specialinvoke $r2.<java.io.FileReader: void <init>(java.lang.String)>(\"file.txt\")",
            "specialinvoke $r1.<java.io.BufferedReader: void <init>(java.io.Reader)>($r2)",
            "$r3 = \"\"",
            "label1:",
            "$r4 = virtualinvoke $r1.<java.io.BufferedReader: java.lang.String readLine()>()",
            "goto label2",
            "$r5 := @caughtexception",
            "virtualinvoke $r1.<java.io.BufferedReader: void close()>()",
            "throw $r5",
            "label2:",
            "$r3 = $r4",
            "$z0 = $r4 != null",
            "if $z0 == 0 goto label3",
            "$r6 = <java.lang.System: java.io.PrintStream out>",
            "virtualinvoke $r6.<java.io.PrintStream: void println(java.lang.String)>($r3)",
            "goto label1",
            "label3:",
            "virtualinvoke $r1.<java.io.BufferedReader: void close()>()",
            "return")
        .collect(Collectors.toCollection(ArrayList::new));
  }
}