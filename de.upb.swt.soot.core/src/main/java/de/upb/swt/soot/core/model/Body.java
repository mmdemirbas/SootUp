package de.upb.swt.soot.core.model;

/*-
 * #%L
 * Soot - a J*va Optimization Framework
 * %%
 * Copyright (C) 1997-2020 Raja Vallee-Rai, Linghui Luo, Markus Schmidt and others
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

import com.google.common.collect.Lists;
import de.upb.swt.soot.core.graph.*;
import de.upb.swt.soot.core.jimple.basic.*;
import de.upb.swt.soot.core.jimple.common.ref.JParameterRef;
import de.upb.swt.soot.core.jimple.common.ref.JThisRef;
import de.upb.swt.soot.core.jimple.common.stmt.*;
import de.upb.swt.soot.core.jimple.javabytecode.stmt.JSwitchStmt;
import de.upb.swt.soot.core.signatures.MethodSignature;
import de.upb.swt.soot.core.types.Type;
import de.upb.swt.soot.core.util.Copyable;
import de.upb.swt.soot.core.util.EscapedWriter;
import de.upb.swt.soot.core.util.ImmutableUtils;
import de.upb.swt.soot.core.util.printer.Printer;
import de.upb.swt.soot.core.validation.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Class that models the Jimple body (code attribute) of a method.
 *
 * @author Linghui Luo
 */
public class Body implements Copyable {

  /** The locals for this Body. */
  private final Set<Local> locals;

  @Nonnull private final ImmutableExceptionalStmtGraph ecfg;

  /** The Position Information in the Source for this Body. */
  @Nonnull private final Position position;

  /** The MethodSignature associated with this Body. */
  @Nonnull private final MethodSignature methodSignature;

  /** An array containing some validators in order to validate the JimpleBody */
  @Nonnull
  private static final List<BodyValidator> validators =
      ImmutableUtils.immutableList(
          new LocalsValidator(),
          new TrapsValidator(),
          new StmtsValidator(),
          new UsesValidator(),
          new ValuesValidator(),
          new CheckInitValidator(),
          new CheckTypesValidator(),
          new CheckVoidLocalesValidator(),
          new CheckEscapingValidator());

  /**
   * Creates an body which is not associated to any method.
   *
   * @param locals please use {@link LocalGenerator} to generate local for a body.
   */
  private Body(
      @Nonnull MethodSignature methodSignature,
      @Nonnull Set<Local> locals,
      @Nonnull StmtGraph stmtGraph,
      @Nonnull Position position) {
    this.methodSignature = methodSignature;
    this.locals = Collections.unmodifiableSet(locals);
    this.ecfg = new ImmutableExceptionalStmtGraph(stmtGraph);
    this.position = position;
    // FIXME: [JMP] Virtual method call in constructor
    checkInit();
  }

  /**
   * Returns the MethodSignature associated with this Body.
   *
   * @return the method that owns this body.
   */
  @Nonnull
  public MethodSignature getMethodSignature() {
    return methodSignature;
  }

  /** Returns the number of locals declared in this body. */
  public int getLocalCount() {
    return locals.size();
  }

  private void runValidation(BodyValidator validator) {
    final List<ValidationException> exceptionList = new ArrayList<>();
    validator.validate(this, exceptionList);
    if (!exceptionList.isEmpty()) {
      throw exceptionList.get(0);
    }
  }

  /** Verifies that a Value is not used in more than one place. */
  public void validateValues() {
    runValidation(new ValuesValidator());
  }

  /** Verifies that each Local of getUsesAndDefs() is in this body's locals Chain. */
  public void validateLocals() {
    runValidation(new LocalsValidator());
  }

  /** Verifies that the begin, end and handler units of each trap are in this body. */
  public void validateTraps() {
    runValidation(new TrapsValidator());
  }

  /** Verifies that the Stmts of this Body all point to a Stmt contained within this body. */
  public void validateStmts() {
    runValidation(new StmtsValidator());
  }

  /** Verifies that each use in this Body has a def. */
  public void validateUses() {
    runValidation(new UsesValidator());
  }

  /** Returns a backed chain of the locals declared in this Body. */
  public Set<Local> getLocals() {
    return locals;
  }

  /** Returns an unmodifiable view of the traps found in this Body. */
  @Nonnull
  public List<Trap> getTraps() {
    return ecfg.getTraps();
  }

  /** Return unit containing the \@this-assignment * */
  public Stmt getThisStmt() {
    for (Stmt u : getStmts()) {
      if (u instanceof JIdentityStmt && ((JIdentityStmt) u).getRightOp() instanceof JThisRef) {
        return u;
      }
    }

    throw new RuntimeException("couldn't find this-assignment!" + " in " + getMethodSignature());
  }

