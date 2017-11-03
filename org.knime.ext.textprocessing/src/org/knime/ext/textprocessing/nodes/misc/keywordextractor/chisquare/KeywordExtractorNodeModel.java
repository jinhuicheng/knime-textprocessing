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
 *   Jun 10, 2008 (Pierre-Francois Laquerre): created
 */
package org.knime.ext.textprocessing.nodes.misc.keywordextractor.chisquare;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.ext.textprocessing.data.Document;
import org.knime.ext.textprocessing.data.DocumentValue;
import org.knime.ext.textprocessing.data.Term;
import org.knime.ext.textprocessing.util.ColumnSelectionVerifier;
import org.knime.ext.textprocessing.util.DataCellCache;
import org.knime.ext.textprocessing.util.DataTableSpecVerifier;
import org.knime.ext.textprocessing.util.DocumentUtil;
import org.knime.ext.textprocessing.util.FrequencyMap;
import org.knime.ext.textprocessing.util.LRUDataCellCache;
import org.knime.ext.textprocessing.util.Maps;
import org.knime.ext.textprocessing.util.TextContainerDataCellFactory;
import org.knime.ext.textprocessing.util.TextContainerDataCellFactoryBuilder;
import org.knime.ext.textprocessing.util.clustering.Cluster;
import org.knime.ext.textprocessing.util.clustering.ClusteringAlgorithm;
import org.knime.ext.textprocessing.util.clustering.GreedyClustering;
import org.knime.ext.textprocessing.util.similarity.NormalizedL1;
import org.knime.ext.textprocessing.util.similarity.OrCombination;
import org.knime.ext.textprocessing.util.similarity.PointwiseMutualInformation;
import org.knime.ext.textprocessing.util.similarity.SimilarityMeasure;

/**
 * Extracts keywords from a document according to the method presented in
 * "Keyword extraction from a single document using word co-occurrence
 * statistical information" by Y. Matsuo and M. Ishizuka.
 *
 * @author Pierre-Francois Laquerre, University of Konstanz
 */
public class KeywordExtractorNodeModel extends NodeModel {

    /**
     * How many terms should be considered for the frequent term set? This is
     * calculated as a proportion of the total number of unique terms in the
     * document.
     * Valid range: ]0, 100]
     */
    private SettingsModelIntegerBounded m_frequentTermsProportion =
        KeywordExtractorNodeDialog.createSetFrequentTermsProportionModel();

    /**
     * How many keywords should we extract? If this value is larger than the
     * number of unique terms in a document, fewer keywords will be returned.
     */
    private SettingsModelIntegerBounded m_nrKeywords =
        KeywordExtractorNodeDialog.createSetNrKeywordsModel();

    /**
     * When true, a copy of the input document will be created during internal
     * processing. This copy contains the same elements as the original with the
     * exception that the terms will not have any tags (essentially making
     * .equals() behave like .equalsOnlyWords(). The documents returned by
     * execute() will not be affected - this is solely for internal use.
     */
    private SettingsModelBoolean m_ignoreTermTags =
        KeywordExtractorNodeDialog.createSetIgnoreTermTagsModel();

    /**
     * Which column holds the documents to analyse?
     */
    private SettingsModelString m_documentColumnName =
        KeywordExtractorNodeDialog.createSetDocumentColumnNameModel();

    /**
     * Inclusive threshold over which two terms are considered as similar.
     */
    private SettingsModelDoubleBounded m_PMIThreshold =
        KeywordExtractorNodeDialog.createSetPMIThresholdModel();

    /**
     * Inclusive threshold over which two cooccurrence probability distributions
     *  are considered as similar.
     */
    private SettingsModelDoubleBounded m_L1Threshold =
        KeywordExtractorNodeDialog.createSetL1ThresholdModel();

    private NodeLogger m_logger = org.knime.core.node.NodeLogger.getLogger(getClass());

    /**
     * One input port, one output port.
     */
    public KeywordExtractorNodeModel() {
        super(1, 1);
    }

