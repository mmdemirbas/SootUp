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
import java.util.ArrayList;
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

public class TypeAssigner implements BodyInterceptor {

  private static class BottomType extends Type {

    private static final BottomType instance = new BottomType();
  }
  /** Serves as a type alias */
  private static class Typing extends HashMap<Local, Type> {

    Typing() {}

    Typing(Map<? extends Local, ? extends Type> m) {
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
    List<Stmt> stmts = new ArrayList<>(originalBody.getStmts());

    // Algorithm 2: General type inference algorithm

    Stream<JAssignStmt> localAssignments =
        stmts.stream()
            .filter(stmt -> stmt instanceof JAssignStmt)
            .map(stmt -> (JAssignStmt) stmt)
            .filter(assignStmt -> assignStmt.getLeftOp() instanceof Local);

    Map<Local, Set<JAssignStmt>> depends = dependentAssignments(originalBody);

    Typing initialTyping = new Typing();
    originalBody.getLocals().forEach(local -> initialTyping.put(local, BottomType.instance));
    Set<Typing> typings = new HashSet<>();
    typings.add(initialTyping);

    Map<Typing, Deque<JAssignStmt>> worklists = new HashMap<>();
    worklists.put(
        initialTyping, localAssignments.collect(Collectors.toCollection(ArrayDeque::new)));

    while (true) {
      Typing incompleteTyping =
          typings.stream()
              .filter(typing -> !worklists.get(typing).isEmpty())
              .findAny()
              .orElse(null);
      if (incompleteTyping == null) break;

      typings.remove(incompleteTyping);
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

    // TODO We have now computed the candidate typings. Check paper for what should be done next

    return originalBody.withStmts(stmts);
  }

  /**
   * Constructs a map <code>depends</code> such that <code>depends.get(v)</code> is a set containing
   * all assignments to some local with <code>v</code> on the right-hand side.
   */
  private static Map<Local, Set<JAssignStmt>> dependentAssignments(Body body) {
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

  // TODO Document
  @Nullable
  private static Type firstNonCommonAncestor(
      Set<Type> types, TypeHierarchy hierarchy, Set<Type> lcaCandidates) {
    for (Type lcaCandidate : lcaCandidates) {
      for (Type type : types) {
        if (!hierarchy.isSubtype(lcaCandidate, type)) {
          return lcaCandidate;
        }
      }
    }

    return null;
  }

  // TODO Document, test
  private static Set<Type> leastCommonAncestors(Set<Type> types, TypeHierarchy hierarchy) {
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
  private static Type typeOfExprUnderTyping(Typing typing, Value expr) {
    throw new UnsupportedOperationException();
  }
}
