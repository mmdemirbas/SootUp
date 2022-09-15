package de.upb.swt.soot.java.bytecode.interceptors.typeresolving;
/*-
 * #%L
 * Soot - a J*va Optimization Framework
 * %%
 * Copyright (C) 2019-2022 Zun Wang
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
import de.upb.swt.soot.core.IdentifierFactory;
import de.upb.swt.soot.core.jimple.basic.Immediate;
import de.upb.swt.soot.core.jimple.basic.Local;
import de.upb.swt.soot.core.jimple.basic.Trap;
import de.upb.swt.soot.core.jimple.basic.Value;
import de.upb.swt.soot.core.jimple.common.constant.*;
import de.upb.swt.soot.core.jimple.common.expr.*;
import de.upb.swt.soot.core.jimple.common.ref.*;
import de.upb.swt.soot.core.jimple.common.stmt.Stmt;
import de.upb.swt.soot.core.model.Body;
import de.upb.swt.soot.core.model.SootClass;
import de.upb.swt.soot.core.typerhierachy.ViewTypeHierarchy;
import de.upb.swt.soot.core.types.*;
import de.upb.swt.soot.core.views.View;
import de.upb.swt.soot.java.bytecode.interceptors.typeresolving.types.BottomType;
import de.upb.swt.soot.java.core.JavaIdentifierFactory;
import java.util.*;

/** @author Zun Wang */
public class AugEvalFunction {

  IdentifierFactory factory = JavaIdentifierFactory.getInstance();
  View view;

  public AugEvalFunction(View view) {
    this.view = view;
  }

  /**
   * This method is used to evaluate the type of the given value which the given stmt and body
   * belongs to.
   */
  // todo[zw]: maybe delete the IntegerType, use PrimitiveType.isIntLikeType
  public Type evaluate(Typing typing, Value value, Stmt stmt, Body body) {
    if (value instanceof Immediate) {
      if (value instanceof Local) {
        return typing.getType((Local) value);
        // if value instanceof Constant
      } else if (value instanceof Constant) {
        // todo[zw]: later check if necessary to divide them
        if (value instanceof IntConstant) {
          int val = ((IntConstant) value).getValue();
          if (val >= 0 && val < 2) {
            return PrimitiveType.getInteger1();
          } else if (val >= 2 && val < 128) {
            return PrimitiveType.getInteger127();
          } else if (val >= -128 && val < 0) {
            return PrimitiveType.getByte();
          } else if (val >= 128 && val < 32768) {
            return PrimitiveType.getInteger32767();
          } else if (val >= -32768 && val < -128) {
            return PrimitiveType.getShort();
          } else if (val >= 32768 && val < 65536) {
            return PrimitiveType.getChar();
          } else {
            return PrimitiveType.getInt();
          }
        } else if (value instanceof LongConstant
            || value instanceof FloatConstant
            || value instanceof DoubleConstant
            || value instanceof NullConstant
            || value instanceof EnumConstant) {
          return value.getType();
        } else if (value instanceof StringConstant) {
          return factory.getClassType("java.lang.String");
        } else if (value instanceof ClassConstant) {
          return factory.getClassType("java.lang.Class");
        } else if (value instanceof MethodHandle) {
          return factory.getClassType("java.lang.MethodHandle");
        } else if (value instanceof MethodType) {
          return factory.getClassType("java.lang.MethodType");
        } else {
          throw new RuntimeException("Invaluable constant in AugEvalFunction: " + value);
        }
      } else {
        throw new RuntimeException("Invaluable constant in AugEvalFunction: " + value);
      }
    } else if (value instanceof Expr) {
      if (value instanceof AbstractBinopExpr) {
        Type tl = evaluate(typing, ((AbstractBinopExpr) value).getOp1(), stmt, body);
        Type tr = evaluate(typing, ((AbstractBinopExpr) value).getOp2(), stmt, body);

        if (value instanceof AbstractIntBinopExpr) {
          return PrimitiveType.getInt();

        } else if (value instanceof AbstractIntLongBinopExpr) {
          if (value instanceof JShlExpr
              || value instanceof JShrExpr
              || value instanceof JUshrExpr) {
            if (tl instanceof IntegerType && tr instanceof IntegerType) {
              return PrimitiveType.getInt();
            } else if (tl instanceof PrimitiveType.LongType && tr instanceof IntegerType) {
              return PrimitiveType.getLong();
            } else {
              throw new RuntimeException("Invaluable expression in AugEvalFunction: " + value);
            }
          } else {
            if (tl instanceof IntegerType && tr instanceof IntegerType) {
              return PrimitiveType.getInt();
            } else if (tl instanceof PrimitiveType.LongType
                && tr instanceof PrimitiveType.LongType) {
              return PrimitiveType.getLong();
            } else {
              throw new RuntimeException("Invaluable expression in AugEvalFunction: " + value);
            }
          }
        } else if (value instanceof AbstractFloatBinopExpr) {
          if (tl instanceof IntegerType && tr instanceof IntegerType) {
            return PrimitiveType.getInt();
          } else if (tl instanceof PrimitiveType.LongType && tr instanceof PrimitiveType.LongType) {
            return PrimitiveType.getLong();
          } else if (tl instanceof PrimitiveType.FloatType
              && tr instanceof PrimitiveType.FloatType) {
            return PrimitiveType.getFloat();
          } else if (tl instanceof PrimitiveType.DoubleType
              && tr instanceof PrimitiveType.DoubleType) {
            return PrimitiveType.getDouble();
          } else {
            throw new RuntimeException("Invaluable expression in AugEvalFunction: " + value);
          }
        }
      } else if (value instanceof AbstractUnopExpr) {
        if (value instanceof JLengthExpr) {
          return PrimitiveType.getInt();
        } else {
          Type opt = evaluate(typing, ((AbstractUnopExpr) value).getOp(), stmt, body);
          return (opt instanceof IntegerType) ? PrimitiveType.IntType.getInstance() : opt;
        }
      } else if (value instanceof AbstractInvokeExpr
          || value instanceof JNewMultiArrayExpr
          || value instanceof JNewArrayExpr
          || value instanceof JCastExpr
          || value instanceof JNewExpr
          || value instanceof JInstanceOfExpr) {
        return value.getType();
      } else {
        throw new RuntimeException("Invaluable expression in AugEvalFunction: " + value);
      }
    } else if (value instanceof Ref) {
      // todo[zw]: one handle stmt to handle multiple types of traps
      if (value instanceof JCaughtExceptionRef) {
        Set<ClassType> exceptionTypes = getExceptionType(stmt, body);
        ClassType throwable = factory.getClassType("java.lang.Throwable");
        ClassType type = null;
        for (ClassType exceptionType : exceptionTypes) {
          Optional<SootClass> exceptionClassOp = view.getClass(exceptionType);
          SootClass exceptionClass;
          if (exceptionClassOp.isPresent()) {
            exceptionClass = exceptionClassOp.get();
          } else {
            throw new RuntimeException(
                "ExceptionType: \"" + exceptionType + "\" is not in the view");
          }
          if (exceptionClass.isPhantomClass()) {
            return throwable;
          } else if (type == null) {
            type = exceptionType;
          } else {
            type = getLeastCommonExceptionType(type, exceptionType);
          }
        }
        if (type == null) {
          throw new RuntimeException("Invaluable reference in AugEvalFunction: " + value);
        }
        return type;
      } else if (value instanceof JArrayRef) {
        Type type = typing.getType(((JArrayRef) value).getBase());
        if (type instanceof ArrayType) {
          return ((ArrayType) type).getBaseType();
        } else if (type instanceof ClassType) {
          String name = ((ClassType) type).getFullyQualifiedName();
          Type retType;
          switch (name) {
            case "java.lang.Object":
              retType = factory.getClassType("java.lang.Object");
              break;
            case "java.lang.Cloneable":
              retType = factory.getClassType("java.lang.Cloneable");
              break;
            case "java.io.Serializable":
              retType = factory.getClassType("java.io.Serializable");
              break;
            default:
              retType = BottomType.getInstance();
          }
          return retType;
        } else {
          return BottomType.getInstance();
        }
      } else if (value instanceof JThisRef
          || value instanceof JParameterRef
          || value instanceof JFieldRef) {
        return value.getType();
      } else {
        throw new RuntimeException("Invaluable reference in AugEvalFunction: " + value);
      }
    }
    return null;
  }

