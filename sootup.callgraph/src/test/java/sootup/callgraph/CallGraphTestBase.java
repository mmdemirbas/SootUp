package sootup.callgraph;

import static junit.framework.TestCase.*;

import java.util.Collections;
import java.util.Optional;
import junit.framework.TestCase;
import org.junit.Ignore;
import org.junit.Test;
import sootup.core.model.SootClass;
import sootup.core.model.SootMethod;
import sootup.core.signatures.MethodSignature;
import sootup.core.typehierarchy.TypeHierarchy;
import sootup.core.typehierarchy.ViewTypeHierarchy;
import sootup.core.types.ClassType;
import sootup.java.bytecode.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.core.JavaIdentifierFactory;
import sootup.java.core.JavaProject;
import sootup.java.core.language.JavaLanguage;
import sootup.java.core.types.JavaClassType;
import sootup.java.core.views.JavaView;
import sootup.java.sourcecode.inputlocation.JavaSourcePathAnalysisInputLocation;

public abstract class CallGraphTestBase<T extends AbstractCallGraphAlgorithm> {

  private T algorithm;
  protected String testDirectory, className;
  protected JavaIdentifierFactory identifierFactory = JavaIdentifierFactory.getInstance();
  protected JavaClassType mainClassSignature;
  protected MethodSignature mainMethodSignature;

  protected abstract T createAlgorithm(JavaView view, TypeHierarchy typeHierarchy);

  // private static Map<String, JavaView> viewToClassPath = new HashMap<>();

  private JavaView createViewForClassPath(String classPath) {
    JavaProject javaProject =
        JavaProject.builder(new JavaLanguage(8))
            .addInputLocation(
                new JavaClassPathAnalysisInputLocation(
                    System.getProperty("java.home") + "/lib/rt.jar"))
            .addInputLocation(new JavaSourcePathAnalysisInputLocation(classPath))
            .build();
    return javaProject.createOnDemandView();
  }

  CallGraph loadCallGraph(String testDirectory, String className) {
    double version = Double.parseDouble(System.getProperty("java.specification.version"));
    if (version > 1.8) {
      fail("The rt.jar is not available after Java 8. You are using version " + version);
    }

    String classPath = "src/test/resources/callgraph/" + testDirectory;

    // JavaView view = viewToClassPath.computeIfAbsent(classPath, this::createViewForClassPath);
    JavaView view = createViewForClassPath(classPath);

    mainClassSignature = identifierFactory.getClassType(className);
    mainMethodSignature =
        identifierFactory.getMethodSignature(
            mainClassSignature, "main", "void", Collections.singletonList("java.lang.String[]"));

    SootClass<?> sc = view.getClass(mainClassSignature).get();
    Optional<SootMethod> m =
        (Optional<SootMethod>) sc.getMethod(mainMethodSignature.getSubSignature());
    assertTrue(mainMethodSignature + " not found in classloader", m.isPresent());

    final ViewTypeHierarchy typeHierarchy = new ViewTypeHierarchy(view);
    algorithm = createAlgorithm(view, typeHierarchy);
    CallGraph cg = algorithm.initialize(Collections.singletonList(mainMethodSignature));

    assertTrue(
        mainMethodSignature + " is not found in CallGraph", cg.containsMethod(mainMethodSignature));
    assertNotNull(cg);
    return cg;
  }

  @Test
  public void testSingleMethod() {
    CallGraph cg = loadCallGraph("Misc", "example.SingleMethod");
    assertEquals(0, cg.callCount());
    assertEquals(0, cg.callsTo(mainMethodSignature).size());
    assertEquals(0, cg.callsFrom(mainMethodSignature).size());
  }

  @Test
  public void testAddClass() {
    CallGraph cg = loadCallGraph("Misc", "update.operation.cg.Class");

    MethodSignature methodSignature =
        identifierFactory.getMethodSignature(
            identifierFactory.getClassType("update.operation.cg.Class"),
            "method",
            "void",
            Collections.emptyList());

    JavaClassType newClass =
        new JavaClassType("AdderA", identifierFactory.getPackageName("update.operation.cg"));
    CallGraph newCallGraph = algorithm.addClass(cg, newClass);

    TestCase.assertEquals(0, cg.callsTo(mainMethodSignature).size());
    TestCase.assertEquals(1, newCallGraph.callsTo(mainMethodSignature).size());

    TestCase.assertEquals(1, cg.callsTo(methodSignature).size());
    TestCase.assertEquals(3, newCallGraph.callsTo(methodSignature).size());
  }

  @Test
  public void testRecursiveCall() {
    CallGraph cg = loadCallGraph("Misc", "recur.Class");

    MethodSignature method =
        identifierFactory.getMethodSignature(
            mainClassSignature, "method", "void", Collections.emptyList());

    MethodSignature uncalledMethod =
        identifierFactory.getMethodSignature(
            mainClassSignature, "method", "void", Collections.singletonList("int"));

    assertTrue(cg.containsMethod(mainMethodSignature));
    assertTrue(cg.containsMethod(method));
    assertFalse(cg.containsMethod(uncalledMethod));
    TestCase.assertEquals(2, cg.getMethodSignatures().size());

    assertTrue(cg.containsCall(mainMethodSignature, mainMethodSignature));
    assertTrue(cg.containsCall(mainMethodSignature, method));
    TestCase.assertEquals(2, cg.callsFrom(mainMethodSignature).size());
  }

