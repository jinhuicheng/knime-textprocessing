/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by 
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, version 2, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 * -------------------------------------------------------------------
 *
 * History
 *   17.03.2009 (thiel): created
 */
package org.knime.ext.textprocessing.nodes.transformation.tagtoString;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.ext.textprocessing.data.PartOfSpeechTag;
import org.knime.ext.textprocessing.data.TagFactory;
import org.knime.ext.textprocessing.util.DataTableSpecVerifier;

/**
 *
 * @author Kilian Thiel
 */
public class TagToStringNodeModel extends NodeModel {

    /**
     * Set with all tag types.
     */
    public static final Set<String> ALL_TAG_TYPES =
        TagFactory.getInstance().getTagTypes();

    /**
     * The default tag type.
     */
    public static final String DEFAULT_TAG_TYPE =
        PartOfSpeechTag.getDefault().getType();

    /**
     * The missing cell vaue.
     */
    public static final String MISSING_CELL_VALUE = "<MissingCell>";

    /**
     * The default missing cell value.
     */
    public static final String DEFAULT_MISSING_VALUE = MISSING_CELL_VALUE;

    private int m_termColIndex = -1;

    private SettingsModelStringArray m_tagTypesModel =
        TagToStringNodeDialog.getTagTypesModel();

    private SettingsModelString m_termColModel =
        TagToStringNodeDialog.getTermColModel();

    private SettingsModelString m_missingTagValueModel =
        TagToStringNodeDialog.getMissingTagModel();

    /**
     * Creates new instance of <code>TagToStringNodeModel</code>.
     */
    public TagToStringNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpecVerifier verifier = new DataTableSpecVerifier(inSpecs[0]);
        verifier.verifyMinimumTermCells(1, true);

        m_termColIndex = inSpecs[0].findColumnIndex(
                m_termColModel.getStringValue());
        if (m_termColIndex < 0) {
            throw new InvalidSettingsException(
                    "Index of specified term column is not valid! "
                    + "Check your settings!");
        }

        List<String> tagTypes = new ArrayList<String>();
        for (String tagType : m_tagTypesModel.getStringArrayValue()) {
            tagTypes.add(tagType);
        }

        return new DataTableSpec[]{new DataTableSpec(inSpecs[0],
                                   new DataTableSpec(getDataTableSpec(
                                           tagTypes, inSpecs[0])))};
    }

    /**
     * Creates output <code>DataColumnSpec</code>s based on given tag types and
     * incoming <code>DataTableSpec</code>.
     * @param tagTypes tag types to consider.
     * @param oldSpec The incoming <code>DataTableSpec</code>
     * @return Output <code>DataColumnSpec</code>s.
     */
    static DataColumnSpec[] getDataTableSpec(final List<String> tagTypes,
            final DataTableSpec oldSpec) {
        DataColumnSpec[] dataColumnSpecs = new DataColumnSpec[tagTypes.size()];
        int i = 0;
        for (String tagType : tagTypes) {
            String name = DataTableSpec.getUniqueColumnName(oldSpec, tagType);
            dataColumnSpecs[i] = new DataColumnSpecCreator(
                    name, StringCell.TYPE).createSpec();
            i++;
        }
        return dataColumnSpecs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        m_termColIndex = inData[0].getDataTableSpec().findColumnIndex(
                m_termColModel.getStringValue());

        List<String> tagTypes = new ArrayList<String>();
        for (String tagType : m_tagTypesModel.getStringArrayValue()) {
            tagTypes.add(tagType);
        }
        CellFactory cellFac = new TagToStringCellFactory(m_termColIndex, tagTypes,
                    inData[0].getDataTableSpec(),
                    m_missingTagValueModel.getStringValue());
        ColumnRearranger rearranger = new ColumnRearranger(
                inData[0].getDataTableSpec());
        rearranger.append(cellFac);

        return new BufferedDataTable[]{exec.createColumnRearrangeTable(
                inData[0], rearranger, exec)};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to do ...
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_tagTypesModel.saveSettingsTo(settings);
        m_termColModel.saveSettingsTo(settings);
        m_missingTagValueModel.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_tagTypesModel.validateSettings(settings);
        m_termColModel.validateSettings(settings);
        m_missingTagValueModel.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_tagTypesModel.loadSettingsFrom(settings);
        m_termColModel.loadSettingsFrom(settings);
        m_missingTagValueModel.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do ...
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // nothing to do ...
    }
}
