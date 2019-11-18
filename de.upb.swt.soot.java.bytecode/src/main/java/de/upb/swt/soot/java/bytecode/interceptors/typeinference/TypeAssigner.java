package de.upb.swt.soot.java.bytecode.interceptors.typeinference;

import de.upb.swt.soot.core.jimple.basic.Local;
import de.upb.swt.soot.core.jimple.basic.Value;
import de.upb.swt.soot.core.jimple.common.expr.JCastExpr;
import de.upb.swt.soot.core.jimple.common.stmt.JAssignStmt;
import de.upb.swt.soot.core.jimple.common.stmt.Stmt;
import de.upb.swt.soot.core.model.Body;
import de.upb.swt.soot.core.transform.BodyInterceptor;
import de.upb.swt.soot.core.types.ArrayType;
import de.upb.swt.soot.core.types.ClassType;
import de.upb.swt.soot.core.types.ReferenceType;
import de.upb.swt.soot.core.types.Type;
import de.upb.swt.soot.core.util.ImmutableUtils;
import de.upb.swt.soot.java.core.typehierarchy.TypeHierarchy;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

// Implementation according to Bellamy et al., "Efficient Local Type Inference"

// The approach below might not always work. See section 3.3 of the paper.
// It outlines that two transformations might be needed sometimes:
// "The first is a particular variable-splitting transformation [...]",
// but the second transformation is not mentioned. It might be mentioned in citation [8].
// Additionally, section 5 "INFERRING JAVA SOURCE TYPES" must be implemented.

public class TypeAssigner implements BodyInterceptor {

  private static class BottomType extends Type {

    private static final BottomType instance = new BottomType();
  }
  /** Serves as a type alias */
  private static class Typing extends HashMap<Local, Type> {

    Typing() {}

    Typing(@Nonnull Map<? extends Local, ? extends Type> m) {
      super(m);
    }
  }

  @Nonnull private final TypeHierarchy hierarchy;

  public TypeAssigner(@Nonnull TypeHierarchy hierarchy) {
    this.hierarchy = hierarchy;
  }

  @Nonnull
  @Override
  public Body interceptBody(@Nonnull Body originalBody) {
    // Algorithm 2: General type inference algorithm

    Stream<JAssignStmt> localAssignments =
        originalBody.getStmts().stream()
            .filter(stmt -> stmt instanceof JAssignStmt)
            .map(stmt -> (JAssignStmt) stmt)
            .filter(assignStmt -> assignStmt.getLeftOp() instanceof Local);

    Map<Local, Set<JAssignStmt>> depends = dependentAssignments(originalBody);

    // Corresponds to σ
    Typing initialTyping = new Typing();
    originalBody.getLocals().forEach(local -> initialTyping.put(local, BottomType.instance));

    // Corresponds to Σ
    Set<Typing> typings = new HashSet<>();
    typings.add(initialTyping);

    Map<Typing, Deque<JAssignStmt>> worklists = new HashMap<>();
    worklists.put(
        initialTyping, localAssignments.collect(Collectors.toCollection(ArrayDeque::new)));

    while (true) {
      // Corresponds to σ in line 6 of the pseudocode of Algorithm 2
      Typing incompleteTyping =
          typings.stream()
              .filter(typing -> !worklists.get(typing).isEmpty())
              .findAny()
              .orElse(null);
      if (incompleteTyping == null) break;

      typings.remove(incompleteTyping);

      // Corresponds to (v := e)
      JAssignStmt assignStmt = Objects.requireNonNull(worklists.get(incompleteTyping).poll());
      Local assignmentLeft = (Local) assignStmt.getLeftOp();

      // TODO Does this set to be recomputed after each iteration of the following loop?
      Set<Type> lcas =
          leastCommonAncestors(
              ImmutableUtils.immutableSet(
                  incompleteTyping.get(assignmentLeft),
                  typeOfExprUnderTyping(incompleteTyping, assignStmt.getRightOp())),
              hierarchy);
      for (Type type : lcas) {
        // `type` corresponds to `t` in line 10
        if (type.equals(incompleteTyping.get(assignmentLeft))) {
          typings.add(incompleteTyping);
        } else {
          Typing newTyping = new Typing(incompleteTyping);
          newTyping.put(assignmentLeft, type);
          Deque<JAssignStmt> newTypingWorklist = new ArrayDeque<>(worklists.get(incompleteTyping));
          newTypingWorklist.addAll(depends.get(assignmentLeft));
          worklists.put(newTyping, newTypingWorklist);
          typings.add(newTyping);
        }
      }
    }

    // We can now pick any typing from the typings set
    Typing typing =
        typings.stream()
            .findAny()
            .orElseThrow(() -> new RuntimeException("Did not find a valid typing"));
    return applyTyping(typing, originalBody);
  }

