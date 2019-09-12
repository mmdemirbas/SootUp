package de.upb.soot.types;

import javax.annotation.Nonnull;

public class GenericTypeConstraint {

  private GenericTypeConstraint() {}

  public static class Exact extends GenericTypeConstraint {
    @Nonnull private final JavaClassType type;

    public Exact(@Nonnull JavaClassType type) {
      this.type = type;
    }

    @Nonnull
    public JavaClassType getType() {
      return type;
    }
  }

  public static class Super extends GenericTypeConstraint {
    @Nonnull private final JavaClassType extendingType;

    public Super(@Nonnull JavaClassType extendingType) {
      this.extendingType = extendingType;
    }

    @Nonnull
    public JavaClassType getExtendingType() {
      return extendingType;
    }
  }

  public static class Extends extends GenericTypeConstraint {
    @Nonnull private final String name;

    @Nonnull private final JavaClassType extendedType;

    public Extends(@Nonnull String name, @Nonnull JavaClassType extendedType) {
      this.name = name;
      this.extendedType = extendedType;
    }

    @Nonnull
    public String getName() {
      return name;
    }

    @Nonnull
    public JavaClassType getExtendedType() {
      return extendedType;
    }
  }
}
