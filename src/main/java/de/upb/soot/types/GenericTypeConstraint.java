package de.upb.soot.types;

import javax.annotation.Nonnull;

public class GenericTypeConstraint {

  private GenericTypeConstraint() {}

  /** Represents a specific type as the actual argument for a type parameter. */
  public static class Exact extends GenericTypeConstraint {
    @Nonnull private final JavaClassType type;

    public Exact(@Nonnull JavaClassType type) {
      this.type = type;
    }

    /** @see Exact */
    @Nonnull
    public JavaClassType getType() {
      return type;
    }
  }

  /** Represents a constraint of the form <code>T1 extends T2</code>. */
  public static class Extends extends GenericTypeConstraint {

    @Nonnull private final String name;

    @Nonnull private final JavaClassType extendedType;

    public Extends(@Nonnull String name, @Nonnull JavaClassType extendedType) {
      this.name = name;
      this.extendedType = extendedType;
    }

    /** A symbolic name, e.g. <code>T</code>. */
    @Nonnull
    public String getName() {
      return name;
    }

    /**
     * A type <code>T</code> must extend. If this has been omitted in Java, this becomes <code>
     * java.lang.Object</code>.
     */
    @Nonnull
    public JavaClassType getExtendedType() {
      return extendedType;
    }
  }
}