  /**
   * Replaces all {@link Local}s in the body with typed ones according to the supplied {@link
   * Typing}.
   */
  @Nonnull
  private static Body applyTyping(@Nonnull Typing typing, @Nonnull Body originalBody) {
    Set<Local> originalLocals = originalBody.getLocals();
    List<Stmt> originalStmts = originalBody.getStmts();

    Map<Local, Local> originalLocalToTypedLocal =
        originalLocals.stream()
            .collect(
                Collectors.toMap(
                    originalLocal -> originalLocal,
                    originalLocal -> originalLocal.withType(typing.get(originalLocal))));

    List<Stmt> typedStmts =
        originalStmts.stream()
            .map(stmt -> replaceLocalsIn(stmt, originalLocalToTypedLocal))
            .collect(Collectors.toList());

    return originalBody
        .withLocals(new HashSet<>(originalLocalToTypedLocal.values()))
        .withStmts(typedStmts);
  }

  /**
   * Checks <code>stmt</code> for Locals that are found in <code>originalLocalToTypedLocal</code>,
   * copies <code>stmt</code> in this case and replaces the old Local with the new, typed Local.
   */
  @Nonnull
  private static Stmt replaceLocalsIn(
      @Nonnull Stmt stmt, @Nonnull Map<Local, Local> originalLocalToTypedLocal) {
    // TODO: Check if the `stmt` contains a local from `originalLocalToTypedLocal`.
    //   If one is found, the stmts needs to be copied and the old Local replaced with the typed
    //   Local from the map.
    //   This needs to handle all combinations from *Stmt and *Expr types. This is not ideal
    //   and architectural changes should be evaluated to make this easier. Immutability
    //   is increasing the code complexity a lot here. A simple `Local.setType(newType)` would
    //   have avoided this, but is currently not possible without breaking immutability guarantees.
    throw new UnsupportedOperationException("TODO");
  }

  /**
   * Constructs a map <code>depends</code> such that <code>depends.get(v)</code> is a set containing
   * all assignments to some local with <code>v</code> on the right-hand side.
   */
  private static Map<Local, Set<JAssignStmt>> dependentAssignments(@Nonnull Body body) {
    Map<Local, Set<JAssignStmt>> depends = new HashMap<>();
    for (Stmt stmt : body.getStmts()) {
      if (!(stmt instanceof JAssignStmt)) {
        continue;
      }
      JAssignStmt assignStmt = (JAssignStmt) stmt;
      Value leftOp = assignStmt.getLeftOp();
      if (!(leftOp instanceof Local)) {
        continue;
      }

      Value rightOp = assignStmt.getRightOp();
      if (rightOp instanceof Local) {
        depends.computeIfAbsent((Local) rightOp, (__) -> new HashSet<>()).add(assignStmt);
      } else if (rightOp instanceof JCastExpr && ((JCastExpr) rightOp).getOp() instanceof Local) {
        depends
            .computeIfAbsent((Local) ((JCastExpr) rightOp).getOp(), (__) -> new HashSet<>())
            .add(assignStmt);
      }
    }
    return depends;
  }

  /**
   * Finds a type in <code>lcaCandidates</code> that is not a supertype of at least one of the types
   * in the Set <code>types</code>. It is therefore not a common ancestor. If no such value is
   * found, this function returns <code>null</code>.
   */
  @Nullable
  private static Type firstNonCommonAncestor(
      @Nonnull Set<Type> types, TypeHierarchy hierarchy, @Nonnull Set<Type> lcaCandidates) {
    for (Type lcaCandidate : lcaCandidates) {
      for (Type type : types) {
        if (!hierarchy.isSubtype(lcaCandidate, type)) {
          return lcaCandidate;
        }
      }
    }

    return null;
  }

  /**
   * Finds the set of lowest common supertypes between the <code>types</code>. Examples: <code>
   * {String, List} -> {Object}</code>, <code>{HashSet, ArrayList} -> {Collection, Cloneable, ...}
   * </code>.
   */
  private static Set<Type> leastCommonAncestors(
      @Nonnull Set<Type> types, @Nonnull TypeHierarchy hierarchy) {
    for (Type type : types) {
      if (!(type instanceof ReferenceType)) {
        throw new IllegalArgumentException(
            "Type set contains type '" + type + "' that is not a reference type");
      }
      if (type instanceof ArrayType) {
        throw new UnsupportedOperationException(
            "leastCommonAncestors is not yet implemented for array types");
      }
    }

    Set<Type> lcaCandidates = new HashSet<>(types);
    Type nonLca = firstNonCommonAncestor(types, hierarchy, lcaCandidates);
    while (nonLca != null) {
      ClassType nonLcaClassType = (ClassType) nonLca;

      Set<ClassType> implementedInterfacesOf =
          hierarchy.directlyImplementedInterfacesOf(nonLcaClassType);
      ClassType typeSuperClass = hierarchy.superClassOf(nonLcaClassType);

      lcaCandidates.remove(nonLca);
      lcaCandidates.addAll(implementedInterfacesOf);
      if (typeSuperClass != null) {
        lcaCandidates.add(typeSuperClass);
      }

      nonLca = firstNonCommonAncestor(types, hierarchy, lcaCandidates);
    }

    return lcaCandidates;
  }

  /**
   * Evaluates the type of <code>expr</code> under the <code>typing</code>. Corresponds to the
   * <code>eval</code> function from the paper.
   */
  private static Type typeOfExprUnderTyping(@Nonnull Typing typing, @Nonnull Value expr) {
    throw new UnsupportedOperationException();
  }
}
