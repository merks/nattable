/*******************************************************************************
 * Copyright (c) 2017 Dirk Fauth.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Dirk Fauth <dirk.fauth@googlemail.com> - initial API and implementation
 ******************************************************************************/
package org.eclipse.nebula.widgets.nattable.resize.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.grid.GridRegion;
import org.eclipse.nebula.widgets.nattable.grid.data.DummyBodyDataProvider;
import org.eclipse.nebula.widgets.nattable.layer.DataLayer;
import org.eclipse.nebula.widgets.nattable.layer.event.RowStructuralRefreshEvent;
import org.eclipse.nebula.widgets.nattable.layer.stack.DummyGridLayerStack;
import org.eclipse.nebula.widgets.nattable.test.fixture.NatTableFixture;
import org.eclipse.nebula.widgets.nattable.test.fixture.layer.LayerListenerFixture;
import org.junit.Before;
import org.junit.Test;

public class RowSizeResetCommandTest {

    private DataLayer dataLayer;
    private LayerListenerFixture listener;

    @Before
    public void setup() {
        this.dataLayer = new DataLayer(new DummyBodyDataProvider(10, 10));
        this.listener = new LayerListenerFixture();

        this.dataLayer.addLayerListener(this.listener);
    }

    @Test
    public void testHandleRowSizeResetCommand() {
        this.dataLayer.doCommand(new RowResizeCommand(this.dataLayer, 3, 50));

        assertEquals(50, this.dataLayer.getRowHeightByPosition(3));
        assertEquals(1, this.listener.getEventsCount());

        this.dataLayer.doCommand(new RowSizeResetCommand());

        assertEquals(20, this.dataLayer.getRowHeightByPosition(3));
        assertEquals(2, this.listener.getEventsCount());
        assertTrue(this.listener.containsInstanceOf(RowStructuralRefreshEvent.class));
    }

    @Test
    public void testHandleRowSizeResetCommandWithoutEvent() {
        this.dataLayer.doCommand(new RowResizeCommand(this.dataLayer, 3, 50));

        assertEquals(50, this.dataLayer.getRowHeightByPosition(3));
        assertEquals(1, this.listener.getEventsCount());

        this.dataLayer.doCommand(new RowSizeResetCommand(false));

        assertEquals(20, this.dataLayer.getRowHeightByPosition(3));
        assertEquals(1, this.listener.getEventsCount());
        assertFalse(this.listener.containsInstanceOf(RowStructuralRefreshEvent.class));
    }

    @Test
    public void testResetAllRegions() {
        DummyGridLayerStack dummyGridLayerStack = new DummyGridLayerStack();
        NatTable natTable = new NatTableFixture(dummyGridLayerStack);

        assertEquals(20, dummyGridLayerStack.getColumnHeaderDataLayer().getRowHeightByPosition(0));
        assertEquals(20, dummyGridLayerStack.getBodyDataLayer().getRowHeightByPosition(2));

        ((DataLayer) dummyGridLayerStack.getColumnHeaderDataLayer()).setRowHeightByPosition(0, 50);
        ((DataLayer) dummyGridLayerStack.getBodyDataLayer()).setRowHeightByPosition(2, 50);

        assertEquals(50, dummyGridLayerStack.getColumnHeaderDataLayer().getRowHeightByPosition(0));
        assertEquals(50, dummyGridLayerStack.getBodyDataLayer().getRowHeightByPosition(2));

        natTable.doCommand(new RowSizeResetCommand());

        assertEquals(20, dummyGridLayerStack.getColumnHeaderDataLayer().getRowHeightByPosition(0));
        assertEquals(20, dummyGridLayerStack.getBodyDataLayer().getRowHeightByPosition(2));
    }

    @Test
    public void testResetOnlyBody() {
        DummyGridLayerStack dummyGridLayerStack = new DummyGridLayerStack();
        NatTable natTable = new NatTableFixture(dummyGridLayerStack);

        assertEquals(20, dummyGridLayerStack.getColumnHeaderDataLayer().getRowHeightByPosition(0));
        assertEquals(20, dummyGridLayerStack.getBodyDataLayer().getRowHeightByPosition(2));

        ((DataLayer) dummyGridLayerStack.getColumnHeaderDataLayer()).setRowHeightByPosition(0, 50);
        ((DataLayer) dummyGridLayerStack.getBodyDataLayer()).setRowHeightByPosition(2, 50);

        assertEquals(50, dummyGridLayerStack.getColumnHeaderDataLayer().getRowHeightByPosition(0));
        assertEquals(50, dummyGridLayerStack.getBodyDataLayer().getRowHeightByPosition(2));

        natTable.doCommand(new RowSizeResetCommand(GridRegion.BODY));

        assertEquals(50, dummyGridLayerStack.getColumnHeaderDataLayer().getRowHeightByPosition(0));
        assertEquals(20, dummyGridLayerStack.getBodyDataLayer().getRowHeightByPosition(2));
    }
}