  /**
   * This function is used to get all exception types for the traps handled by the given handle
   * statement in body.
   */
  private Set<ClassType> getExceptionType(Stmt handleStmt, Body body) {
    Set<ClassType> exceptionTypes = new HashSet<>();
    for (Trap trap : body.getTraps()) {
      if (trap.getHandlerStmt() == handleStmt) {
        exceptionTypes.add(trap.getExceptionType());
      }
    }
    return exceptionTypes;
  }

  /**
   * This function is used to retrieve the path from the type "Throwable" to the given exception
   * type
   */
  private Deque<ClassType> getExceptionPath(ClassType exceptionType) {
    ViewTypeHierarchy hierarchy = new ViewTypeHierarchy(view);
    ClassType throwable = factory.getClassType("java.lang.Throwable");
    Deque<ClassType> path = new ArrayDeque<>();
    path.push(exceptionType);

    while (!exceptionType.equals(throwable)) {
      ClassType superType = hierarchy.directlySuperClassOf(exceptionType);
      if (superType != null) {
        path.push(superType);
        exceptionType = superType;
      } else {
        throw new RuntimeException(
            "The path from " + exceptionType + " to java.lang.Throwable cannot be found!");
      }
    }
    return path;
  }

  /**
   * This function is used to get least common type for two exception types
   *
   * @param a an exception type
   * @param b an exception type
   */
  private ClassType getLeastCommonExceptionType(ClassType a, ClassType b) {
    if (a.equals(b)) {
      return a;
    }
    ClassType commonType = null;
    Deque<ClassType> pathA = getExceptionPath(a);
    Deque<ClassType> pathB = getExceptionPath(b);
    while (!pathA.isEmpty() && !pathB.isEmpty() && pathA.getFirst().equals(pathB.getFirst())) {
      commonType = pathA.removeFirst();
      pathB.removeFirst();
    }
    return commonType;
  }
}