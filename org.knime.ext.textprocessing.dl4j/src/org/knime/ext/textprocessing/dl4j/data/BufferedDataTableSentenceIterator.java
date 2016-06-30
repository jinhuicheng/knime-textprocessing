/*******************************************************************************
 * Copyright by KNIME GmbH, Konstanz, Germany
 * Website: http://www.knime.org; Email: contact@knime.org
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, Version 3, as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 * Additional permission under GNU GPL version 3 section 7:
 *
 * KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 * Hence, KNIME and ECLIPSE are both independent programs and are not
 * derived from each other. Should, however, the interpretation of the
 * GNU GPL Version 3 ("License") under any applicable laws result in
 * KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 * you the additional permission to use and propagate KNIME together with
 * ECLIPSE with only the license terms in place for ECLIPSE applying to
 * ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 * license terms of ECLIPSE themselves allow for the respective use and
 * propagation of ECLIPSE together with KNIME.
 *
 * Additional permission relating to nodes for KNIME that extend the Node
 * Extension (and in particular that are based on subclasses of NodeModel,
 * NodeDialog, and NodeView) and that only interoperate with KNIME through
 * standard APIs ("Nodes"):
 * Nodes are deemed to be separate and independent programs and to not be
 * covered works.  Notwithstanding anything to the contrary in the
 * License, the License does not apply to Nodes, you are not required to
 * license Nodes under the License, and you are granted a license to
 * prepare and propagate Nodes, in each case even if such Nodes are
 * propagated with or for interoperation with KNIME.  The owner of a Node
 * may freely choose the license terms applicable to such Node, including
 * when such Node is propagated with or for interoperation with KNIME.
 *******************************************************************************/
package org.knime.ext.textprocessing.dl4j.data;

import java.util.Optional;

import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentencePreProcessor;
import org.knime.core.data.DataCell;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.convert.java.DataCellToJavaConverterFactory;
import org.knime.core.data.convert.java.DataCellToJavaConverterRegistry;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.NodeLogger;
import org.knime.ext.dl4j.base.util.ConverterUtils;
import org.knime.ext.textprocessing.data.DocumentValue;

/**
 * {@link SentenceIterator} for a {@link BufferedDataTable}. Expects a column contained in the 
 * data table holding one sentence per row.
 * 
 * @author David Kolb, KNIME.com GmbH
 */
public class BufferedDataTableSentenceIterator implements SentenceIterator {

	// the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger(BufferedDataTableSentenceIterator.class);
	
	private final BufferedDataTable m_table;
	private CloseableRowIterator m_tableIterator;
	private final int m_documentColumnIndex;
	
	/**
	 * Constructor for class BufferedDataTableSentenceIterator
	 * 
	 * @param table the table to iterate
	 * @param documentColumnName the name of the document column
	 */
	public BufferedDataTableSentenceIterator(final BufferedDataTable table, final String documentColumnName) {
		this.m_table = table;
		this.m_documentColumnIndex = table.getSpec().findColumnIndex(documentColumnName);
		this.m_tableIterator = table.iterator();
	}
	
	/**
	 * Returns the next String contained in the document column of the table.
	 */
	@Override
	public String nextSentence() {
		DataCell cell = m_tableIterator.next().getCell(m_documentColumnIndex);
		String documentContent = null;
	
	
		/* Can't use converter for documents because the getStringValue() method used for conversion to String returns
		 * the document title and no the content
		 */
//			Optional<DataCellToJavaConverterFactory<DataCell, String>> factory =
//					DataCellToJavaConverterRegistry.getInstance().getConverterFactory(cell.getType(), String.class);
//			return ConverterUtils.convertWithFactory(factory, cell);	
		if (cell.getType().isCompatible(DocumentValue.class)){
			DocumentValue dCell = (DocumentValue)cell;
			documentContent = dCell.getDocument().getDocumentBodyText();
		} else if(cell.getType().isCompatible(StringValue.class)){
			StringCell sCell = (StringCell)cell;
			documentContent = sCell.getStringValue();
		} else {
			logger.coding("Problem with input conversion");
		}
		return documentContent;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean hasNext() {
		return m_tableIterator.hasNext();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void reset() {
		m_tableIterator.close();	
		m_tableIterator = m_table.iterator();	
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void finish() {
		// nothing to do
	}

	/**
	 * Method not supported. Will throw exception if called.
	 */
	@Override
	public SentencePreProcessor getPreProcessor() {		
		throw new UnsupportedOperationException("Method getPreProcessor not available in " + this.getClass().getName());
	}

	/**
	 * Method not supported. Will throw exception if called.
	 */
	@Override
	public void setPreProcessor(SentencePreProcessor preProcessor) {
		throw new UnsupportedOperationException("Method setPreProcessor not available in " + this.getClass().getName());
	}
}
