package de.upb.swt.soot.java.bytecode.interceptors.typeinference;

import de.upb.swt.soot.core.jimple.basic.Local;
import de.upb.swt.soot.core.jimple.common.expr.Expr;
import de.upb.swt.soot.core.jimple.common.stmt.JAssignStmt;
import de.upb.swt.soot.core.jimple.common.stmt.Stmt;
import de.upb.swt.soot.core.model.Body;
import de.upb.swt.soot.core.transform.BodyInterceptor;
import de.upb.swt.soot.core.types.ArrayType;
import de.upb.swt.soot.core.types.ClassType;
import de.upb.swt.soot.core.types.ReferenceType;
import de.upb.swt.soot.core.types.Type;
import de.upb.swt.soot.java.core.typehierarchy.TypeHierarchy;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Stage1 implements BodyInterceptor {

  private static class BottomType extends Type {
    private static final BottomType instance = new BottomType();
  }

  /** Serves as a type alias */
  private static class Typing extends HashMap<Local, Type> {}

  @Nonnull
  @Override
  public Body interceptBody(@Nonnull Body originalBody) {
    List<Stmt> stmts = new ArrayList<>(originalBody.getStmts());
    Stream<JAssignStmt> localAssignments =
        stmts.stream()
            .filter(stmt -> stmt instanceof JAssignStmt)
            .map(stmt -> (JAssignStmt) stmt)
            .filter(assignStmt -> assignStmt.getLeftOp() instanceof Local);

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
      JAssignStmt assignStmt = worklists.get(incompleteTyping).poll();

      // TODO
    }

    return originalBody.withStmts(stmts);
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

  private static Type eval(Typing typing, Expr expr) {
    throw new UnsupportedOperationException();
  }
}
