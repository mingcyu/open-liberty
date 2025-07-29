/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jakarta.data.restrict;

import java.util.List;

/**
 * Method signatures are copied from Jakarta Data.
 */
class Unrestricted<T> implements CompositeRestriction<T> {
    static final Unrestricted<?> INSTANCE = new Unrestricted<>();

    private Unrestricted() {
    }

    @Override
    public boolean isNegated() {
        return false;
    }

    @Override
    public CompositeRestriction<T> negate() {
        @SuppressWarnings("unchecked")
        CompositeRestriction<T> r = (CompositeRestriction<T>) Unmatchable.INSTANCE;
        return r;
    }

    @Override
    public List<Restriction<? super T>> restrictions() {
        return List.of();
    }

    @Override
    public String toString() {
        return "UNRESTRICTED";
    }

    @Override
    public Type type() {
        return Type.ALL;
    }

}
