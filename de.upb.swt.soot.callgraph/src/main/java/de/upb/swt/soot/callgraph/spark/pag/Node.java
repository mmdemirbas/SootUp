package de.upb.swt.soot.callgraph.spark.pag;

/*-
 * #%L
 * Soot - a J*va Optimization Framework
 * %%
 * Copyright (C) 2002-2021 Ondrej Lhotak, Kadiray Karakaya and others
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

import de.upb.swt.soot.callgraph.spark.sets.EmptyPointsToSet;
import de.upb.swt.soot.callgraph.spark.sets.PointsToSetInternal;
import de.upb.swt.soot.core.types.Type;

/**
 * Represents every node in the pointer assignment graph.
 *
 * @author Kadiray Karakaya
 */
public class Node implements ReferenceVariable{
    private int number = 0;
    protected Type type;
    protected Node replacement;
    protected PointerAssignmentGraph pag;
    protected PointsToSetInternal p2set;

    public final int getNumber() {
        return number;
    }

    public final void setNumber(int number) {
        this.number = number;
    }

    /** Creates a new node of pointer assignment graph pag, with type type. */
    Node(PointerAssignmentGraph pag, Type type) {
//        if (TypeManager.isUnresolved(type)) {
//            throw new RuntimeException("Unresolved type " + type);
//        }
        this.type = type;
        this.pag = pag;
        replacement = this;
    }

    /** Returns the declared type of this node, null for unknown. */
    public Type getType() {
        return type;
    }

    /** Sets the declared type of this node, null for unknown. */
    public void setType(Type type) {
//        if (TypeManager.isUnresolved(type)) {
//            throw new RuntimeException("Unresolved type " + type);
//        }
        this.type = type;
    }

    /**
     * If this node has been merged with another, returns the new node to be used as the representative of this node; returns
     * this if the node has not been merged.
     */
    public Node getReplacement() {
        if (replacement != replacement.replacement) {
            replacement = replacement.getReplacement();
        }
        return replacement;
    }

    /** Merge with the node other. */
    public void mergeWith(Node other) {
        if (other.replacement != other) {
            throw new RuntimeException("Shouldn't happen");
        }
        Node myRep = getReplacement();
        if (other == myRep) {
            return;
        }
        other.replacement = myRep;
        if (other.p2set != p2set && other.p2set != null && !other.p2set.isEmpty()) {
            if (myRep.p2set == null || myRep.p2set.isEmpty()) {
                myRep.p2set = other.p2set;
            } else {
                myRep.p2set.mergeWith(other.p2set);
            }
        }
        other.p2set = null;
        pag.mergedWith(myRep, other);
        if ((other instanceof VarNode) && (myRep instanceof VarNode) && ((VarNode) other).isInterProcTarget()) {
            ((VarNode) myRep).setInterProcTarget();
        }
    }

    /** Returns the points-to set for this node. */
    public PointsToSetInternal getP2Set() {
        if (p2set != null) {
            if (replacement != this) {
                throw new RuntimeException("Node " + this + " has replacement " + replacement + " but has p2set");
            }
            return p2set;
        }
        Node rep = getReplacement();
        if (rep == this) {
            return EmptyPointsToSet.v();
        }
        return rep.getP2Set();
    }

    /** Returns the points-to set for this node, makes it if necessary. */
    public PointsToSetInternal makeP2Set() {
        if (p2set != null) {
            if (replacement != this) {
                throw new RuntimeException("Node " + this + " has replacement " + replacement + " but has p2set");
            }
            return p2set;
        }
        Node rep = getReplacement();
        if (rep == this) {
            p2set = pag.getSetFactory().newSet(type, pag);
        }
        return rep.makeP2Set();
    }

    /** Returns the pointer assignment graph that this node is a part of. */
    public PointerAssignmentGraph getPag() {
        return pag;
    }

    /** Delete current points-to set and make a new one */
    public void discardP2Set() {
        p2set = null;
    }

    /** Use the specified points-to set to replace current one */
    public void setP2Set(PointsToSetInternal ptsInternal) {
        p2set = ptsInternal;
    }


    public final int hashCode() {
        return number;
    }

    public final boolean equals(Object other) {
        return this == other;
    }
}