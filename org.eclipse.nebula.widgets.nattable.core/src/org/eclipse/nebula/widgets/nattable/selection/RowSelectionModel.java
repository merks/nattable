/*******************************************************************************
 * Copyright (c) 2012 Original authors and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Original authors and others - initial API and implementation
 ******************************************************************************/
package org.eclipse.nebula.widgets.nattable.selection;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.nebula.widgets.nattable.coordinate.Range;
import org.eclipse.nebula.widgets.nattable.data.IRowDataProvider;
import org.eclipse.nebula.widgets.nattable.data.IRowIdAccessor;
import org.eclipse.nebula.widgets.nattable.layer.cell.ILayerCell;
import org.eclipse.swt.graphics.Rectangle;

public class RowSelectionModel<R> implements IRowSelectionModel<R> {

	protected final SelectionLayer selectionLayer;
	protected final IRowDataProvider<R> rowDataProvider;
	protected final IRowIdAccessor<R> rowIdAccessor;
	private boolean multipleSelectionAllowed;
	
	protected Map<Serializable, R> selectedRows;
	protected Rectangle lastSelectedRange;  // *live* reference to last range parameter used in addSelection(range)
	protected Set<Serializable> lastSelectedRowIds;
	protected final ReadWriteLock selectionsLock;

	public RowSelectionModel(SelectionLayer selectionLayer, IRowDataProvider<R> rowDataProvider, IRowIdAccessor<R> rowIdAccessor) {
		this(selectionLayer, rowDataProvider, rowIdAccessor, true);
	}
	
	public RowSelectionModel(SelectionLayer selectionLayer, IRowDataProvider<R> rowDataProvider, IRowIdAccessor<R> rowIdAccessor, boolean multipleSelectionAllowed) {
		this.selectionLayer = selectionLayer;
		this.rowDataProvider = rowDataProvider;
		this.rowIdAccessor = rowIdAccessor;
		this.multipleSelectionAllowed = multipleSelectionAllowed;
		
		selectedRows = new HashMap<Serializable, R>();
		selectionsLock = new ReentrantReadWriteLock();
	}
	
	public boolean isMultipleSelectionAllowed() {
		return multipleSelectionAllowed;
	}
	
	public void setMultipleSelectionAllowed(boolean multipleSelectionAllowed) {
		this.multipleSelectionAllowed = multipleSelectionAllowed;
	}

	public void addSelection(int columnPosition, int rowPosition) {
		selectionsLock.writeLock().lock();
		
		try {
			if (!multipleSelectionAllowed) {
				selectedRows.clear();
			}
			
			R rowObject = getRowObjectByPosition(rowPosition);
			if (rowObject != null) {
				Serializable rowId = rowIdAccessor.getRowId(rowObject);
				selectedRows.put(rowId, rowObject);
			}
		} finally {
			selectionsLock.writeLock().unlock();
		}
	}

	public void addSelection(Rectangle range) {
		selectionsLock.writeLock().lock();
		
		try {
			if (multipleSelectionAllowed) {
				if (range.equals(lastSelectedRange)) {
					// Unselect all previously selected rowIds
					if (lastSelectedRowIds != null) {
						for (Serializable rowId : lastSelectedRowIds) {
							selectedRows.remove(rowId);
						}
					}
				}
			} else {
				selectedRows.clear();
			}
			
			Map<Serializable, R> rowsToSelect = new HashMap<Serializable, R>();
			
			for (int rowPosition = range.y; rowPosition < range.y + range.height; rowPosition++) {
				R rowObject = getRowObjectByPosition(rowPosition);
				if (rowObject != null) {
					Serializable rowId = rowIdAccessor.getRowId(rowObject);
					rowsToSelect.put(rowId, rowObject);
				}
			}
			
			selectedRows.putAll(rowsToSelect);
			
			if (range.equals(lastSelectedRange)) {
				lastSelectedRowIds = rowsToSelect.keySet();
			} else {
				lastSelectedRowIds = null;
			}
			
			lastSelectedRange = range;
		} finally {
			selectionsLock.writeLock().unlock();
		}
	}
	
	public void clearSelection() {
		selectionsLock.writeLock().lock();
		try {
			selectedRows.clear();
		} finally {
			selectionsLock.writeLock().unlock();
		}
	}

	public void clearSelection(int columnPosition, int rowPosition) {
		selectionsLock.writeLock().lock();
		
		try {
			Serializable rowId = getRowIdByPosition(rowPosition);
			selectedRows.remove(rowId);
		} finally {
			selectionsLock.writeLock().unlock();
		}
	}

	public void clearSelection(Rectangle removedSelection) {
		selectionsLock.writeLock().lock();
		
		try {
			for (int rowPosition = removedSelection.y; rowPosition < removedSelection.y + removedSelection.height; rowPosition++) {
				clearSelection(0, rowPosition);
			}
		} finally {
			selectionsLock.writeLock().unlock();
		}
	}

	public void clearSelection(R rowObject) {
		selectionsLock.writeLock().lock();
		
		try {
			selectedRows.values().remove(rowObject);
		} finally {
			selectionsLock.writeLock().unlock();
		}
	};
	
	public boolean isEmpty() {
		selectionsLock.readLock().lock();
		
		try {
			return selectedRows.isEmpty();
		} finally {
			selectionsLock.readLock().unlock();
		}
	}