  /** Return LHS of the first identity stmt assigning from \@this. */
  public Local getThisLocal() {
    return (Local) (((JIdentityStmt) getThisStmt()).getLeftOp());
  }

  /** Return LHS of the first identity stmt assigning from \@parameter i. */
  public Local getParameterLocal(int i) {
    for (Stmt s : getStmts()) {
      if (s instanceof JIdentityStmt && ((JIdentityStmt) s).getRightOp() instanceof JParameterRef) {
        JIdentityStmt is = (JIdentityStmt) s;
        JParameterRef pr = (JParameterRef) is.getRightOp();
        if (pr.getIndex() == i) {
          return (Local) is.getLeftOp();
        }
      }
    }

    throw new RuntimeException("couldn't find JParameterRef" + i + "! in " + getMethodSignature());
  }

  /**
   * Get all the LHS of the identity statements assigning from parameter references.
   *
   * @return a list of size as per <code>getMethod().getParameterCount()</code> with all elements
   *     ordered as per the parameter index.
   * @throws RuntimeException if a JParameterRef is missing
   */
  @Nonnull
  public Collection<Local> getParameterLocals() {
    final List<Local> retVal = new ArrayList<>();
    // TODO: [ms] performance: don't iterate over all stmt -> lazy vs freedom/error tolerance -> use
    // fixed index positions at the beginning?
    for (Stmt u : ecfg.nodes()) {
      if (u instanceof JIdentityStmt) {
        JIdentityStmt is = (JIdentityStmt) u;
        if (is.getRightOp() instanceof JParameterRef) {
          JParameterRef pr = (JParameterRef) is.getRightOp();
          retVal.add(pr.getIndex(), (Local) is.getLeftOp());
        }
      }
    }
    return Collections.unmodifiableCollection(retVal);
  }

  /**
   * Returns the result of iterating through all Stmts in this body. All Stmts thus found are
   * returned. Branching Stmts and statements which use PhiExpr will have Stmts; a Stmt contains a
   * Stmt that is either a target of a branch or is being used as a pointer to the end of a CFG
   * block.
   *
   * <p>This method was typically used for pointer patching, e.g. when the unit chain is cloned.
   *
   * @return A collection of all the Stmts
   */
  @Nonnull
  public Collection<Stmt> getTargetStmtsInBody() {
    List<Stmt> stmtList = new ArrayList<>();
    for (Stmt stmt : ecfg.nodes()) {
      if (stmt instanceof BranchingStmt) {
        if (stmt instanceof JIfStmt) {
          stmtList.add(((JIfStmt) stmt).getTarget(this));
        } else if (stmt instanceof JGotoStmt) {
          stmtList.add(((JGotoStmt) stmt).getTarget(this));
        } else if (stmt instanceof JSwitchStmt) {
          stmtList.addAll(getBranchTargetsOf((BranchingStmt) stmt));
        }
      }
    }

    for (Trap item : getTraps()) {
      stmtList.addAll(item.getStmts());
    }
    return Collections.unmodifiableCollection(stmtList);
  }

  /**
   * returns the control flow graph that represents this body into a linear List of statements.
   *
   * @return the statements in this Body
   */
  @Nonnull
  public List<Stmt> getStmts() {
    final ArrayList<Stmt> stmts = new ArrayList<>(ecfg.nodes().size());
    for (Stmt stmt : ecfg) {
      stmts.add(stmt);
    }
    return stmts;
  }

  @Nonnull
  public ImmutableExceptionalStmtGraph getStmtGraph() {
    return ecfg;
  }

  private void checkInit() {
    runValidation(new CheckInitValidator());
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    StringWriter writer = new StringWriter();
    try (PrintWriter writerOut = new PrintWriter(new EscapedWriter(writer))) {
      new Printer().printTo(this, writerOut);
    }
    return writer.toString();
  }

  @Nonnull
  public Position getPosition() {
    return position;
  }

  /** returns a List of Branch targets of Branching Stmts */
  @Nonnull
  public List<Stmt> getBranchTargetsOf(@Nonnull BranchingStmt fromStmt) {
    return ecfg.successors(fromStmt);
  }

  public boolean isStmtBranchTarget(@Nonnull Stmt targetStmt) {
    final List<Stmt> predecessors = ecfg.predecessors(targetStmt);
    if (predecessors.size() > 1) {
      return true;
    }

    final Iterator<Stmt> iterator = predecessors.iterator();
    if (iterator.hasNext()) {
      Stmt pred = iterator.next();
      if (pred.branches()) {
        if (pred instanceof JIfStmt) {
          return ((JIfStmt) pred).getTarget(this) == targetStmt;
        }
        return true;
      }
    }

    return false;
  }

