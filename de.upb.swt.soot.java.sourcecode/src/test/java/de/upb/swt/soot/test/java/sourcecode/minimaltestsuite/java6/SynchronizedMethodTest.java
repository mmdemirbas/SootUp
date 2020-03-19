package de.upb.swt.soot.test.java.sourcecode.minimaltestsuite.java6;

import de.upb.swt.soot.core.model.SootMethod;
import de.upb.swt.soot.core.signatures.MethodSignature;
import de.upb.swt.soot.test.java.sourcecode.minimaltestsuite.MinimalSourceTestSuiteBase;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SynchronizedMethodTest extends MinimalSourceTestSuiteBase {

  public MethodSignature getMethodSignature() {
    return identifierFactory.getMethodSignature(
        "run", getDeclaredClassSignature(), "void", Collections.emptyList());
  }

  @org.junit.Test
  public void defaultTest() {
    SootMethod method = loadMethod(getMethodSignature());
    assertJimpleStmts(method, expectedBodyStmts());
    /** TODO assertTrue(method.isSynchronized()); */
  }

  @Override
  public List<String> expectedBodyStmts() {
    return Stream.of(
            "r0 := @this: SynchronizedMethod",
            "$r1 = r0.<SynchronizedMethod: SenderMethod sender>",
            "$r2 = r0.<SynchronizedMethod: java.lang.String msg>",
            "virtualinvoke $r1.<SenderMethod: void send(java.lang.String)>($r2)",
            "return")
        .collect(Collectors.toCollection(ArrayList::new));
  }
}