	public List<Rectangle> getSelections() {
		List<Rectangle> selectionRectangles = new ArrayList<Rectangle>();
		
		selectionsLock.readLock().lock();
		
		try {
			int width = selectionLayer.getColumnCount();
			for (Serializable rowId : selectedRows.keySet()) {
				int rowPosition = getRowPositionById(rowId);
				selectionRectangles.add(new Rectangle(0, rowPosition, width, 1));
			}
		} finally {
			selectionsLock.readLock().unlock();
		}
		
		return selectionRectangles;
	}
	
	// Cell features

	public boolean isCellPositionSelected(int columnPosition, int rowPosition) {
		ILayerCell cell = selectionLayer.getCellByPosition(columnPosition, rowPosition);
		int cellOriginRowPosition = cell.getOriginRowPosition();
		for (int testRowPosition = cellOriginRowPosition; testRowPosition < cellOriginRowPosition + cell.getRowSpan(); testRowPosition++) {
			if (isRowPositionSelected(testRowPosition)) {
				return true;
			}
		}
		return false;
	}
	
	// Column features

	public int[] getSelectedColumnPositions() {
		if (!isEmpty()) {
			selectionsLock.readLock().lock();
			
			int columnCount;
			
			try {
				columnCount = selectionLayer.getColumnCount();
			} finally {
				selectionsLock.readLock().unlock();
			}
			
			int[] columns = new int[columnCount];
			for (int i = 0; i < columnCount; i++) {
				columns[i] = i;
			}
			return columns;
		}
		return new int[] {};
	}
	
	public boolean isColumnPositionSelected(int columnPosition) {
		selectionsLock.readLock().lock();

		try {
			return !selectedRows.isEmpty();
		} finally {
			selectionsLock.readLock().unlock();
		}
	}

	public int[] getFullySelectedColumnPositions(int fullySelectedColumnRowCount) {
		selectionsLock.readLock().lock();
		
		try {
			if (isColumnPositionFullySelected(0, fullySelectedColumnRowCount)) {
				return getSelectedColumnPositions();
			}
		} finally {
			selectionsLock.readLock().unlock();
		}
		
		return new int[] {};
	}

	public boolean isColumnPositionFullySelected(int columnPosition, int fullySelectedColumnRowCount) {
		selectionsLock.readLock().lock();
		
		try {
			int selectedRowCount = selectedRows.size();
			
			if (selectedRowCount == 0) {
				return false;
			}
			
			return selectedRowCount == fullySelectedColumnRowCount;
		} finally {
			selectionsLock.readLock().unlock();
		}
	}
	
	// Row features
	
	public List<R> getSelectedRowObjects() {
		final List<R> rowObjects = new ArrayList<R>();

		this.selectionsLock.readLock().lock();
		try {
			rowObjects.addAll(this.selectedRows.values());
		} finally {
			this.selectionsLock.readLock().unlock();
		}

		return rowObjects;
	}

	public int getSelectedRowCount() {
		selectionsLock.readLock().lock();
		
		try {
			return selectedRows.size();
		} finally {
			selectionsLock.readLock().unlock();
		}
	}

	public Set<Range> getSelectedRowPositions() {
		Set<Range> selectedRowRanges = new HashSet<Range>();
		
		selectionsLock.readLock().lock();
		
		try {
			for (Serializable rowId : selectedRows.keySet()) {
				int rowPosition = getRowPositionById(rowId);
				selectedRowRanges.add(new Range(rowPosition, rowPosition + 1));
			}
		} finally {
			selectionsLock.readLock().unlock();
		}
		
		return selectedRowRanges;
	}

	public boolean isRowPositionSelected(int rowPosition) {
		selectionsLock.readLock().lock();
		
		try {
			Serializable rowId = getRowIdByPosition(rowPosition);
			return selectedRows.containsKey(rowId);
		} finally {
			selectionsLock.readLock().unlock();
		}
	}

	public int[] getFullySelectedRowPositions(int rowWidth) {
		selectionsLock.readLock().lock();
		
		try {
			int selectedRowCount = selectedRows.size();
			int[] selectedRowPositions = new int[selectedRowCount];
			int i = 0;
			for (Serializable rowId : selectedRows.keySet()) {
				selectedRowPositions[i] = getRowPositionById(rowId);
				i++;
			}
			return selectedRowPositions;
		} finally {
			selectionsLock.readLock().unlock();
		}
	}

	public boolean isRowPositionFullySelected(int rowPosition, int rowWidth) {
		return isRowPositionSelected(rowPosition);
	}

	private Serializable getRowIdByPosition(int rowPosition) {
		R rowObject = getRowObjectByPosition(rowPosition);
		if (rowObject != null) {
			Serializable rowId = rowIdAccessor.getRowId(rowObject);
			return rowId;
		}
		return null;
	}

	private R getRowObjectByPosition(int rowPosition) {
		selectionsLock.readLock().lock();
		
		try {
			int rowIndex = selectionLayer.getRowIndexByPosition(rowPosition);
			if (rowIndex >= 0) {
				try {
					R rowObject = rowDataProvider.getRowObject(rowIndex);
					return rowObject;
				} catch (Exception e) {
					// row index is invalid for the data provider
				}
			}
		} finally {
			selectionsLock.readLock().unlock();
		}
		
		return null;
	}
	
	private int getRowPositionById(Serializable rowId) {
		selectionsLock.readLock().lock();
		
		try {
			R rowObject = selectedRows.get(rowId);
			int rowIndex = rowDataProvider.indexOfRowObject(rowObject);
			if(rowIndex == -1){
				return -1;
			}
			int rowPosition = selectionLayer.getRowPositionByIndex(rowIndex);
			return rowPosition;
		} finally {
			selectionsLock.readLock().unlock();
		}
	}
	
}
