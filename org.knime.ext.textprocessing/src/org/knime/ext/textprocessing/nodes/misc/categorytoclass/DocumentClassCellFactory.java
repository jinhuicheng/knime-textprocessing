/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   25.06.2008 (thiel): created
 */
package org.knime.ext.textprocessing.nodes.misc.categorytoclass;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.ExecutionMonitor;
import org.knime.ext.textprocessing.data.Document;
import org.knime.ext.textprocessing.data.DocumentCategory;
import org.knime.ext.textprocessing.data.DocumentValue;

/**
 *
 * @author Kilian Thiel, University of Konstanz
 */
public class DocumentClassCellFactory implements CellFactory {

    private static final String UNDEFINED = "undefined";

    private int m_documentCellIndex = -1;

    /**
     * Creates a new instance of <code>DocumentClassCellFactory</code> with
     * given index of the column containing <code>DocumentCell</code>s.
     * @param documentCellIndex the index of the column containing
     * <code>DocumentCell</code>s.
     */
    public DocumentClassCellFactory(final int documentCellIndex) {
        m_documentCellIndex = documentCellIndex;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataCell[] getCells(final DataRow row) {
        if (!row.getCell(m_documentCellIndex).isMissing()) {
            StringCell classCell;
            Document doc = ((DocumentValue)row.getCell(m_documentCellIndex))
                            .getDocument();

            Set<DocumentCategory> cats = doc.getCategories();
            List<DocumentCategory> catsList = new ArrayList<DocumentCategory>(cats);
            Collections.sort(catsList, new Comparator <DocumentCategory>() {
                @Override
                public int compare(final DocumentCategory o1,
                        final DocumentCategory o2) {
                    if (o1 != null && o2 != null) {
                        return o1.getCategoryName().compareTo(o2.getCategoryName());
                    }
                    return 0;
                }
            });

            String cat = UNDEFINED;
            if (catsList.size() > 0) {
                cat = catsList.get(0).getCategoryName();
            }
            classCell = new StringCell(cat);
            return new DataCell[]{classCell};
        } else {
            return new DataCell[]{DataType.getMissingCell()};
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataColumnSpec[] getColumnSpecs() {
        DataColumnSpec classCell = new DataColumnSpecCreator("Document class",
                    StringCell.TYPE).createSpec();
        return new DataColumnSpec[]{classCell};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setProgress(final int curRowNr, final int rowCount,
            final RowKey lastKey, final ExecutionMonitor exec) {
        double prog = (double)curRowNr / (double)rowCount;
        exec.setProgress(prog, "Addig class of row: " + curRowNr
                + " of " + rowCount + " rows");
    }
}
