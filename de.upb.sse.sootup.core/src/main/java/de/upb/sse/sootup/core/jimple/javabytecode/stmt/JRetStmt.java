package de.upb.sse.sootup.core.jimple.javabytecode.stmt;

/*-
 * #%L
 * Soot - a J*va Optimization Framework
 * %%
 * Copyright (C) 1999-2020 Patrick Lam, Markus Schmidt, Linghui luo and others
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

import de.upb.sse.sootup.core.jimple.Jimple;
import de.upb.sse.sootup.core.jimple.basic.*;
import de.upb.sse.sootup.core.jimple.basic.JimpleComparator;
import de.upb.sse.sootup.core.jimple.basic.StmtPositionInfo;
import de.upb.sse.sootup.core.jimple.basic.Value;
import de.upb.sse.sootup.core.jimple.common.stmt.Stmt;
import de.upb.sse.sootup.core.jimple.visitor.StmtVisitor;
import de.upb.sse.sootup.core.util.Copyable;
import de.upb.sse.sootup.core.util.printer.StmtPrinter;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

/** Represents the deprecated JVM <code>ret</code> statement */
public final class JRetStmt extends Stmt implements Copyable {

  @Nonnull private final Value stmtAddress;

  public JRetStmt(@Nonnull Value stmtAddress, @Nonnull StmtPositionInfo positionInfo) {
    super(positionInfo);
    this.stmtAddress = stmtAddress;
  }

  @Override
  public String toString() {
    return Jimple.RET + " " + stmtAddress.toString();
  }

  @Override
  public void toString(@Nonnull StmtPrinter up) {
    up.literal(Jimple.RET);
    up.literal(" ");
    stmtAddress.toString(up);
  }

  @Nonnull
  public Value getStmtAddress() {
    return stmtAddress;
  }

  @Override
  @Nonnull
  public List<Value> getUses() {
    final List<Value> uses = stmtAddress.getUses();
    List<Value> list = new ArrayList<>(uses.size() + 1);
    list.add(stmtAddress);
    return list;
  }

  @Override
  public void accept(@Nonnull StmtVisitor sw) {
    sw.caseRetStmt(this);
  }

  @Override
  public boolean fallsThrough() {
    return true;
  }

  @Override
  public boolean branches() {
    return false;
  }

  @Override
  public boolean equivTo(@Nonnull Object o, @Nonnull JimpleComparator comparator) {
    return comparator.caseRetStmt(this, o);
  }

  @Override
  public int equivHashCode() {
    return stmtAddress.equivHashCode();
  }

  @Nonnull
  public JRetStmt withStmtAddress(@Nonnull Value stmtAddress) {
    return new JRetStmt(stmtAddress, getPositionInfo());
  }

  @Nonnull
  public JRetStmt withPositionInfo(@Nonnull StmtPositionInfo positionInfo) {
    return new JRetStmt(getStmtAddress(), positionInfo);
  }
}