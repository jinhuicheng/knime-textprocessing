/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ---------------------------------------------------------------------
 *
 * History
 *   03.03.2008 (Kilian Thiel): created
 */
package org.knime.ext.textprocessing.nodes.transformation.bow;

import org.knime.base.data.sort.SortedTable;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.ext.textprocessing.data.Document;
import org.knime.ext.textprocessing.data.DocumentValue;
import org.knime.ext.textprocessing.data.Sentence;
import org.knime.ext.textprocessing.data.Term;
import org.knime.ext.textprocessing.util.BagOfWordsDataTableBuilder;
import org.knime.ext.textprocessing.util.DataTableSpecVerifier;
import org.knime.ext.textprocessing.util.TextContainerDataCellFactory;
import org.knime.ext.textprocessing.util.TextContainerDataCellFactoryBuilder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * The model class of the Bag of word creator node. The method
 * {@link BagOfWordsNodeModel#configure(DataTableSpec[])} validates the incoming
 * {@link org.knime.core.data.DataTableSpec}. Only one column containing
 * {@link org.knime.ext.textprocessing.data.DocumentCell}s is allowed.
 * The output table contains two columns. The first consists of
 * {@link org.knime.ext.textprocessing.data.TermCell}s the second of
 * {@link org.knime.ext.textprocessing.data.DocumentCell}s. A single row
 * represents a tupel of a term and a document meaning that the term is
 * contained in the document.
 *
 * @author Kilian Thiel, University of Konstanz
 */
public class BagOfWordsNodeModel extends NodeModel {

    private int m_documentColIndex = -1;

    private BagOfWordsDataTableBuilder m_dtBuilder = 
        new BagOfWordsDataTableBuilder();
    
    private TextContainerDataCellFactory m_termFac = 
        TextContainerDataCellFactoryBuilder.createTermCellFactory();

    /**
     * Creates a new instance of <code>BagOfWordsNodeModel</code> with one in
     * and one data table out port.
     */
    public BagOfWordsNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        checkDataTableSpec(inSpecs[0]);
        return new DataTableSpec[]{m_dtBuilder.createDataTableSpec()};
    }

    private void checkDataTableSpec(final DataTableSpec spec)
    throws InvalidSettingsException {
        DataTableSpecVerifier verifier = new DataTableSpecVerifier(spec);
        verifier.verifyDocumentCell(true);
        m_documentColIndex = verifier.getDocumentCellIndex();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        checkDataTableSpec(inData[0].getDataTableSpec());

        // sort list of documents
        ExecutionContext subExec = exec.createSubExecutionContext(0.3);
        List<String> sortBy = new ArrayList<String>();
        sortBy.add(inData[0].getDataTableSpec().getColumnSpec(
                m_documentColIndex).getName());
        BufferedDataTable sortedTable = new SortedTable(inData[0], sortBy,
                new boolean[]{false}, false, subExec).getBufferedDataTable();

        // prepare data container
        BufferedDataContainer bdc = exec.createDataContainer(
                m_dtBuilder.createDataTableSpec());

        ExecutionContext subExec2 = exec.createSubExecutionContext(0.7);
        Document currDoc = null;
        Document lastDoc = null;
        int rowCount = sortedTable.getRowCount();
        int currRow = 1;
        int rowId = 1;
        RowIterator it = sortedTable.iterator();
        while (it.hasNext()) {
            DataRow row = it.next();

            // get terms for document
            DataCell docCell = row.getCell(m_documentColIndex);
            DocumentValue docVal = (DocumentValue)row
                    .getCell(m_documentColIndex);

            currDoc = docVal.getDocument();
            if (lastDoc == null) {
                lastDoc = currDoc;
            }
            if (!currDoc.equals(lastDoc)) {
                lastDoc = currDoc;

                Set<Term> terms = setOfTerms(currDoc);
                for(Term t : terms) {
                    RowKey key = RowKey.createRowKey(rowId);
                    DataCell tc = m_termFac.createDataCell(t);
                    DataRow newRow = new DefaultRow(key, tc, docCell);
                    bdc.addRowToTable(newRow);
                    rowId++;
                }
            }

            // report status
            double progress = (double)currRow / (double)rowCount;
            subExec2.setProgress(progress, "Processing document " + currRow
                    + " of " + rowCount);
            exec.checkCanceled();
            currRow++;
        }
        bdc.close();
        return new BufferedDataTable[]{bdc.getTable()};
    }


    private Set<Term> setOfTerms(final Document doc) {
        Set<Term> termSet = null;
        if (doc != null) {
            termSet = new HashSet<Term>();

            Iterator<Sentence> it = doc.sentenceIterator();
            while (it.hasNext()) {
                Sentence sen = it.next();
                termSet.addAll(sen.getTerms());
            }
        }
        return termSet;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    }



    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
    }
}
