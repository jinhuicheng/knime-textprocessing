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
 *   08.08.2008 (thiel): created
 */
package org.knime.ext.textprocessing.nodes.view.documentviewer2;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.ext.textprocessing.data.DocumentValue;
import org.knime.ext.textprocessing.nodes.view.documentviewer.DocumentViewerConfigKeys;
import org.knime.ext.textprocessing.nodes.view.documentviewer.DocumentViewerNodeDialog;

/**
 *
 * @author Hermann Azong, KNIME.com, Berlin, Germany
 */
public class DocumentViewer2NodeDialog extends DefaultNodeSettingsPane {

    /**
     * @return Creates and returns the string settings model containing the name of the column with the documents to
     *         preprocess.
     */
    public static SettingsModelString getDocumentColumnModel() {
        return new SettingsModelString(DocumentViewerConfigKeys.CFG_KEY_DOCUMENT_COL, "");
    }

    /**
     * Creates new instance of {@link DocumentViewerNodeDialog}.
     */
    @SuppressWarnings("unchecked")
    public DocumentViewer2NodeDialog() {
        removeTab("Options");
        createNewTabAt("Preprocessing", 1);

        DialogComponentColumnNameSelection comp1 =
            new DialogComponentColumnNameSelection(getDocumentColumnModel(), "Document column", 0, DocumentValue.class);
        comp1.setToolTipText("Column has to contain documents to preprocess!");
        addDialogComponent(comp1);
    }
}
