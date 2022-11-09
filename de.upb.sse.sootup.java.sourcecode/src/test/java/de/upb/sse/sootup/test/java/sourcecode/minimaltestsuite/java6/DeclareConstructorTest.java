package de.upb.sse.sootup.test.java.sourcecode.minimaltestsuite.java6;

import de.upb.sse.sootup.core.model.SootMethod;
import de.upb.sse.sootup.core.signatures.MethodSignature;
import de.upb.sse.sootup.test.java.sourcecode.minimaltestsuite.MinimalSourceTestSuiteBase;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;

public class DeclareConstructorTest extends MinimalSourceTestSuiteBase {

  public MethodSignature getMethodSignatureInitOneParam() {
    return identifierFactory.getMethodSignature(
        getDeclaredClassSignature(), "<init>", "void", Collections.singletonList("int"));
  }

  public MethodSignature getMethodSignatureInitTwoParam() {
    return identifierFactory.getMethodSignature(
        getDeclaredClassSignature(), "<init>", "void", Arrays.asList("int", "int"));
  }

  @Test
  public void test() {
    SootMethod method = loadMethod(getMethodSignatureInitOneParam());
    assertJimpleStmts(method, expectedBodyStmts());
    method = loadMethod(getMethodSignatureInitTwoParam());
    assertJimpleStmts(method, expectedBodyStmts1());
  }

  /**
   *
   *
   * <pre>
   * public DeclareConstructor(int var1){
   *         this.var1=var1;
   *         this.var2=0;
   *     }
   * </pre>
   */
  @Override
  public List<String> expectedBodyStmts() {
    return Stream.of(
            "r0 := @this: DeclareConstructor",
            "$i0 := @parameter0: int",
            "specialinvoke r0.<java.lang.Object: void <init>()>()",
            "r0.<DeclareConstructor: int var1> = $i0",
            "r0.<DeclareConstructor: int var2> = 0",
            "return")
        .collect(Collectors.toCollection(ArrayList::new));
  }

  /**
   *
   *
   * <pre>
   *     public DeclareConstructor(int var1, int var2){
   *         this.var1=var1;
   *         this.var2=var2;
   *     }
   * </pre>
   */
  public List<String> expectedBodyStmts1() {
    return Stream.of(
            "r0 := @this: DeclareConstructor",
            "$i0 := @parameter0: int",
            "$i1 := @parameter1: int",
            "specialinvoke r0.<java.lang.Object: void <init>()>()",
            "r0.<DeclareConstructor: int var1> = $i0",
            "r0.<DeclareConstructor: int var2> = $i1",
            "return")
        .collect(Collectors.toCollection(ArrayList::new));
  }
}