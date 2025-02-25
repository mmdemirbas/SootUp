package sootup.java.core;

import java.util.List;
import java.util.function.Function;
import javax.annotation.Nonnull;
import sootup.core.SourceTypeSpecifier;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.inputlocation.ClassLoadingOptions;
import sootup.java.core.language.JavaLanguage;
import sootup.java.core.views.JavaModuleView;

public class JavaModuleProject extends JavaProject {

  @Nonnull private final List<ModuleInfoAnalysisInputLocation> moduleInfoAnalysisInputLocation;

  public JavaModuleProject(
      JavaLanguage language,
      @Nonnull List<AnalysisInputLocation<? extends JavaSootClass>> inputLocations,
      @Nonnull List<ModuleInfoAnalysisInputLocation> moduleInputLocations,
      @Nonnull SourceTypeSpecifier sourceTypeSpecifier) {
    super(language, inputLocations, sourceTypeSpecifier);
    moduleInfoAnalysisInputLocation = moduleInputLocations;
  }

  @Nonnull
  @Override
  public JavaModuleIdentifierFactory getIdentifierFactory() {
    return JavaModuleIdentifierFactory.getInstance();
  }

  @Override
  public void validate() {
    if (getInputLocations().isEmpty() && getModuleInfoAnalysisInputLocation().isEmpty()) {
      throw new IllegalArgumentException(
          "The inputLocations collection for classPath and modulePath must not be empty.");
    }
  }

  @Nonnull
  public List<ModuleInfoAnalysisInputLocation> getModuleInfoAnalysisInputLocation() {
    return moduleInfoAnalysisInputLocation;
  }

  @Nonnull
  @Override
  public JavaModuleView createOnDemandView() {
    return new JavaModuleView(this);
  }

  @Nonnull
  @Override
  public JavaModuleView createOnDemandView(
      @Nonnull
          Function<AnalysisInputLocation<? extends JavaSootClass>, ClassLoadingOptions>
              classLoadingOptionsSpecifier) {
    return new JavaModuleView(this, classLoadingOptionsSpecifier);
  }
}
