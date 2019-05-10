/*
 * Licensed to Crate under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.  Crate licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial
 * agreement.
 */

package io.crate.planner.optimizer.rule;

import io.crate.collections.Lists2;
import io.crate.expression.symbol.Symbol;
import io.crate.planner.operators.LogicalPlan;
import io.crate.planner.optimizer.Rule;
import io.crate.planner.optimizer.matcher.Captures;
import io.crate.planner.optimizer.matcher.Pattern;

import java.util.Collection;
import java.util.HashSet;

import static io.crate.planner.operators.LogicalPlanner.extractColumns;
import static io.crate.planner.optimizer.matcher.Pattern.typeOf;

public final class PruneColumnsAndMaybeFetch implements Rule<LogicalPlan> {

    private final Pattern<LogicalPlan> pattern;

    public PruneColumnsAndMaybeFetch() {
        this.pattern = typeOf(LogicalPlan.class)
            .with(p -> !p.sources().isEmpty() && hasUnusedOutputs(p));
    }

    private static boolean hasUnusedOutputs(LogicalPlan plan) {
        return plan.usedColumns().size() < plan.outputs().size()
               || plan.usedColumns().size() < extractColumns(plan.outputs()).size();
    }

    @Override
    public Pattern<LogicalPlan> pattern() {
        return pattern;
    }

    @Override
    public LogicalPlan apply(LogicalPlan plan, Captures captures) {
        Collection<Symbol> usedColumns = plan.usedColumns();
        HashSet<Symbol> fetchCandidates = new HashSet<>(plan.outputs());
        fetchCandidates.removeAll(usedColumns);
        return plan.replaceSources(Lists2.map(plan.sources(), p -> p.pruneOutputs(p.usedColumns(), fetchCandidates)));
    }
}