  public void validateIdentityStatements() {
    runValidation(new IdentityStatementsValidator());
  }

  /** Returns the first non-identity stmt in this body. */
  @Nonnull
  public Stmt getFirstNonIdentityStmt() {
    Iterator<Stmt> it = getStmts().iterator();
    Stmt o = null;
    while (it.hasNext()) {
      if (!((o = it.next()) instanceof JIdentityStmt)) {
        break;
      }
    }
    if (o == null) {
      throw new RuntimeException("no non-id statements!");
    }
    return o;
  }

  /**
   * Returns the results of iterating through all Stmts in this Body and querying them for Values
   * defined. All of the Values found are then returned as a List.
   *
   * @return a List of all the Values for Values defined by this Body's Stmts.
   */
  public Collection<Value> getUses() {
    ArrayList<Value> useList = new ArrayList<>();

    for (Stmt stmt : ecfg.nodes()) {
      useList.addAll(stmt.getUses());
    }
    return useList;
  }

  /**
   * Returns the results of iterating through all Stmts in this Body and querying them for Values
   * defined. All of the Values found are then returned as a List.
   *
   * @return a List of all the Values for Values defined by this Body's Stmts.
   */
  public Collection<Value> getDefs() {
    ArrayList<Value> defList = new ArrayList<>();

    for (Stmt stmt : ecfg.nodes()) {
      defList.addAll(stmt.getDefs());
    }
    return defList;
  }

  @Nonnull
  public Body withLocals(@Nonnull Set<Local> locals) {
    return new Body(getMethodSignature(), locals, getStmtGraph(), getPosition());
  }

  /** helps against ConcurrentModificationException; it queues changes until they are committed */
  // TODO: think about same nodes/flows added AND removed
  // [ms] use/implement a snapshotiterator instead?
  private static class StmtGraphManipulationQueue {

    @Nonnull private final List<Stmt> nodesToRemove = new ArrayList<>();
    @Nonnull private final List<Stmt> nodesToAdd = new ArrayList<>();

    void addNode(@Nonnull Stmt node) {
      nodesToAdd.add(node);
    }

    void removeNode(@Nonnull Stmt node) {
      nodesToRemove.add(node);
    }

    // List sizes are a multiple of 2; even: from odd: to of an edge
    @Nonnull private final List<Stmt> flowsToRemove = new ArrayList<>();
    @Nonnull private final List<Stmt> flowsToAdd = new ArrayList<>();

    void addFlow(@Nonnull Stmt from, @Nonnull Stmt to) {
      flowsToAdd.add(from);
      flowsToAdd.add(to);
    }

    void removeFlow(@Nonnull Stmt from, @Nonnull Stmt to) {
      flowsToRemove.add(from);
      flowsToRemove.add(to);
    }

    /** return true if there where queued changes */
    boolean commit(MutableStmtGraph graph) {

      if (!flowsToAdd.isEmpty()
          || !flowsToRemove.isEmpty()
          || !nodesToAdd.isEmpty()
          || !nodesToRemove.isEmpty()) {
        for (Stmt stmt : nodesToAdd) {
          graph.addNode(stmt);
        }

        for (Stmt stmt : nodesToRemove) {
          graph.removeNode(stmt);
        }

        Iterator<Stmt> addIt = flowsToAdd.iterator();
        while (addIt.hasNext()) {
          final Stmt from = addIt.next();
          final Stmt to = addIt.next();
          graph.putEdge(from, to);
        }

        Iterator<Stmt> remIt = flowsToRemove.iterator();
        while (remIt.hasNext()) {
          final Stmt from = remIt.next();
          final Stmt to = remIt.next();
          graph.removeEdge(from, to);
        }
        clear();

        return true;
      }
      return false;
    }

    public void clear() {
      nodesToAdd.clear();
      nodesToRemove.clear();
      flowsToAdd.clear();
      flowsToRemove.clear();
    }
  }

  public static BodyBuilder builder() {
    return new BodyBuilder();
  }

  public static BodyBuilder builder(@Nonnull Body body, Set<Modifier> modifiers) {
    return new BodyBuilder(body, modifiers);
  }

