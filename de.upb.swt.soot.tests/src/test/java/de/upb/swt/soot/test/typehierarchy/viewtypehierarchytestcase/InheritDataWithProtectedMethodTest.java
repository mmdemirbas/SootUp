package de.upb.swt.soot.test.typehierarchy.viewtypehierarchytestcase;

import static org.junit.Assert.*;

import categories.Java8Test;
import de.upb.swt.soot.core.model.Body;
import de.upb.swt.soot.core.model.SootClass;
import de.upb.swt.soot.core.model.SootMethod;
import de.upb.swt.soot.core.typerhierachy.ViewTypeHierarchy;
import de.upb.swt.soot.core.types.ClassType;
import de.upb.swt.soot.core.util.Utils;
import de.upb.swt.soot.test.typehierarchy.JavaTypeHierarchyTestBase;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/** @author Hasitha Rajapakse * */
@Category(Java8Test.class)
public class InheritDataWithProtectedMethodTest extends JavaTypeHierarchyTestBase {
  @Test
  public void method() {
    ViewTypeHierarchy typeHierarchy =
        (ViewTypeHierarchy) customTestWatcher.getView().getTypeHierarchy();
    ClassType sootClassType = getClassType(customTestWatcher.getClassName());

    assertEquals(typeHierarchy.superClassOf(sootClassType), getClassType("SuperClass"));
    assertTrue(typeHierarchy.isSubtype(getClassType("SuperClass"), sootClassType));

    SootClass<?> sootClass =
        customTestWatcher
            .getView()
            .getClass(
                customTestWatcher
                    .getView()
                    .getIdentifierFactory()
                    .getClassType(customTestWatcher.getClassName()))
            .get();
    SootMethod sootMethod =
        sootClass
            .getMethod(
                identifierFactory
                    .getMethodSignature("method", sootClassType, "void", Collections.emptyList())
                    .getSubSignature())
            .get();
    Body body = sootMethod.getBody();
    assertNotNull(body);

    List<String> actualStmts = Utils.bodyStmtsAsStrings(body);
    List<String> expectedStmts =
        Stream.of(
                "r0 := @this: InheritDataWithProtectedMethod",
                "$i0 = specialinvoke r0.<SuperClass: int getnum()>()",
                "return")
            .collect(Collectors.toList());

    assertEquals(expectedStmts, actualStmts);
  }
}