    /**
     * For each unique document in the input table, a set of
     * (Term, Double, Document) tuples will be output, where the set of Terms is
     * the keywords that were extracted and the Double value is their chi-square
     * deviation.
     *
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        checkDataTableSpec(inData[0].getDataTableSpec());
        Set<Document> documents = DocumentUtil.extractUniqueDocuments(
                inData[0], exec.createSubProgress(0.1),
                m_documentColumnName.getStringValue());

        Map<Document, Map<Term, Double>> keywords =
                new LinkedHashMap<Document, Map<Term, Double>>();

        // Process each document
        int i = 1;
        int nbdocs = documents.size();
        ExecutionMonitor subExecDocs = exec.createSubProgress(0.9);
        for (Document doc : documents) {
            exec.checkCanceled();
            ExecutionMonitor subDoc = subExecDocs.createSubProgress(
                    1.0 / nbdocs);

            subExecDocs.setProgress("Processing document " + i + " of "
                    + nbdocs);

            if (m_ignoreTermTags.getBooleanValue()) {
                keywords.put(doc, extractKeywords(DocumentUtil
                        .stripTermTags(doc), subDoc));
            } else {
                keywords.put(doc, extractKeywords(doc, subDoc));
            }

            ++i;
        }

        return new BufferedDataTable[]{buildResultTable(keywords, exec)};
    }

    /**
     * Extracts keywords from a document according to the method presented in
     * "Keyword extraction from a single document using word co-occurrence
     * statistical information" by Y. Matsuo and M. Ishizuka.
     *
     * The document's m_frequentTermsProportion% most frequent terms are
     * clustered. Terms are then sorted by the difference between their expected
     * probability of cooccurrence with the clusters and the actual observed
     * values. The terms with the top m_nrKeywords values will be returned as
     * keywords.
     *
     * @param doc the document to extract the keywords from
     * @param subDoc an ExecutionMonitor to report on the progress
     * @return a set of keywords
     */
    private final Map<Term, Double> extractKeywords(
            final Document doc, final ExecutionMonitor subDoc) {
        subDoc.setProgress(0.0, "Analysing the document");

        TermEvent e = new TermEvent(doc,
                (double)m_frequentTermsProportion.getIntValue() / 100);

        subDoc.setProgress(0.1, "Clustering the frequent terms");
        Set<Term> frequentTerms = e.getTopFrequentTerms();
        if (m_logger.isDebugEnabled()) {
            m_logger.debug("Frequent terms: " + frequentTerms.toString());
        }

        ClusteringAlgorithm<Term> c = new GreedyClustering<Term>();

        Set<SimilarityMeasure<Term>> measures =
            new LinkedHashSet<SimilarityMeasure<Term>>();

        measures.add(new NormalizedL1<Term>(
                m_L1Threshold.getDoubleValue(), e));
        measures.add(new PointwiseMutualInformation<Term>(
                m_PMIThreshold.getDoubleValue(), e));
        SimilarityMeasure<Term> sim = new OrCombination<Term>(measures);

        Set<Cluster<Term>> clusters = c.cluster(frequentTerms, sim);
        if (m_logger.isDebugEnabled()) {
            m_logger.debug("Clusters");
            for (Cluster<Term> cluster : clusters) {
                m_logger.debug(cluster);
            }
        }

        subDoc.setProgress(0.8, "Calculating the chi square values");
        Map<Term, Double> chivalues = calculateChiSquare(doc, e, clusters);

        subDoc.setProgress(1, "Extracting the top deviations");
        return Maps.getTopValues(chivalues, m_nrKeywords.getIntValue());
    }

    /**
     * Calculates the total deviation for each term between the expected
     * cooccurrence frequency with each cluster and the actual observed value.
     *
     * A high value indicates that a term might be important.
     *
     * Observed frequency: the number of cooccurrences of the term and
     * the cluster in the document
     * Expected frequency: the probability of a term cooccurring with the
     * cluster times the total amount of cooccurrences for the term.
     *
     * @param doc the document
     * @param e the probability distributions for the term (co)occurrences
     * @param clusters the clusters against which to compare the observed values
     * @return a total deviation score for each term
     */
    private Map<Term, Double> calculateChiSquare(
            final Document doc, final Event<Term> e,
            final Set<Cluster<Term>> clusters) {
        // How many cooccurrences per cluster?
        FrequencyMap<Cluster<Term>> clusternbcoocs =
            DocumentUtil.getClusterNbCoocs(clusters, doc);

        // How many times does a specific term cooccur with a given cluster?
        Map<Cluster<Term>, FrequencyMap<Term>> clustercoocs =
            DocumentUtil.getClusterCoocs(clusters, doc);

        // Compute the expected probability of a term cooccurring with a cluster
        Map<Cluster<Term>, Double> expectedProbabilities =
            new HashMap<Cluster<Term>, Double>();

        int totalnrcoocs = DocumentUtil.getNrTotalCoocs(doc);
        for (Cluster<Term> c : clusters) {
            expectedProbabilities.put(
                    c, (double)clusternbcoocs.get(c) / totalnrcoocs);
        }

        // How many cooccurrences per term in total?
        FrequencyMap<Term> nrTermCoocs =
            DocumentUtil.getTotalCoocs(doc, e.getDomain());

        // Calculate the deviation for each term (chi-square)
        Map<Term, Double> chivalues = new LinkedHashMap<Term, Double>();

        for (Term term : e.getDomain()) {
            double chi = 0.0;
            double maximalTerm = 0.0;
            double val = 0.0;

            for (Cluster<Term> cluster : clusters) {
                double expected =
                    expectedProbabilities.get(cluster) * nrTermCoocs.get(term);
                int observed = clustercoocs.get(cluster).get(term);

                val = Math.pow(observed - expected, 2) / expected;
                chi += val;

                if (val > maximalTerm) {
                    maximalTerm = val;
                }
            }

            // Subtract the most extreme value
            if (!isNaN(chi)) {
                chivalues.put(term, chi - maximalTerm);
            } else {
                chivalues.put(term, 0.0);
            }
        }

        return chivalues;
    }