  /**
   * The BodyBuilder helps to create a new Body in a fluent way (see Builder Pattern)
   *
   * <pre>
   * <code>
   * Stmt stmt1, stmt2, stmt3;
   * ...
   * Body.BodyBuilder builder = Body.builder();
   * builder.setMethodSignature( ... );
   * builder.setStartingStmt(stmt1);
   * builder.addFlow(stmt1,stmt2);
   * builder.addFlow(stmt2,stmt3);
   * ...
   * Body body = builder.build();
   *
   * </code>
   * </pre>
   */
  public static class BodyBuilder {
    @Nonnull private Set<Local> locals = new LinkedHashSet<>();
    @Nonnull private final LocalGenerator localGen = new LocalGenerator(locals);
    @Nonnull private Set<Modifier> modifiers = Collections.emptySet();

    @Nullable private Position position = null;
    @Nonnull private final MutableExceptionalStmtGraph ecfg;
    @Nullable private MethodSignature methodSig = null;

    @Nullable private StmtGraphManipulationQueue changeQueue = null;
    @Nullable private List<Stmt> cachedLinearizedStmts = null;

    BodyBuilder() {
      ecfg = new MutableExceptionalStmtGraph();
    }

    BodyBuilder(@Nonnull Body body, @Nonnull Set<Modifier> modifiers) {
      setModifiers(modifiers);
      setMethodSignature(body.getMethodSignature());
      setLocals(body.getLocals());
      setPosition(body.getPosition());
      ecfg = new MutableExceptionalStmtGraph(body.getStmtGraph());
      setTraps(body.getTraps());
    }

    @Nonnull
    public ExceptionalStmtGraph getStmtGraph() {
      return ecfg.unmodifiableStmtGraph();
    }

    @Nonnull
    public List<Stmt> getStmts() {
      cachedLinearizedStmts = Lists.newArrayList(ecfg);
      return cachedLinearizedStmts;
    }

    @Nonnull
    public Set<Local> getLocals() {
      return Collections.unmodifiableSet(locals);
    }

    @Nonnull
    public BodyBuilder setStartingStmt(@Nonnull Stmt startingStmt) {
      ecfg.setStartingStmt(startingStmt);
      return this;
    }

    @Nonnull
    public BodyBuilder setLocals(@Nonnull Set<Local> locals) {
      this.locals = locals;
      return this;
    }

    @Nonnull
    public BodyBuilder addLocal(@Nonnull String name, Type type) {
      locals.add(localGen.generateLocal(type));
      return this;
    }

    @Nonnull
    public BodyBuilder addLocal(@Nonnull Local local) {
      locals.add(local);
      return this;
    }

    @Nonnull
    public BodyBuilder setTraps(@Nonnull List<Trap> traps) {
      ecfg.setTraps(traps);
      return this;
    }

    /** replace the oldStmt with newStmt in stmtGraph and branches */
    @Nonnull
    public BodyBuilder replaceStmt(@Nonnull Stmt oldStmt, @Nonnull Stmt newStmt) {
      ecfg.replaceNode(oldStmt, newStmt);
      adaptTraps(oldStmt, newStmt);
      return this;
    }

    /**
     * Fit the modified stmt in Traps
     *
     * @param oldStmt a Stmt which maybe a beginStmt or endStmt in a Trap
     * @param newStmt a modified stmt to replace the oldStmt.
     */
    private void adaptTraps(@Nonnull Stmt oldStmt, @Nonnull Stmt newStmt) {
      List<Trap> traps = new ArrayList<>(this.getStmtGraph().getTraps());
      for (ListIterator<Trap> iterator = traps.listIterator(); iterator.hasNext(); ) {
        Trap trap = iterator.next();
        if (oldStmt.equivTo(trap.getBeginStmt())) {
          Trap newTrap = trap.withBeginStmt(newStmt);
          iterator.set(newTrap);
        } else if (oldStmt.equivTo(trap.getEndStmt())) {
          Trap newTrap = trap.withEndStmt(newStmt);
          iterator.set(newTrap);
        }
      }
      this.setTraps(traps);
    }

    /** remove the a stmt from the graph and stmt */
    @Nonnull
    public BodyBuilder removeStmt(@Nonnull Stmt stmt) {
      if (changeQueue == null) {
        ecfg.removeNode(stmt);
        cachedLinearizedStmts = null;
      } else {
        changeQueue.removeNode(stmt);
      }
      return this;
    }

    @Nonnull
    public BodyBuilder removeDestinations(@Nonnull Stmt stmt) {
      if (ecfg.containsNode(stmt)) {
        ecfg.removeDestinations(stmt);
      }
      return this;
    }

    @Nonnull
    public List<Trap> getTraps() {
      return ecfg.getTraps();
    }

