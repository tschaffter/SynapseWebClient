package org.sagebionetworks.web.client.widget.entity;

import static org.sagebionetworks.repo.model.EntityTypeUtils.getEntityTypeForClassName;
import static org.sagebionetworks.web.client.DisplayUtils.TEXTBOX_SELECT_ALL_FIELD_CLICKHANDLER;

import org.gwtbootstrap3.client.ui.Anchor;
import org.gwtbootstrap3.client.ui.Icon;
import org.gwtbootstrap3.client.ui.Tooltip;
import org.gwtbootstrap3.client.ui.constants.Emphasis;
import org.gwtbootstrap3.client.ui.constants.IconType;
import org.gwtbootstrap3.client.ui.constants.Placement;
import org.gwtbootstrap3.client.ui.constants.Pull;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.web.client.DateTimeUtils;
import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.client.PortalGinInjector;
import org.sagebionetworks.web.client.SynapseJSNIUtils;
import org.sagebionetworks.web.client.jsinterop.EntityBadgeIconsProps;
import org.sagebionetworks.web.client.jsinterop.React;
import org.sagebionetworks.web.client.jsinterop.ReactDOM;
import org.sagebionetworks.web.client.jsinterop.ReactElement;
import org.sagebionetworks.web.client.jsinterop.SRC;
import org.sagebionetworks.web.client.jsinterop.SynapseContextProviderProps;
import org.sagebionetworks.web.client.place.Synapse;
import org.sagebionetworks.web.client.utils.Callback;
import org.sagebionetworks.web.client.view.bootstrap.table.TableData;
import org.sagebionetworks.web.client.widget.EntityTypeIcon;
import org.sagebionetworks.web.client.widget.ReactComponentDiv;
import org.sagebionetworks.web.client.widget.user.UserBadge;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class EntityBadgeViewImpl extends Composite implements EntityBadgeView {
	SynapseJSNIUtils synapseJSNIUtils;
	Widget modifiedByWidget;
	Presenter presenter;
	UserBadge modifiedByBadge;
	DateTimeUtils dateTimeUtils;

	public interface Binder extends UiBinder<Widget, EntityBadgeViewImpl> {
	}

	@UiField
	FocusPanel entityTypeIconContainer;
	@UiField
	EntityTypeIcon entityTypeIcon;
	@UiField
	FlowPanel entityContainer;
	@UiField
	TextBox idField;
	@UiField
	SimplePanel modifiedByField;
	@UiField
	Label modifiedOnField;
	@UiField
	Label createdOnField;
	@UiField
	Label sizeField;
	@UiField
	TextBox md5Field;
	@UiField
	TableData sizeTableData;
	@UiField
	TableData modifiedOnTableData;
	@UiField
	TableData createdOnTableData;
	@UiField
	TableData idTableData;
	@UiField
	TableData md5TableData;
	@UiField
	TableData modifiedByTableData;
	@UiField
	ReactComponentDiv iconsContainer;
	@UiField
	TableData downloadTableData;
	@UiField
	TableData entityTableData;
	@UiField
	org.gwtbootstrap3.client.ui.Anchor addToDownloadListLink;


	Callback onAttachCallback;
	Anchor entityAnchor;
	public static PlaceChanger placeChanger = null;
	HandlerRegistration clickHandlerRegistration;
	public static final String ENTITY_ID_ATTRIBUTE = "data-entity-id";

	public static final ClickHandler STANDARD_CLICKHANDLER = event -> {
		if (!DisplayUtils.isAnyModifierKeyDown(event)) {
			event.preventDefault();
			Widget panel = (Widget) event.getSource();
			String entityId = panel.getElement().getAttribute(ENTITY_ID_ATTRIBUTE);
			placeChanger.goTo(new Synapse(entityId));
		}
	};

	@Inject
	public EntityBadgeViewImpl(final Binder uiBinder, final SynapseJSNIUtils synapseJSNIUtils, PortalGinInjector ginInjector, GlobalApplicationState globalAppState, UserBadge modifiedByBadge, DateTimeUtils dateTimeUtils) {
		this.modifiedByBadge = modifiedByBadge;
		this.dateTimeUtils = dateTimeUtils;
		this.synapseJSNIUtils = synapseJSNIUtils;
		initWidget(uiBinder.createAndBindUi(this));
		if (EntityBadgeViewImpl.placeChanger == null) {
			EntityBadgeViewImpl.placeChanger = ginInjector.getGlobalApplicationState().getPlaceChanger();
		}

		idField.addClickHandler(TEXTBOX_SELECT_ALL_FIELD_CLICKHANDLER);
		md5Field.addClickHandler(TEXTBOX_SELECT_ALL_FIELD_CLICKHANDLER);
		addToDownloadListLink.addClickHandler(event -> {
			presenter.onAddToDownloadList();
		});
	}

	@Override
	public void setPresenter(Presenter p) {
		this.presenter = p;
	}

	@Override
	public void setOnAttachCallback(Callback onAttachCallback) {
		this.onAttachCallback = onAttachCallback;
	}

	@Override
	protected void onAttach() {
		super.onAttach();
		if (onAttachCallback != null) {
			onAttachCallback.invoke();
		}
	}

	@Override
	public void setEntity(final EntityHeader entityHeader) {
		entityTypeIconContainer.clear();
		if (entityHeader == null)
			throw new IllegalArgumentException("Entity is required");

		entityAnchor = new Anchor();
		clickHandlerRegistration = entityAnchor.addClickHandler(STANDARD_CLICKHANDLER);
		entityAnchor.setText(entityHeader.getName());
		entityAnchor.addStyleName("text-align-left text-overflow-ellipsis inline-block whitespace-nowrap overflowHidden maxWidth100");
		entityAnchor.setHref("#!Synapse:" + entityHeader.getId());
		entityAnchor.getElement().setAttribute(ENTITY_ID_ATTRIBUTE, entityHeader.getId());
		entityAnchor.setPaddingLeft(0);
		entityAnchor.setPaddingRight(0);
		entityAnchor.setPaddingTop(0);
		entityAnchor.setPaddingBottom(0);
		entityTypeIconContainer.setWidget(entityTypeIcon);
		entityTypeIcon.setType(getEntityTypeForClassName(entityHeader.getType()));
		entityContainer.add(entityAnchor);
		idField.setText(entityHeader.getId());
		if (entityHeader.getModifiedBy() != null) {
			modifiedByBadge.configure(entityHeader.getModifiedBy());
			if (!modifiedByBadge.asWidget().equals(modifiedByField.getWidget())) {
				modifiedByField.clear();
				modifiedByField.add(modifiedByBadge);
			}
		}
		if (entityHeader.getModifiedOn() != null) {
			modifiedOnField.setText(dateTimeUtils.getDateTimeString(entityHeader.getModifiedOn()));
		}
		if (entityHeader.getCreatedOn() != null) {
			createdOnField.setText(dateTimeUtils.getDateTimeString(entityHeader.getCreatedOn()));
		}
	}

	@Override
	public void showLoadError(String principalId) {
		clear();
		entityContainer.add(new HTML(DisplayConstants.ERROR_LOADING));
	}

	@Override
	public void clearEntityInformation() {
		entityContainer.clear();
	}
	
	@Override
	public void clearIcons() {
		iconsContainer.clear();
	}
	
	public void clear() {
		clearEntityInformation();
		clearIcons();
	}

	@Override
	public void setClickHandler(final ClickHandler handler) {
		if (clickHandlerRegistration != null) {
			clickHandlerRegistration.removeHandler();
		}
		clickHandlerRegistration = entityAnchor.addClickHandler(event -> {
			if (!DisplayUtils.isAnyModifierKeyDown(event)) {
				event.preventDefault();
				handler.onClick(event);
			}
		});
	}

	@Override
	public String getFriendlySize(Long contentSize, boolean abbreviatedUnits) {
		return DisplayUtils.getFriendlySize(contentSize, abbreviatedUnits);
	}

	@Override
	public void setError(String error) {
		Icon icon = new Icon(IconType.EXCLAMATION_CIRCLE);
		icon.setFixedWidth(true);
		icon.setEmphasis(Emphasis.DANGER);
		icon.setPull(Pull.RIGHT);
		Tooltip tooltip = new Tooltip(icon, error);
		tooltip.setPlacement(Placement.RIGHT);
		iconsContainer.add(tooltip);
	}

	@Override
	public void setSize(String s) {
		sizeField.setText(s);
	}

	@Override
	public void setMd5(String s) {
		md5Field.setText(s);
	}


	@Override
	public void setIcons(EntityBadgeIconsProps props, SynapseContextProviderProps providerProps) {
		ReactElement reactElement = React.createElementWithSynapseContext(
				SRC.SynapseComponents.EntityBadgeIcons,
				props,
				providerProps
		);

		ReactDOM.render(reactElement, iconsContainer.getElement());
	}

	@Override
	public boolean isInViewport() {
		return DisplayUtils.isInViewport(this);
	}

	@Override
	public void showAddToDownloadList() {
		addToDownloadListLink.setVisible(true);
	}

	@Override
	public void setModifiedByUserBadgeClickHandler(ClickHandler handler) {
		modifiedByBadge.setCustomClickHandler(handler);
	}

	@Override
	public void showMinimalColumnSet() {
		sizeTableData.setVisible(false);
		sizeTableData.setStyleName("");
		modifiedOnTableData.setVisible(false);
		modifiedOnTableData.setStyleName("");
		// idTableData.setVisible(false);
		createdOnTableData.setVisible(false);
		createdOnTableData.setStyleName("");
		// modifiedByTableData.setVisible(false);
		// modifiedByTableData.setStyleName("");
		md5TableData.setVisible(false);
		md5TableData.setStyleName("");
		downloadTableData.setVisible(false);
		downloadTableData.setStyleName("");
	}
}
