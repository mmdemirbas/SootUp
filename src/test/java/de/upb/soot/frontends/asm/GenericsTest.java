package de.upb.soot.frontends.asm;

import categories.Java8Test;
import de.upb.soot.IdentifierFactory;
import de.upb.soot.Project;
import de.upb.soot.core.AbstractClass;
import de.upb.soot.frontends.AbstractClassSource;
import de.upb.soot.inputlocation.JavaClassPathAnalysisInputLocation;
import de.upb.soot.views.View;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(Java8Test.class)
public class GenericsTest {

  @Test
  public void genericsTest() {
    JavaClassPathAnalysisInputLocation input =
        new JavaClassPathAnalysisInputLocation("src/test/resources/bytecode-target/");
    Project<JavaClassPathAnalysisInputLocation> p = new Project<>(input);

    View view = p.createOnDemandView();
    IdentifierFactory factory = p.getIdentifierFactory();

    AbstractClass<? extends AbstractClassSource> genericsTest = view
        .getClass(factory.getClassType("GenericsTest")).get();

    AbstractClass<? extends AbstractClassSource> stringArrayList = view
        .getClass(factory.getClassType("GenericsTest$StringArrayList")).get();

    genericsTest.getFields();

    System.out.println();
  }
}
