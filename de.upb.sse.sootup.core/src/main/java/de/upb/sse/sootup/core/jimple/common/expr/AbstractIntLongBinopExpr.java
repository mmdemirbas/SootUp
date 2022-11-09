package de.upb.sse.sootup.core.jimple.common.expr;

/*-
 * #%L
 * Soot - a J*va Optimization Framework
 * %%
 * Copyright (C) 1999-2020 Patrick Lam, Linghui Luo, Christian Brüggemann
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

import de.upb.sse.sootup.core.jimple.basic.Immediate;
import de.upb.sse.sootup.core.jimple.basic.Value;
import de.upb.sse.sootup.core.types.PrimitiveType;
import de.upb.sse.sootup.core.types.Type;
import de.upb.sse.sootup.core.types.UnknownType;
import javax.annotation.Nonnull;

public abstract class AbstractIntLongBinopExpr extends AbstractBinopExpr {

  AbstractIntLongBinopExpr(@Nonnull Immediate op1, @Nonnull Immediate op2) {
    super(op1, op2);
  }

  @Nonnull
  @Override
  public Type getType() {
    Value op1 = getOp1();
    Value op2 = getOp2();
    Type op1t = op1.getType();
    Type op2t = op2.getType();

    if (PrimitiveType.isIntLikeType(op1t) && PrimitiveType.isIntLikeType(op2t)) {
      return PrimitiveType.getInt();
    } else if (op1t.equals(PrimitiveType.getLong()) || op2t.equals(PrimitiveType.getLong())) {
      return PrimitiveType.getLong();
    } else {
      return UnknownType.getInstance();
    }
  }
}