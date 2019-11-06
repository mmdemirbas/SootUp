package de.upb.swt.soot.java.bytecode.interceptors.typeinference;

import de.upb.swt.soot.core.jimple.basic.Local;
import de.upb.swt.soot.core.jimple.common.expr.Expr;
import de.upb.swt.soot.core.jimple.common.stmt.JAssignStmt;
import de.upb.swt.soot.core.jimple.common.stmt.Stmt;
import de.upb.swt.soot.core.model.Body;
import de.upb.swt.soot.core.transform.BodyInterceptor;
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

  private static Set<Type> leastCommonAncestors(Set<Type> types, TypeHierarchy hierarchy) {
    // lca := {}
    // For each type t
    //  For each direct supertype s of t
    //    if s is supertype of all types
    //      lca += s
    //    else
    //      traverse up in hierarchy from s until the if-condition is met or there is no higher
    // type.
    // return lca

    throw new UnsupportedOperationException();
  }

  private static Type eval(Typing typing, Expr expr) {
    throw new UnsupportedOperationException();
  }
}
