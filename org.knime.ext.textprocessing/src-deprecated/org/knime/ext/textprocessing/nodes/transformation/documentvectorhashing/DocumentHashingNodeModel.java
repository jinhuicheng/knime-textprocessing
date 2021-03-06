/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
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
 *   10.08.2016 (andisadewi): created
 */
package org.knime.ext.textprocessing.nodes.transformation.documentvectorhashing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.streamable.simple.SimpleStreamableFunctionNodeModel;
import org.knime.ext.textprocessing.data.Document;
import org.knime.ext.textprocessing.data.DocumentValue;
import org.knime.ext.textprocessing.data.Paragraph;
import org.knime.ext.textprocessing.data.Section;
import org.knime.ext.textprocessing.data.Sentence;
import org.knime.ext.textprocessing.data.Term;
import org.knime.ext.textprocessing.util.BagOfWordsDataTableBuilder;
import org.knime.ext.textprocessing.util.DataTableSpecVerifier;
import org.knime.ext.textprocessing.util.DocumentDataTableBuilder;

/**
 * The node model of the Document vector hashing node. This model extends
 * {@link org.knime.core.node.streamable.simple.SimpleStreamableFunctionNodeModel} and is streamable.
 *
 * @author Tobias Koetter and Andisa Dewi, KNIME.com, Berlin, Germany
 * @since 3.3
 * @deprecated Use {@link DocumentHashingNodeModel2} instead
 */
@Deprecated
public class DocumentHashingNodeModel extends SimpleStreamableFunctionNodeModel {

    /**
     * The default document column to use.
     */
    public static final String DEFAULT_DOCUMENT_COLNAME = BagOfWordsDataTableBuilder.DEF_ORIG_DOCUMENT_COLNAME;

    /**
     * The default value to the as collection flag.
     */
    public static final boolean DEFAULT_ASCOLLECTION = false;

    /**
     * Default seed value
     */
    protected static final int DEFAULT_SEED = new Random().nextInt();

    private int m_documentColIndex = -1;

    private static final DoubleCell DEFAULT_CELL = new DoubleCell(0.0);

    private final SettingsModelIntegerBounded m_dim = DocumentHashingNodeDialog.getDimModel();

    private final SettingsModelString m_docCol = DocumentHashingNodeDialog.getDocumentColModel();

    private SettingsModelInteger m_seed = DocumentHashingNodeDialog.getSeedModel();

    private final SettingsModelString m_vectVal = DocumentHashingNodeDialog.getVectorValueModel();

    private final SettingsModelString m_hashFunc = DocumentHashingNodeDialog.getHashingMethod();

    private final SettingsModelBoolean m_asCol = DocumentHashingNodeDialog.getAsCollectionModel();