    /**
     * Builds a BufferedDataTable from a map of Document->{Keyword, Chi value}*.
     *
     * @param keywords the (documents, keyword, chi-value) tuples
     * @param exec an execution monitor to report on progress
     * @return a buffered data table with Keyword|Value|Document rows
     */
    private static BufferedDataTable buildResultTable(
            final Map<Document, Map<Term, Double>> keywords,
            final ExecutionContext exec) throws CanceledExecutionException {
        final TextContainerDataCellFactory termFac = TextContainerDataCellFactoryBuilder.createTermCellFactory();
        final TextContainerDataCellFactory docCellFac = TextContainerDataCellFactoryBuilder.createDocumentCellFactory();

        BufferedDataContainer con = exec.createDataContainer(createDataTableSpec());
        docCellFac.prepare(FileStoreFactory.createWorkflowFileStoreFactory(exec));
        final DataCellCache docCache = new LRUDataCellCache(docCellFac);

        try {
            int rowid = 0;
            for (Entry<Document, Map<Term, Double>> e : keywords.entrySet()) {
                exec.checkCanceled();
                Document doc = e.getKey();
                for (Entry<Term, Double> kw : e.getValue().entrySet()) {
                    DefaultRow row =
                        new DefaultRow(new RowKey(Integer.toString(rowid)), new DataCell[]{
                            termFac.createDataCell(kw.getKey()), new DoubleCell(kw.getValue()),
                            docCache.getInstance(doc)});
                    con.addRowToTable(row);
                    rowid++;
                }
            }
        } finally {
            con.close();
            docCache.close();
        }

        return con.getTable();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        checkDataTableSpec(inSpecs[0]);
        return new DataTableSpec[]{createDataTableSpec()};
    }

    /**
     * Ensures that the input table spec is valid. This node requires that a
     * document cell be present.
     *
     * @param spec the spec to validate
     * @throws InvalidSettingsException thrown if the spec is not valid
     */
    private final void checkDataTableSpec(final DataTableSpec spec)
            throws InvalidSettingsException {
        DataTableSpecVerifier verifier = new DataTableSpecVerifier(spec);
        verifier.verifyMinimumDocumentCells(1, true);

        ColumnSelectionVerifier docVerifier =
                new ColumnSelectionVerifier(m_documentColumnName, spec, DocumentValue.class);
        if (docVerifier.hasWarningMessage()) {
            setWarningMessage(docVerifier.getWarningMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to reset
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals to save
    }

    /**
     * Creates a DataTableSpec for the node, namely (Term, Double, Document).
     * @return the data table spec
     */
    private static DataTableSpec createDataTableSpec() {
        final TextContainerDataCellFactory termFac = TextContainerDataCellFactoryBuilder.createTermCellFactory();
        final TextContainerDataCellFactory docCellFac = TextContainerDataCellFactoryBuilder.createDocumentCellFactory();

        DataColumnSpecCreator keywords = new DataColumnSpecCreator("Keyword", termFac.getDataType());
        DataColumnSpecCreator chivalues = new DataColumnSpecCreator("Chi value", DoubleCell.TYPE);
        DataColumnSpecCreator docs = new DataColumnSpecCreator("Document", docCellFac.getDataType());
        return new DataTableSpec(keywords.createSpec(), chivalues.createSpec(), docs.createSpec());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals to load
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_frequentTermsProportion.saveSettingsTo(settings);
        m_ignoreTermTags.saveSettingsTo(settings);
        m_L1Threshold.saveSettingsTo(settings);
        m_nrKeywords.saveSettingsTo(settings);
        m_PMIThreshold.saveSettingsTo(settings);
        m_documentColumnName.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_frequentTermsProportion.loadSettingsFrom(settings);
        m_ignoreTermTags.loadSettingsFrom(settings);
        m_L1Threshold.loadSettingsFrom(settings);
        m_nrKeywords.loadSettingsFrom(settings);
        m_PMIThreshold.loadSettingsFrom(settings);
        m_documentColumnName.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_frequentTermsProportion.validateSettings(settings);
        m_ignoreTermTags.validateSettings(settings);
        m_L1Threshold.validateSettings(settings);
        m_nrKeywords.validateSettings(settings);
        m_PMIThreshold.validateSettings(settings);
        m_documentColumnName.validateSettings(settings);
    }

    /**
     * @param x the value
     * @return true if a double is equal to NaN, false otherwise.
     */
    private static boolean isNaN(final double x) {
        // All comparisons return false when NaN is involved, hence
        // x == Double.NaN would also return false even if x were Double.NaN.
        return x != x;
    }
}
