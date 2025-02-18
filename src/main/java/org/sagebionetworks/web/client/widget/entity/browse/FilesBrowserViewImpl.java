package org.sagebionetworks.web.client.widget.entity.browse;

import org.gwtbootstrap3.client.ui.AnchorListItem;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Heading;
import org.gwtbootstrap3.client.ui.Tooltip;
import org.gwtbootstrap3.client.ui.html.Div;
import org.gwtbootstrap3.client.ui.html.Span;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.utils.CallbackP;
import org.sagebionetworks.web.client.widget.HelpWidget;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class FilesBrowserViewImpl implements FilesBrowserView {

	public interface FilesBrowserViewImplUiBinder extends UiBinder<Widget, FilesBrowserViewImpl> {
	}

	private EntityTreeBrowser entityTreeBrowser;
	private Widget widget;
	private Presenter presenter;

	@UiField
	Div files;
	@UiField
	Div commandsContainer;
	@UiField
	AnchorListItem addToDownloadListLink;
	@UiField
	AnchorListItem programmaticOptionsLink;
	@UiField
	Div addToDownloadListContainer;
	@UiField
	Button downloadOptionsButton;
	@UiField
	Div actionMenuContainer;
	@UiField
	Heading title;
	@UiField
	Tooltip downloadTooltip;

	@Inject
	public FilesBrowserViewImpl(FilesBrowserViewImplUiBinder binder, EntityTreeBrowser entityTreeBrowser, AuthenticationController authController) {
		widget = binder.createAndBindUi(this);
		this.entityTreeBrowser = entityTreeBrowser;
		Widget etbW = entityTreeBrowser.asWidget();
		etbW.addStyleName("margin-top-10");
		files.add(etbW);
		programmaticOptionsLink.addClickHandler(event -> {
			presenter.onProgrammaticDownloadOptions();
		});
		addToDownloadListLink.addClickHandler(event -> {
			presenter.onAddToDownloadList();
		});
		entityTreeBrowser.setIsEmptyCallback(isEmpty -> {
			if (isEmpty) {
				downloadTooltip.setTitle("There are no downloadable items in this folder.");
				downloadOptionsButton.setEnabled(false);
			} else if (!authController.isLoggedIn()) {
				downloadTooltip.setTitle("You must be logged in to download items in this folder.");
				downloadOptionsButton.setEnabled(false);
			} else {
				downloadTooltip.setTitle("Direct and programmatic download options");
				downloadOptionsButton.setEnabled(true);
			}
		});
	}

	@Override
	public void configure(String entityId) {
		title.setVisible(false);
		entityTreeBrowser.configure(entityId);
	}

	@Override
	public void setEntityClickedHandler(CallbackP<String> callback) {
		entityTreeBrowser.setEntityClickedHandler(entityId -> {
			entityTreeBrowser.setLoadingVisible(true);
			callback.invoke(entityId);
		});
	}

	@Override
	public Widget asWidget() {
		return widget;
	}

	@Override
	public void showErrorMessage(String message) {
		DisplayUtils.showErrorMessage(message);
	}

	@Override
	public void showLoading() {

	}

	@Override
	public void showInfo(String message) {
		DisplayUtils.showInfo(message);
	}

	@Override
	public void clear() {
		entityTreeBrowser.clear();
	}

	@Override
	public void setPresenter(Presenter p) {
		this.presenter = p;
	}

	@Override
	public void setAddToDownloadList(IsWidget w) {
		addToDownloadListContainer.clear();
		addToDownloadListContainer.add(w);
	}
	
	@Override
	public void setActionMenu(IsWidget w) {
		w.asWidget().removeFromParent();
		actionMenuContainer.clear();
		actionMenuContainer.add(w);
		// if showing action menu, then show title.
		title.setVisible(true);
	}
}
