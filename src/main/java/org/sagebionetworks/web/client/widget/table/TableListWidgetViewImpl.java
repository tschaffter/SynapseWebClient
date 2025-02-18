package org.sagebionetworks.web.client.widget.table;

import java.util.ArrayList;
import java.util.List;

import org.gwtbootstrap3.client.ui.Icon;
import org.gwtbootstrap3.client.ui.TextArea;
import org.gwtbootstrap3.client.ui.html.Div;
import org.gwtbootstrap3.client.ui.html.Span;
import org.sagebionetworks.repo.model.entity.Direction;
import org.sagebionetworks.repo.model.entity.SortBy;
import org.sagebionetworks.repo.model.table.SortDirection;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.PortalGinInjector;
import org.sagebionetworks.web.client.view.bootstrap.table.TableHeader;
import org.sagebionetworks.web.client.widget.LoadingSpinner;
import org.sagebionetworks.web.client.widget.table.modal.fileview.TableType;
import org.sagebionetworks.web.client.widget.table.v2.results.SortableTableHeaderImpl;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

/**
 * View of a widget that lists table entities.
 */
public class TableListWidgetViewImpl implements TableListWidgetView {

	public interface Binder extends UiBinder<HTMLPanel, TableListWidgetViewImpl> {
	}

	List<TableEntityListGroupItem> tablesList;

	@UiField
	Div tablesListDiv;
	@UiField
	Div loadMoreWidgetContainer;
	@UiField
	Div synAlertContainer;
	@UiField
	Div tableArea;
	@UiField
	Span emptyUI;
	@UiField
	SortableTableHeaderImpl nameColumnHeader;
	@UiField
	SortableTableHeaderImpl createdOnColumnHeader;
	@UiField
	SortableTableHeaderImpl modifiedOnColumnHeader;
	HTMLPanel panel;
	Presenter presenter;
	PortalGinInjector ginInjector;
	@UiField
	LoadingSpinner loadingUI;
	@UiField
	TextArea copyToClipboardTextbox;
	@UiField
	Icon copyIDToClipboardIcon;
	@UiField
	TableHeader itemCountColumnHeader;

	@Inject
	public TableListWidgetViewImpl(Binder binder, PortalGinInjector ginInjector) {
		this.tablesList = new ArrayList<>();
		this.panel = binder.createAndBindUi(this);
		this.ginInjector = ginInjector;
		nameColumnHeader.setSortingListener(header -> {
			presenter.toggleSort(SortBy.NAME);
		});
		createdOnColumnHeader.setSortingListener(header -> {
			presenter.toggleSort(SortBy.CREATED_ON);
		});
		modifiedOnColumnHeader.setSortingListener(header -> {
			presenter.toggleSort(SortBy.MODIFIED_ON);
		});

		copyIDToClipboardIcon.addClickHandler(event -> presenter.copyIDsToClipboard());
	}

	@Override
	public void clearSortUI() {
		nameColumnHeader.setSortDirection(null);
		createdOnColumnHeader.setSortDirection(null);
		modifiedOnColumnHeader.setSortDirection(null);
	}

	@Override
	public void setSortUI(SortBy sortBy, Direction dir) {
		clearSortUI();
		SortDirection direction = Direction.ASC.equals(dir) ? SortDirection.ASC : SortDirection.DESC;
		if (SortBy.NAME.equals(sortBy)) {
			nameColumnHeader.setSortDirection(direction);
		} else if (SortBy.CREATED_ON.equals(sortBy)) {
			createdOnColumnHeader.setSortDirection(direction);
		} else if (SortBy.MODIFIED_ON.equals(sortBy)) {
			modifiedOnColumnHeader.setSortDirection(direction);
		}
	}

	@Override
	public void addTableListItem(final TableEntityListGroupItem item) {
		tablesList.add(item);
		tablesListDiv.add(item);
	}

	@Override
	public void clearTableWidgets() {
		tablesList.clear();
		tablesListDiv.clear();
	}

	@Override
	public void setLoadMoreWidget(IsWidget w) {
		loadMoreWidgetContainer.clear();
		loadMoreWidgetContainer.add(w);
	}

	@Override
	public void setPresenter(final Presenter presenter) {
		this.presenter = presenter;
	}

	@Override
	public void setTableType(TableType tableType) {
		String emptyUiCopy = "&#8212; There are no " + tableType.getDisplayName() + "s associated with this project. Any " + tableType.getDisplayName() + "s you create in this project will appear here.";
		emptyUI.setHTML(emptyUiCopy);
	}

	@Override
	public void setState(TableListWidgetViewState state) {
		loadingUI.setVisible(TableListWidgetViewState.LOADING.equals(state));
		emptyUI.setVisible(TableListWidgetViewState.EMPTY.equals(state));
		tableArea.setVisible(TableListWidgetViewState.POPULATED.equals(state));
	}

	@Override
	public void showLoading() {
		setState(TableListWidgetViewState.LOADING);
	}

	@Override
	public void showInfo(String message) {
		DisplayUtils.showInfo(message);
	}

	@Override
	public void showErrorMessage(String message) {
		DisplayUtils.showErrorMessage(message);
	}

	@Override
	public Widget asWidget() {
		return panel;
	}

	@Override
	public void clear() {
		tablesList.clear();
		tablesListDiv.clear();
	}

	@Override
	public void setSynAlert(IsWidget w) {
		synAlertContainer.clear();
		synAlertContainer.add(w);
	}


	@Override
	public void copyToClipboard(String value) {
		copyToClipboardTextbox.setVisible(true);
		copyToClipboardTextbox.setFocus(true);
		copyToClipboardTextbox.setValue(value);
		copyToClipboardTextbox.selectAll();
		ginInjector.getSynapseJSNIUtils().copyToClipboard();
		copyToClipboardTextbox.setVisible(false);
	}

	@Override
	public void setFileCountVisible(boolean visible) {
		itemCountColumnHeader.setVisible(visible);
		if (visible) {
			itemCountColumnHeader.addStyleName("visible-md visible-lg");
		} else {
			itemCountColumnHeader.removeStyleName("visible-md visible-lg");
		}
		for (TableEntityListGroupItem item : tablesList) {
			item.setItemCountVisible(visible);
		}
	}
}