    /**
     * Creates a new instance of <code>DocumentHashingNodeModel</code>. For each node, a new integer value is assigned
     * as initial value of the seed
     */
    public DocumentHashingNodeModel() {
        m_seed.setIntValue(new Random().nextInt());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_dim.saveSettingsTo(settings);
        m_docCol.saveSettingsTo(settings);
        m_seed.saveSettingsTo(settings);
        m_vectVal.saveSettingsTo(settings);
        m_hashFunc.saveSettingsTo(settings);
        m_asCol.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_dim.validateSettings(settings);
        m_docCol.validateSettings(settings);
        m_seed.validateSettings(settings);
        m_vectVal.validateSettings(settings);
        m_hashFunc.validateSettings(settings);
        m_asCol.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_dim.loadSettingsFrom(settings);
        m_docCol.loadSettingsFrom(settings);
        m_seed.loadSettingsFrom(settings);
        m_vectVal.loadSettingsFrom(settings);
        m_hashFunc.loadSettingsFrom(settings);
        m_asCol.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ColumnRearranger createColumnRearranger(final DataTableSpec spec) throws InvalidSettingsException {
        final DataTableSpecVerifier verifier = new DataTableSpecVerifier(spec);
        verifier.verifyMinimumDocumentCells(1, true);

        m_documentColIndex = spec.findColumnIndex(m_docCol.getStringValue());
        if (m_documentColIndex < 0) {
            throw new InvalidSettingsException(
                "Index of specified document column is not valid! " + "Check your settings!");
        }

        final ColumnRearranger rearranger = new ColumnRearranger(spec);

        DataColumnSpec[] specs = m_asCol.getBooleanValue() ? createSpecAsCollection() : createSpecAsColumns();
        rearranger.append(new AbstractCellFactory(specs) {

            private final int m_idx = spec.findColumnIndex(m_docCol.getStringValue());

            @Override
            public DataCell[] getCells(final DataRow row) {
                final DocumentValue doc = (DocumentValue)row.getCell(m_idx);
                return createVector(m_dim.getIntValue(), doc.getDocument());
            }

        });

        return rearranger;
    }

    private DataCell[] createVector(final int dim, final Document doc) {
        Double totalTerms = 0.0;
        final HashingFunction hashFunction =
            HashingFunctionFactory.getInstance().getHashFunction(m_hashFunc.getStringValue());

        List<Integer> output = new ArrayList<Integer>(Collections.nCopies(dim, 0));
        List<Integer> occupiedIndexes = new ArrayList<Integer>();

        for (final Section s : doc.getSections()) {
            for (final Paragraph p : s.getParagraphs()) {
                final List<Sentence> sentences = p.getSentences();
                for (final Sentence sentence : sentences) {
                    final List<Term> terms = sentence.getTerms();
                    for (final Term term : terms) {
                        int idx = hashFunction.hash(term.getText(), m_seed.getIntValue()) % dim;

                        if (idx < 0) {
                            idx += dim;
                        }
                        output.set(idx, output.get(idx)+1);
                        if (!occupiedIndexes.contains(idx)) {
                            occupiedIndexes.add(idx);
                        }
                    }
                }
            }
        }
        for (int index : occupiedIndexes) {
            totalTerms += output.get(index);
        }
        DataCell[] cells;
        final DoubleCell zero = new DoubleCell(0);
        final DoubleCell one = new DoubleCell(1);

        if (m_asCol.getBooleanValue()) {
            List<DoubleCell> featureVector = initFeatureVector(dim);
            cells = new DataCell[1];

            if (m_vectVal.getStringValue().equals("Binary")) {
                for (int i = 0, length = featureVector.size(); i < length; i++) {
                    featureVector.set(i, output.get(i) > 0 ? one : zero);
                }
            } else if (m_vectVal.getStringValue().equals("TF-Absolute")) {
                for (int i = 0, length = featureVector.size(); i < length; i++) {
                    if (occupiedIndexes.contains(i)) {
                        featureVector.add(i, new DoubleCell(output.get(i)));
                    }
                }
            } else if (m_vectVal.getStringValue().equals("TF-Relative")) {
                for (int i = 0, length = featureVector.size(); i < length; i++) {
                    if (occupiedIndexes.contains(i)) {
                        featureVector.add(i, new DoubleCell(output.get(i) / totalTerms));
                    }
                }
            }

            cells[0] = CollectionCellFactory.createSparseListCell(featureVector, DEFAULT_CELL);

        } else {
            cells = new DataCell[dim];

            if (m_vectVal.getStringValue().equals("Binary")) {
                for (int i = 0, length = cells.length; i < length; i++) {
                    cells[i] = output.get(i) > 0 ? one : zero;
                }
            } else if (m_vectVal.getStringValue().equals("TF-Absolute")) {
                for (int i = 0, length = cells.length; i < length; i++) {
                    if (occupiedIndexes.contains(i)) {
                        cells[i] = new DoubleCell(output.get(i));
                    } else {
                        cells[i] = DEFAULT_CELL;
                    }
                }
            } else if (m_vectVal.getStringValue().equals("TF-Relative")) {
                for (int i = 0, length = cells.length; i < length; i++) {
                    if (occupiedIndexes.contains(i)) {
                        cells[i] = new DoubleCell(output.get(i) / totalTerms);
                    } else {
                        cells[i] = DEFAULT_CELL;
                    }
                }
            }
        }
        return cells;
    }

    private List<DoubleCell> initFeatureVector(final int size) {
        final List<DoubleCell> featureVector = new ArrayList<DoubleCell>(size);
        for (int i = 0; i < size; i++) {
            featureVector.add(i, DEFAULT_CELL);
        }
        return featureVector;
    }

    private DataColumnSpec[] createSpecAsColumns() {
        DataColumnSpecCreator creator = new DataColumnSpecCreator("Col1", DoubleCell.TYPE);
        final int dim = m_dim.getIntValue();
        final DataColumnSpec[] specs = new DataColumnSpec[dim];

        for (int i = 0; i < dim; i++) {
            creator.setName("Col" + i);
            specs[i] = creator.createSpec();
        }

        return specs;
    }

    private DataColumnSpec[] createSpecAsCollection() {
        final int dim = m_dim.getIntValue();
        DataColumnSpec[] columnSpecs;
        columnSpecs = new DataColumnSpec[1];

        // add feature vector columns
        DataColumnSpecCreator columnSpecCreator = new DataColumnSpecCreator(
            DocumentDataTableBuilder.DEF_DOCUMENT_VECTOR_COLNAME, ListCell.getCollectionType(DoubleCell.TYPE));

        String[] elemNames = new String[dim];
        for (int i = 0; i < dim; i++) {
            elemNames[i] = "Col" + i;
        }
        columnSpecCreator.setElementNames(elemNames);
        columnSpecs[0] = columnSpecCreator.createSpec();

        return columnSpecs;
    }

}
