/* This file is part of VoltDB.
 * Copyright (C) 2008-2013 VoltDB Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with VoltDB.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.voltdb.planner.microoptimizations;

import java.util.ArrayList;
import java.util.List;

import org.voltdb.catalog.Database;
import org.voltdb.catalog.Index;
import org.voltdb.catalog.Table;
import org.voltdb.planner.CompiledPlan;
import org.voltdb.plannodes.AbstractPlanNode;
import org.voltdb.plannodes.IndexScanPlanNode;
import org.voltdb.plannodes.SchemaColumn;
import org.voltdb.plannodes.SeqScanPlanNode;
import org.voltdb.types.IndexLookupType;
import org.voltdb.types.IndexType;
import org.voltdb.types.SortDirectionType;

public class SeqScansToUniqueTreeScans implements MicroOptimization {

    @Override
    public List<CompiledPlan> apply(CompiledPlan plan, Database db) {
        ArrayList<CompiledPlan> retval = new ArrayList<CompiledPlan>();

        AbstractPlanNode planGraph = plan.rootPlanGraph;
        planGraph = recursivelyApply(planGraph, db);
        plan.rootPlanGraph = planGraph;

        retval.add(plan);
        return retval;
    }

    AbstractPlanNode recursivelyApply(AbstractPlanNode plan, Database db)
    {
        assert(plan != null);

        // depth first:
        //     Find Sequential Scan node.
        //     Replace any unique tree index scan if possible.

        ArrayList<AbstractPlanNode> children = new ArrayList<AbstractPlanNode>();

        for (int i = 0; i < plan.getChildCount(); i++)
            children.add(plan.getChild(i));

        for (AbstractPlanNode child : children) {
            // TODO this will break when children feed multiple parents
            AbstractPlanNode newChild = recursivelyApply(child, db);
            // Do a graft into the (parent) plan only if a replacement for a child was found.
            if (newChild == child) {
                continue;
            }
            child.removeFromGraph();
            plan.addAndLinkChild(newChild);
        }

        // check for an aggregation of the right form
        if ((plan instanceof SeqScanPlanNode) == false) {
            return plan;
        }
        assert(plan.getChildCount() == 0);

        SeqScanPlanNode scanNode = (SeqScanPlanNode) plan;

        String tableName = scanNode.getTargetTableName();
        Table table = db.getTables().get(tableName);
        assert(table != null);

        Index indexToScan = null;

        for (Index index : table.getIndexes()) {
            // skip non-unique indexes
            if (index.getUnique() == false) {
                continue;
            }
            // skip hash indexes
            else if (index.getType() != IndexType.BALANCED_TREE.getValue()) {
                continue;
            }
            else {
                indexToScan = index;
                break;
            }
        }

        if (indexToScan == null) {
            return plan;
        }

        IndexScanPlanNode indexScanNode = new IndexScanPlanNode();
        indexScanNode.setTargetTableName(scanNode.getTargetTableName());
        indexScanNode.setTargetTableAlias(scanNode.getTargetTableAlias());
        indexScanNode.setEndExpression(null);
        //SchemaColumn
        indexScanNode.setScanColumns(new ArrayList<SchemaColumn>());
        indexScanNode.setCatalogIndex(indexToScan);
        indexScanNode.setKeyIterate(true);
        indexScanNode.setTargetIndexName(indexToScan.getTypeName());
        indexScanNode.setLookupType(IndexLookupType.GTE);
        indexScanNode.setSortDirection(SortDirectionType.ASC);
        indexScanNode.setPredicate(scanNode.getPredicate());
        for (AbstractPlanNode inlineNode : scanNode.getInlinePlanNodes().values()) {
            indexScanNode.addInlinePlanNode(inlineNode);
        }
        indexScanNode.generateOutputSchema(db);

        return indexScanNode;
    }

}