  @Test
  public void testNonVirtualCall1() {
    CallGraph cg = loadCallGraph("NonVirtualCall", "nvc1.Class");
    MethodSignature targetMethod =
        identifierFactory.getMethodSignature(
            mainClassSignature, "method", "void", Collections.emptyList());
    assertTrue(cg.containsCall(mainMethodSignature, targetMethod));
  }

  @Test
  public void testNonVirtualCall2() {
    CallGraph cg = loadCallGraph("NonVirtualCall", "nvc2.Class");
    MethodSignature targetMethod =
        identifierFactory.getMethodSignature(
            mainClassSignature, "<init>", "void", Collections.emptyList());
    assertTrue(cg.containsCall(mainMethodSignature, targetMethod));
  }

  @Test
  public void testNonVirtualCall3() {
    CallGraph cg = loadCallGraph("NonVirtualCall", "nvc3.Class");
    MethodSignature targetMethod =
        identifierFactory.getMethodSignature(
            mainClassSignature, "method", "void", Collections.emptyList());
    MethodSignature uncalledMethod =
        identifierFactory.getMethodSignature(
            mainClassSignature, "method", "void", Collections.singletonList("int"));
    assertTrue(cg.containsCall(mainMethodSignature, targetMethod));
    assertFalse(cg.containsMethod(uncalledMethod));
  }

  @Test
  public void testNonVirtualCall4() {
    CallGraph cg = loadCallGraph("NonVirtualCall", "nvc4.Class");
    MethodSignature firstMethod =
        identifierFactory.getMethodSignature(
            mainClassSignature, "method", "void", Collections.emptyList());
    assertTrue(cg.containsCall(mainMethodSignature, firstMethod));

    MethodSignature targetMethod =
        identifierFactory.getMethodSignature(
            identifierFactory.getClassType("nvc4.Rootclass"),
            "method",
            "void",
            Collections.emptyList());
    assertTrue(cg.containsCall(firstMethod, targetMethod));
  }

  @Test
  public void testNonVirtualCall5() {
    CallGraph cg = loadCallGraph("NonVirtualCall", "nvc5.Demo");

    MethodSignature firstMethod =
        identifierFactory.getMethodSignature(
            identifierFactory.getClassType("nvc5.Sub"), "method", "void", Collections.emptyList());
    assertTrue(cg.containsCall(mainMethodSignature, firstMethod));

    MethodSignature targetMethod =
        identifierFactory.getMethodSignature(
            identifierFactory.getClassType("nvc5.Middle"),
            "method",
            "void",
            Collections.emptyList());
    assertTrue(cg.containsCall(firstMethod, targetMethod));
  }

  @Test
  public void testVirtualCall1() {
    CallGraph cg = loadCallGraph("VirtualCall", "vc1.Class");

    MethodSignature targetMethod =
        identifierFactory.getMethodSignature(
            mainClassSignature, "target", "void", Collections.emptyList());
    assertTrue(cg.containsCall(mainMethodSignature, targetMethod));
  }

  @Test
  public void testVirtualCall2() {
    CallGraph cg = loadCallGraph("VirtualCall", "vc2.Class");

    JavaClassType subClassSig = identifierFactory.getClassType("vc2.SubClass");
    MethodSignature constructorMethod =
        identifierFactory.getMethodSignature(
            subClassSig, "<init>", "void", Collections.emptyList());
    assertTrue(cg.containsCall(mainMethodSignature, constructorMethod));

    MethodSignature callMethod =
        identifierFactory.getMethodSignature(
            mainClassSignature, "callMethod", "void", Collections.singletonList("vc2.Class"));
    assertTrue(cg.containsCall(mainMethodSignature, callMethod));

    MethodSignature targetMethod =
        identifierFactory.getMethodSignature(
            identifierFactory.getClassType("vc2.Class"), "method", "void", Collections.emptyList());
    assertTrue(cg.containsCall(callMethod, targetMethod));
  }

  @Test
  public void testVirtualCall3() {
    CallGraph cg = loadCallGraph("VirtualCall", "vc3.Class");

    JavaClassType subClassSig = identifierFactory.getClassType("vc3.ClassImpl");
    MethodSignature constructorMethod =
        identifierFactory.getMethodSignature(
            subClassSig, "<init>", "void", Collections.emptyList());
    assertTrue(cg.containsCall(mainMethodSignature, constructorMethod));

    MethodSignature callMethod =
        identifierFactory.getMethodSignature(
            mainClassSignature,
            "callOnInterface",
            "void",
            Collections.singletonList("vc3.Interface"));
    assertTrue(cg.containsCall(mainMethodSignature, callMethod));

    MethodSignature targetMethod =
        identifierFactory.getMethodSignature(
            subClassSig, "method", "void", Collections.emptyList());
    assertTrue(cg.containsCall(callMethod, targetMethod));
  }

