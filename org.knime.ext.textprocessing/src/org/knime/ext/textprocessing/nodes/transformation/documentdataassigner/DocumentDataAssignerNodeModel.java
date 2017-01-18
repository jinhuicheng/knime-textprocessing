/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *   17.01.2017 (Julian): created
 */
package org.knime.ext.textprocessing.nodes.transformation.documentdataassigner;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.streamable.simple.SimpleStreamableFunctionNodeModel;
import org.knime.ext.textprocessing.data.DocumentValue;
import org.knime.ext.textprocessing.nodes.transformation.documenttostring.DocumentDataExtractor;
import org.knime.ext.textprocessing.util.DataTableSpecVerifier;
import org.knime.ext.textprocessing.util.TextContainerDataCellFactory;
import org.knime.ext.textprocessing.util.TextContainerDataCellFactoryBuilder;

/**
 *
 * @author Julian Bunzel, KNIME.com Berlin
 */
public class DocumentDataAssignerNodeModel extends SimpleStreamableFunctionNodeModel {

    private SettingsModelString m_docColumnModel = DocumentDataAssignerNodeDialog.getDocumentColumnModel();

    private SettingsModelString m_authorsColModel = DocumentDataAssignerNodeDialog.getAuthorsColumnModel();

    private SettingsModelString m_sourceColModel = DocumentDataAssignerNodeDialog.getSourceColumnModel();

    private SettingsModelString m_categoryColModel = DocumentDataAssignerNodeDialog.getCategoryColumnModel();

    private SettingsModelString m_pubDateColModel = DocumentDataAssignerNodeDialog.getPubDateColumnModel();

    private SettingsModelBoolean m_useAuthorsColModel = DocumentDataAssignerNodeDialog.getUseAuthorsColumnModel();

    private SettingsModelBoolean m_useSourceColModel = DocumentDataAssignerNodeDialog.getUseSourceColumnModel();

    private SettingsModelBoolean m_useCategoryColModel = DocumentDataAssignerNodeDialog.getUseCategoryColumnModel();

    private SettingsModelBoolean m_usePubDateColModel = DocumentDataAssignerNodeDialog.getUsePubDateColumnModel();

    private SettingsModelString m_authorsFirstNameModel = DocumentDataAssignerNodeDialog.getAuthorsFirstNameModel();

    private SettingsModelString m_authorsLastNameModel = DocumentDataAssignerNodeDialog.getAuthorsLastNameModel();

    private SettingsModelString m_authorsSplitStrModel = DocumentDataAssignerNodeDialog.getAuthorsSplitStringModel();

    private SettingsModelString m_sourceModel = DocumentDataAssignerNodeDialog.getSourceModel();

    private SettingsModelString m_categoryModel = DocumentDataAssignerNodeDialog.getCategoryModel();

    private SettingsModelString m_pubDateModel = DocumentDataAssignerNodeDialog.getPubDateModel();

    private SettingsModelString m_typeModel = DocumentDataAssignerNodeDialog.getTypeModel();

    private SettingsModelIntegerBounded m_threadsModel = DocumentDataAssignerNodeDialog.getNumberOfThreadsModel();

    private SettingsModelBoolean m_replaceDocColModel = DocumentDataAssignerNodeDialog.getReplaceDocColumnModel();

    /**
     *
     */
    public DocumentDataAssignerNodeModel() {
        modelStateChanged();
    }

