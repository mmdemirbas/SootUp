package sootup.callgraph;

import static junit.framework.TestCase.*;

import categories.Java8Test;
import java.util.*;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import sootup.core.signatures.MethodSignature;
import sootup.core.typehierarchy.TypeHierarchy;
import sootup.java.core.views.JavaView;

/**
 * Input source examples taken from https://bitbucket.org/delors/jcg/src/master/
 *
 * @author Markus Schmidt
 */
@Category(Java8Test.class)
public class ClassHierarchyAnalysisAlgorithmTest
    extends CallGraphTestBase<ClassHierarchyAnalysisAlgorithm> {

  // TODO: StaticInitializers, Lambdas ?

  @Override
  protected ClassHierarchyAnalysisAlgorithm createAlgorithm(
      JavaView view, TypeHierarchy typeHierarchy) {
    return new ClassHierarchyAnalysisAlgorithm(view, typeHierarchy);
  }

  @Test
  public void testMiscExample1() {
    /** We expect constructors for B and C We expect A.print(), B.print(), C.print(), D.print() */
    CallGraph cg = loadCallGraph("Misc", "example1.Example");

    MethodSignature constructorB =
        identifierFactory.getMethodSignature(
            identifierFactory.getClassType("example1.B"),
            "<init>",
            "void",
            Collections.emptyList());

    MethodSignature constructorC =
        identifierFactory.getMethodSignature(
            identifierFactory.getClassType("example1.C"),
            "<init>",
            "void",
            Collections.emptyList());

    MethodSignature methodA =
        identifierFactory.getMethodSignature(
            identifierFactory.getClassType("example1.A"),
            "print",
            "void",
            Collections.singletonList("java.lang.Object"));

    MethodSignature methodB =
        identifierFactory.getMethodSignature(
            identifierFactory.getClassType("example1.B"),
            "print",
            "void",
            Collections.singletonList("java.lang.Object"));

    MethodSignature methodC =
        identifierFactory.getMethodSignature(
            identifierFactory.getClassType("example1.C"),
            "print",
            "void",
            Collections.singletonList("java.lang.Object"));

    MethodSignature methodD =
        identifierFactory.getMethodSignature(
            identifierFactory.getClassType("example1.D"),
            "print",
            "void",
            Collections.singletonList("java.lang.Object"));

    assertTrue(cg.containsCall(mainMethodSignature, constructorB));
    assertTrue(cg.containsCall(mainMethodSignature, constructorC));

    assertTrue(cg.containsCall(mainMethodSignature, methodA));
    assertTrue(cg.containsCall(mainMethodSignature, methodB));
    assertTrue(cg.containsCall(mainMethodSignature, methodC));
    assertTrue(cg.containsCall(mainMethodSignature, methodD));

    assertEquals(6, cg.callsFrom(mainMethodSignature).size());

    assertEquals(2, cg.callsTo(constructorB).size());
    assertEquals(1, cg.callsTo(constructorC).size());
    assertEquals(1, cg.callsTo(methodA).size());
    assertEquals(1, cg.callsTo(methodB).size());
    assertEquals(1, cg.callsTo(methodC).size());
    assertEquals(1, cg.callsTo(methodD).size());

    assertEquals(0, cg.callsFrom(methodA).size());
    assertEquals(0, cg.callsFrom(methodB).size());
    assertEquals(0, cg.callsFrom(methodC).size());
    assertEquals(0, cg.callsFrom(methodD).size());
  }
}