    @Nonnull
    public BodyBuilder addFlow(@Nonnull Stmt fromStmt, @Nonnull Stmt toStmt) {
      if (changeQueue == null) {
        ecfg.putEdge(fromStmt, toStmt);
        cachedLinearizedStmts = null;
      } else {
        changeQueue.addFlow(fromStmt, toStmt);
      }
      return this;
    }

    @Nonnull
    public BodyBuilder removeFlow(@Nonnull Stmt fromStmt, @Nonnull Stmt toStmt) {
      if (changeQueue == null) {
        ecfg.removeEdge(fromStmt, toStmt);
        cachedLinearizedStmts = null;
      } else {
        changeQueue.removeFlow(fromStmt, toStmt);
      }
      return this;
    }

    public BodyBuilder setModifiers(@Nonnull Set<Modifier> modifiers) {
      this.modifiers = modifiers;
      return this;
    }

    @Nonnull
    public BodyBuilder setPosition(@Nonnull Position position) {
      this.position = position;
      return this;
    }

    public BodyBuilder setMethodSignature(MethodSignature methodSig) {
      this.methodSig = methodSig;
      return this;
    }

    /**
     * Queues changes to the StmtGraph (e.g. addFlow, removeFlow) until they are commited. helps to
     * prevent ConcurrentModificationException
     */
    public BodyBuilder enableDeferredStmtGraphChanges() {
      if (changeQueue == null) {
        changeQueue = new StmtGraphManipulationQueue();
      }
      return this;
    }

    /**
     * commits the changes that were added to the queue if that was enabled before AND disables
     * further queueing of changes.
     */
    public BodyBuilder disableAndCommitDeferredStmtGraphChanges() {
      commitDeferredStmtGraphChanges();
      changeQueue = null;
      return this;
    }

    /** commits the changes that were added to the queue if that was enabled before */
    public BodyBuilder commitDeferredStmtGraphChanges() {
      if (changeQueue != null) {
        if (changeQueue.commit(ecfg)) {
          cachedLinearizedStmts = null;
        }
      }
      return this;
    }
    /** clears queued changes fot */
    public BodyBuilder clearDeferredStmtGraphChanges() {
      if (changeQueue != null) {
        changeQueue.clear();
      }
      return this;
    }

    @Nonnull
    public Body build() {

      if (methodSig == null) {
        throw new RuntimeException("There is no MethodSignature set.");
      }

      if (position == null) {
        setPosition(NoPositionInformation.getInstance());
      }

      // commit pending changes
      commitDeferredStmtGraphChanges();

      final Stmt startingStmt = ecfg.getStartingStmt();
      final Set<Stmt> nodes = ecfg.nodes();
      if (nodes.size() > 0 && !nodes.contains(startingStmt)) {
        throw new RuntimeException(
            methodSig
                + ": The given startingStmt '"
                + startingStmt
                + "' does not exist in the StmtGraph.");
      }

      // validate statements
      try {
        ecfg.validateStmtConnectionsInGraph();
      } catch (Exception e) {
        throw new RuntimeException("StmtGraph of " + methodSig + " is invalid.", e);
      }

      return new Body(methodSig, locals, ecfg, position);
    }

    public Set<Modifier> getModifiers() {
      return modifiers;
    }

    /** Removes a stmt and all of it's then unreachable sucessor stmts */
    public BodyBuilder removeStmtAndUnreachableSuccessors(Stmt stmt) {
      final StmtGraph stmtGraph = this.getStmtGraph();
      Deque<Stmt> stack = new ArrayDeque<>();
      stack.addFirst(stmt);

      while (!stack.isEmpty()) {
        Stmt itStmt = stack.pollFirst();
        if (stmtGraph.containsNode(itStmt)) {
          for (Stmt succ : stmtGraph.successors(itStmt)) {
            removeFlow(itStmt, succ);
            stack.add(succ);
          }
        }
      }

      if (this.getStmtGraph().getStartingStmt() == stmt) {
        setStartingStmt(null);
      }

      return this;
    }

    public BodyBuilder removeStmtAndLinkPredecessorsToSuccessors(Stmt stmt) {
      final StmtGraph stmtGraph = this.getStmtGraph();
      final Collection<Stmt> predecessors = stmtGraph.predecessors(stmt);
      final Collection<Stmt> successors = stmtGraph.successors(stmt);

      for (Stmt predecessor : predecessors) {
        this.removeFlow(predecessor, stmt);
        for (Stmt successor : successors) {
          this.addFlow(predecessor, successor);
        }
      }
      for (Stmt successor : successors) {
        this.removeFlow(stmt, successor);
      }

      if (this.getStmtGraph().getStartingStmt() == stmt) {
        setStartingStmt(null);
      }

      return this;
    }
  }
}
