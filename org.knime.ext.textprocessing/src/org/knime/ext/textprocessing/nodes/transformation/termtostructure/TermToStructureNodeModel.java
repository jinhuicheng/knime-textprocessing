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
 *   26.06.2008 (thiel): created
 */
package org.knime.ext.textprocessing.nodes.transformation.termtostructure;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
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
import org.knime.ext.textprocessing.data.TermValue;
import org.knime.ext.textprocessing.util.ColumnSelectionVerifier;
import org.knime.ext.textprocessing.util.DataTableSpecVerifier;

import uk.ac.cam.ch.wwmm.oscar.chemnamedict.entities.FormatType;

/**
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public class TermToStructureNodeModel extends NodeModel {

    /**
     * The default format type to convert strings to.
     */
    public static final String DEF_FORMAT_TYPE = "SMILES";
    
    private static final int INDATA_INDEX = 0;
    
    private int m_termColIndex = -1;
    
    private String m_newColName;
    
    private SettingsModelString m_termColModel = 
        TermToStructureNodeDialog.getTermColModel();
    
    private SettingsModelString m_formatTypeModel = 
        TermToStructureNodeDialog.getFormatTypeModel();    
    
    /**
     * Creates a new instance of <code>TermToStructureNodeModel</code>.
     */
    public TermToStructureNodeModel() {
        super(1, 1);
    }
    
    private final void checkDataTableSpec(final DataTableSpec spec)
    throws InvalidSettingsException {
        DataTableSpecVerifier verifier = new DataTableSpecVerifier(spec);
        verifier.verifyMinimumTermCells(1, true);

        ColumnSelectionVerifier termVerifier =
                new ColumnSelectionVerifier(m_termColModel, spec, TermValue.class);
        if (termVerifier.hasWarningMessage()) {
            setWarningMessage(termVerifier.getWarningMessage());
        }

        m_termColIndex = spec.findColumnIndex(
            m_termColModel.getStringValue());
    }
    
    private DataTableSpec createDataTableSpec(
            final DataTableSpec inDataSpec) {
        
        String termCol = m_termColModel.getStringValue();
        if (termCol.isEmpty()) {
            termCol = "Term as Structure";
        } else {
            termCol += " as Structure";
        }
        
        int count = 1;
        m_newColName = termCol;
        while (inDataSpec.containsName(m_newColName)) {
            m_newColName = termCol + " " + count;
            count++;
        }
        
        DataColumnSpec strCol = new DataColumnSpecCreator(m_newColName, 
                StringCell.TYPE).createSpec();
        return new DataTableSpec(inDataSpec, new DataTableSpec(strCol));
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        checkDataTableSpec(inSpecs[INDATA_INDEX]);
        return new DataTableSpec[]{createDataTableSpec(inSpecs[INDATA_INDEX])};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
                
        BufferedDataTable inDataTable = inData[INDATA_INDEX];
        checkDataTableSpec(inDataTable.getDataTableSpec());

        // find index of term column
        m_termColIndex = inDataTable.getDataTableSpec().findColumnIndex(
                m_termColModel.getStringValue());
        
        // initializes the corresponding cell factory
        TermToStructureCellFactory cellFac = new TermToStructureCellFactory(
                m_termColIndex, m_newColName, 
                FormatType.valueOf(m_formatTypeModel.getStringValue()));
        
        // compute frequency and add column
        ColumnRearranger rearranger = new ColumnRearranger(
                inDataTable.getDataTableSpec());
        rearranger.append(cellFac);
        
        return new BufferedDataTable[] {
                exec.createColumnRearrangeTable(inDataTable, rearranger, 
                exec)};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_termColModel.loadSettingsFrom(settings);
        m_formatTypeModel.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // Nothing to do ...
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_termColModel.saveSettingsTo(settings);
        m_formatTypeModel.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_termColModel.validateSettings(settings);
        m_formatTypeModel.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, 
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // Nothing to do ...
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, 
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // Nothing to do ...
    }
}
