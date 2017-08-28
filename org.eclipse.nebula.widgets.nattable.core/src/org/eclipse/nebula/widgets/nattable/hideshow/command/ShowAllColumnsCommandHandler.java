/*******************************************************************************
 * Copyright (c) 2012, 2017 Original authors and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Original authors and others - initial API and implementation
 ******************************************************************************/
package org.eclipse.nebula.widgets.nattable.hideshow.command;

import org.eclipse.nebula.widgets.nattable.command.AbstractLayerCommandHandler;
import org.eclipse.nebula.widgets.nattable.hideshow.ColumnHideShowLayer;
import org.eclipse.nebula.widgets.nattable.hideshow.IColumnHideShowLayer;

public class ShowAllColumnsCommandHandler extends AbstractLayerCommandHandler<ShowAllColumnsCommand> {

    private final IColumnHideShowLayer columnHideShowLayer;

    /**
     *
     * @param columnHideShowLayer
     *            The {@link IColumnHideShowLayer} on which this command handler
     *            should operate.
     */
    public ShowAllColumnsCommandHandler(ColumnHideShowLayer columnHideShowLayer) {
        this.columnHideShowLayer = columnHideShowLayer;
    }

    /**
     *
     * @param columnHideShowLayer
     *            The {@link IColumnHideShowLayer} on which this command handler
     *            should operate.
     * @since 1.6
     */
    public ShowAllColumnsCommandHandler(IColumnHideShowLayer columnHideShowLayer) {
        this.columnHideShowLayer = columnHideShowLayer;
    }

    @Override
    public Class<ShowAllColumnsCommand> getCommandClass() {
        return ShowAllColumnsCommand.class;
    }

    @Override
    protected boolean doCommand(ShowAllColumnsCommand command) {
        this.columnHideShowLayer.showAllColumns();
        return true;
    }

}