  @Test
  public void testVirtualCall4() {
    CallGraph cg = loadCallGraph("VirtualCall", "vc4.Class");

    // more precise its: declareClassSig
    MethodSignature callMethod =
        identifierFactory.getMethodSignature(
            identifierFactory.getClassType("vc4.Interface"),
            "method",
            "void",
            Collections.emptyList());
    assertTrue(cg.containsCall(mainMethodSignature, callMethod));
  }

  @Test
  public void testDynamicInterfaceMethod1() {
    CallGraph cg = loadCallGraph("InterfaceMethod", "j8dim1.Class");
    MethodSignature callMethod =
        identifierFactory.getMethodSignature(
            identifierFactory.getClassType("j8dim1.Interface"),
            "method",
            "void",
            Collections.emptyList());
    assertTrue(cg.containsCall(mainMethodSignature, callMethod));
  }

  @Test
  public void testDynamicInterfaceMethod2() {
    CallGraph cg = loadCallGraph("InterfaceMethod", "j8dim2.SuperClass");

    MethodSignature callMethod =
        identifierFactory.getMethodSignature(
            identifierFactory.getClassType("j8dim2.Interface"),
            "method",
            "void",
            Collections.emptyList());
    assertTrue(cg.containsCall(mainMethodSignature, callMethod));
  }

  @Test
  public void testDynamicInterfaceMethod3() {
    CallGraph cg = loadCallGraph("InterfaceMethod", "j8dim3.SuperClass");

    MethodSignature callMethod =
        identifierFactory.getMethodSignature(
            mainClassSignature, "method", "void", Collections.emptyList());
    assertTrue(cg.containsCall(mainMethodSignature, callMethod));
  }

  @Test
  public void testDynamicInterfaceMethod4() {
    CallGraph cg = loadCallGraph("InterfaceMethod", "j8dim4.SuperClass");

    MethodSignature callMethod =
        identifierFactory.getMethodSignature(
            identifierFactory.getClassType("j8dim4.Interface"),
            "method",
            "void",
            Collections.emptyList());
    assertTrue(cg.containsCall(mainMethodSignature, callMethod));
  }

  @Test
  public void testDynamicInterfaceMethod5() {
    CallGraph cg = loadCallGraph("InterfaceMethod", "j8dim5.SuperClass");

    MethodSignature method =
        identifierFactory.getMethodSignature(
            identifierFactory.getClassType("j8dim5.DirectInterface"),
            "method",
            "void",
            Collections.emptyList());
    assertTrue(cg.containsCall(mainMethodSignature, method));

    MethodSignature compute =
        identifierFactory.getMethodSignature(
            mainClassSignature, "compute", "void", Collections.emptyList());
    assertTrue(cg.containsCall(mainMethodSignature, compute));
  }

  @Ignore
  // TODO: WALA can't handle this case?
  public void testDynamicInterfaceMethod6() {
    CallGraph cg = loadCallGraph("InterfaceMethod", "j8dim6.Demo");

    MethodSignature combinedInterfaceMethod =
        identifierFactory.getMethodSignature(
            identifierFactory.getClassType("j8dim6.CombinedInterface"),
            "method",
            "void",
            Collections.emptyList());
    assertTrue(cg.containsCall(mainMethodSignature, combinedInterfaceMethod));

    MethodSignature method =
        identifierFactory.getMethodSignature(
            identifierFactory.getClassType("j8dim6.SomeInterface"),
            "method",
            "void",
            Collections.emptyList());
    assertTrue(cg.containsCall(combinedInterfaceMethod, method));

    MethodSignature anotherMethod =
        identifierFactory.getMethodSignature(
            identifierFactory.getClassType("j8dim6.AnotherInterface"),
            "method",
            "void",
            Collections.emptyList());
    assertTrue(cg.containsCall(combinedInterfaceMethod, anotherMethod));
  }

  @Test
  public void testStaticInterfaceMethod() {
    CallGraph cg = loadCallGraph("InterfaceMethod", "j8sim.Class");

    MethodSignature method =
        identifierFactory.getMethodSignature(
            identifierFactory.getClassType("j8sim.Interface"),
            "method",
            "void",
            Collections.emptyList());

    assertTrue(cg.containsCall(mainMethodSignature, method));
  }

  /** Ignored because takes too long to analyze whole rt.jar */
  @Ignore
  public void testHelloWorld() {
    CallGraph cg = loadCallGraph("Misc", "HelloWorld");

    ClassType clazzType = JavaIdentifierFactory.getInstance().getClassType("java.io.PrintStream");

    MethodSignature method =
        identifierFactory.getMethodSignature(
            clazzType, "println", "void", Collections.singletonList("java.lang.String"));

    assertTrue(cg.containsCall(mainMethodSignature, method));
  }
}
