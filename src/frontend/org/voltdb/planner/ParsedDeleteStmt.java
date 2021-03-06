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

package org.voltdb.planner;

import org.hsqldb_voltpatches.VoltXMLElement;
import org.voltdb.catalog.Database;
import org.voltdb.catalog.Table;
import org.voltdb.expressions.AbstractExpression;
import org.voltdb.expressions.ExpressionUtil;

/**
 *
 *
 */
public class ParsedDeleteStmt extends AbstractParsedStmt {
    Table table = null;

    /**
    * Class constructor
    * @param paramValues
    * @param db
    */
    public ParsedDeleteStmt(String[] paramValues, Database db) {
        super(paramValues, db);
    }

    @Override
    void parse(VoltXMLElement stmtNode) {
        String tableName = stmtNode.attributes.get("table");
        assert(tableName != null);
        tableName = tableName.trim();
        table = getTableFromDB(tableName);
        tableList.add(table);

        for (VoltXMLElement child : stmtNode.children) {
            if (child.name.equalsIgnoreCase("condition"))
                parseCondition(child);
        }
    }

    //XXX: This looks a lot like it might be a slightly more verbose duplicate of AbstractParsedStmt.parseConditions
    private void parseCondition(VoltXMLElement conditionNode) {
        AbstractExpression tempWhere = null;
        for (VoltXMLElement exprNode : conditionNode.children) {
            if (tempWhere == null) {
                tempWhere = parseExpressionTree(exprNode);
            }
            else {
                tempWhere = ExpressionUtil.combine(tempWhere, parseExpressionTree(exprNode));
            }
        }
        assert(where == null); // Should be non-reentrant -- never overwriting a previous value!
        where = tempWhere;
        if (where == null) {
            return;
        }
        ExpressionUtil.finalizeValueTypes(where);
    }

    @Override
    public String toString() {
        String retval = super.toString() + "\n";

        retval = retval.trim();

        return retval;
    }
}