    private final DataColumnSpec[] createNewColumnSpecs() {
        final TextContainerDataCellFactory docFactory = TextContainerDataCellFactoryBuilder.createDocumentCellFactory();
        String newDocColName = m_docColumnModel.getStringValue();
        if (!m_replaceDocColModel.getBooleanValue()) {
            newDocColName = "Processed " + newDocColName;
        }
        DataColumnSpecCreator docColSpecCreator = new DataColumnSpecCreator(newDocColName, docFactory.getDataType());
        return new DataColumnSpec[]{docColSpecCreator.createSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ColumnRearranger createColumnRearranger(final DataTableSpec spec) throws InvalidSettingsException {
        DataTableSpecVerifier verifier = new DataTableSpecVerifier(spec);
        verifier.verifyMinimumDocumentCells(1, true);

        DocumentDataAssignerConfig conf = new DocumentDataAssignerConfig();

        //document
        conf.setDocumentColumnIndex(spec.findColumnIndex(m_docColumnModel.getStringValue()));

        //authors
        if (m_useAuthorsColModel.getBooleanValue()) {
            conf.setAuthorsColumnIndex(spec.findColumnIndex(m_authorsColModel.getStringValue()));
            conf.setAuthorsSplitStr(m_authorsSplitStrModel.getStringValue());
        } else {
            conf.setAuthorsFirstName(m_authorsFirstNameModel.getStringValue());
            conf.setAuthorsLastName(m_authorsLastNameModel.getStringValue());
        }

        //source
        if (m_useSourceColModel.getBooleanValue()) {
            conf.setSourceColumnIndex(spec.findColumnIndex(m_sourceColModel.getStringValue()));
        } else {
            conf.setDocSource(m_sourceModel.getStringValue());
        }

        //category
        if (m_useCategoryColModel.getBooleanValue()) {
            conf.setCategoryColumnIndex(spec.findColumnIndex(m_categoryColModel.getStringValue()));
        } else {
            conf.setDocCategory(m_categoryModel.getStringValue());
        }

        //pubdate
        if (m_usePubDateColModel.getBooleanValue()) {
            conf.setPubDateColumnIndex(spec.findColumnIndex(m_pubDateColModel.getStringValue()));
        } else {
            conf.setDocPubDate(m_pubDateModel.getStringValue());
        }

        //type
        conf.setDocType(m_typeModel.getStringValue());

        //threads
        conf.setNumberOfThreads(m_threadsModel.getIntValue());

        DocumentDataAssignerCellFactory cellFac = new DocumentDataAssignerCellFactory(conf, createNewColumnSpecs());
        ColumnRearranger rearranger = new ColumnRearranger(spec);
        if (m_replaceDocColModel.getBooleanValue()) {
            rearranger.replace(cellFac, m_docColumnModel.getStringValue());
        } else {
            rearranger.append(cellFac);
        }
        return rearranger;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_docColumnModel.saveSettingsTo(settings);
        m_authorsColModel.saveSettingsTo(settings);
        m_authorsFirstNameModel.saveSettingsTo(settings);
        m_authorsLastNameModel.saveSettingsTo(settings);
        m_authorsSplitStrModel.saveSettingsTo(settings);
        m_categoryColModel.saveSettingsTo(settings);
        m_categoryModel.saveSettingsTo(settings);
        m_pubDateColModel.saveSettingsTo(settings);
        m_pubDateModel.saveSettingsTo(settings);
        m_useAuthorsColModel.saveSettingsTo(settings);
        m_useCategoryColModel.saveSettingsTo(settings);
        m_usePubDateColModel.saveSettingsTo(settings);
        m_useSourceColModel.saveSettingsTo(settings);
        m_threadsModel.saveSettingsTo(settings);
        m_typeModel.saveSettingsTo(settings);
        m_sourceColModel.saveSettingsTo(settings);
        m_sourceModel.saveSettingsTo(settings);
        m_replaceDocColModel.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_docColumnModel.validateSettings(settings);
        m_authorsColModel.validateSettings(settings);
        m_authorsFirstNameModel.validateSettings(settings);
        m_authorsLastNameModel.validateSettings(settings);
        m_authorsSplitStrModel.validateSettings(settings);
        m_categoryColModel.validateSettings(settings);
        m_categoryModel.validateSettings(settings);
        m_pubDateColModel.validateSettings(settings);
        m_pubDateModel.validateSettings(settings);
        m_useAuthorsColModel.validateSettings(settings);
        m_useCategoryColModel.validateSettings(settings);
        m_usePubDateColModel.validateSettings(settings);
        m_useSourceColModel.validateSettings(settings);
        m_threadsModel.validateSettings(settings);
        m_typeModel.validateSettings(settings);
        m_sourceColModel.validateSettings(settings);
        m_sourceModel.validateSettings(settings);
        m_replaceDocColModel.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_docColumnModel.loadSettingsFrom(settings);
        m_authorsColModel.loadSettingsFrom(settings);
        m_authorsFirstNameModel.loadSettingsFrom(settings);
        m_authorsLastNameModel.loadSettingsFrom(settings);
        m_authorsSplitStrModel.loadSettingsFrom(settings);
        m_categoryColModel.loadSettingsFrom(settings);
        m_categoryModel.loadSettingsFrom(settings);
        m_pubDateColModel.loadSettingsFrom(settings);
        m_pubDateModel.loadSettingsFrom(settings);
        m_useAuthorsColModel.loadSettingsFrom(settings);
        m_useCategoryColModel.loadSettingsFrom(settings);
        m_usePubDateColModel.loadSettingsFrom(settings);
        m_useSourceColModel.loadSettingsFrom(settings);
        m_threadsModel.loadSettingsFrom(settings);
        m_typeModel.loadSettingsFrom(settings);
        m_sourceColModel.loadSettingsFrom(settings);
        m_sourceModel.loadSettingsFrom(settings);
        m_replaceDocColModel.loadSettingsFrom(settings);
    }

    private void modelStateChanged() {
        m_categoryModel.setEnabled(!m_useCategoryColModel.getBooleanValue());
        m_sourceModel.setEnabled(!m_useSourceColModel.getBooleanValue());
        m_sourceColModel.setEnabled(m_useSourceColModel.getBooleanValue());
        m_categoryColModel.setEnabled(m_useCategoryColModel.getBooleanValue());
        m_authorsColModel.setEnabled(m_useAuthorsColModel.getBooleanValue());
        m_authorsSplitStrModel.setEnabled(m_useAuthorsColModel.getBooleanValue());
        m_authorsFirstNameModel.setEnabled(!m_useAuthorsColModel.getBooleanValue());
        m_authorsLastNameModel.setEnabled(!m_useAuthorsColModel.getBooleanValue());
        m_pubDateColModel.setEnabled(m_usePubDateColModel.getBooleanValue());
        m_pubDateModel.setEnabled(!m_usePubDateColModel.getBooleanValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        doSmartDialogSelection(inSpecs[0]);
        return super.configure(inSpecs);
    }

    /**
     * Automatically detects column names that fit to some options.
     *
     * @param dataTableSpec The DataTableSpec.
     */
    protected void doSmartDialogSelection(final DataTableSpec dataTableSpec) {
        String[] columns = dataTableSpec.getColumnNames();
        if (settingsNotConfigured()) {
            for (int i = 0; i < columns.length; i++) {
                String column = columns[i];
                if (dataTableSpec.getColumnSpec(column).getType().isCompatible(DocumentValue.class)
                    && m_docColumnModel.getStringValue().isEmpty()) {
                    m_docColumnModel.setStringValue(column);
                }
                if (column.equalsIgnoreCase(DocumentDataExtractor.SOURCE.getName())
                    && dataTableSpec.getColumnSpec(column).getType().isCompatible(StringValue.class)) {
                    m_sourceColModel.setStringValue(column);
                }
                if (column.equalsIgnoreCase(DocumentDataExtractor.AUTHOR.getName())
                    && dataTableSpec.getColumnSpec(column).getType().isCompatible(StringValue.class)) {
                    m_authorsColModel.setStringValue(column);
                }
                if (column.equalsIgnoreCase(DocumentDataExtractor.CATEGORY.getName())
                    && dataTableSpec.getColumnSpec(column).getType().isCompatible(StringValue.class)) {
                    m_categoryColModel.setStringValue(column);
                }
                if (column.equalsIgnoreCase(DocumentDataExtractor.PUB_DATE.getName())
                    && dataTableSpec.getColumnSpec(column).getType().isCompatible(StringValue.class)) {
                    m_pubDateColModel.setStringValue(column);
                }
            }
        }
    }

    /**
     * @return true if settings have not been configured before
     */
    protected boolean settingsNotConfigured() {
        return (m_docColumnModel.getStringValue().isEmpty() && m_authorsColModel.getStringValue().isEmpty()
            && m_sourceColModel.getStringValue().isEmpty() && m_categoryColModel.getStringValue().isEmpty()
            && m_pubDateColModel.getStringValue().isEmpty());
    }
}
