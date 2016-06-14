package org.sagebionetworks.web.client.widget.docker;

import java.util.List;

import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.ListGroup;
import org.gwtbootstrap3.client.ui.constants.HeadingSize;
import org.sagebionetworks.repo.model.entity.query.EntityQueryResult;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.widget.pagination.PaginationWidget;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class DockerRepoListWidgetViewImpl implements DockerRepoListWidgetView {
	@UiField
	ListGroup dockerList;
	@UiField
	Button addExternalRepo;
	@UiField
	SimplePanel paginationPanel;
	@UiField
	SimplePanel addExternalRepoModalPanel;

	Widget widget;
	Presenter presenter;

	public interface Binder extends UiBinder<Widget, DockerRepoListWidgetViewImpl> {}

	@Inject
	public DockerRepoListWidgetViewImpl(Binder binder) {
		this.widget = binder.createAndBindUi(this);
		addExternalRepo.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				presenter.onClickAddExternalRepo();
			}
		});
	}

	@Override
	public void setPresenter(Presenter presenter) {
		this.presenter = presenter;
	}

	@Override
	public Widget asWidget() {
		return widget;
	}

	@Override
	public void showLoading() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void showInfo(String title, String message) {
		DisplayUtils.showInfo(title, message);
	}

	@Override
	public void addRepos(List<EntityQueryResult> headers) {
		for(final EntityQueryResult header: headers){
			dockerList.add(new DockerRepoListGroupItem(HeadingSize.H4, header, new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					presenter.onRepoClicked(header.getId());
				}
			}));
		}
	}

	@Override
	public void clear() {
		dockerList.clear();
	}

	@Override
	public void showErrorMessage(String message) {
		DisplayUtils.showErrorMessage(message);
	}

	@Override
	public void addPaginationWidget(PaginationWidget paginationWidget) {
		paginationPanel.add(paginationWidget);
	}

	@Override
	public void showPaginationVisible(boolean visible) {
		paginationPanel.setVisible(visible);
	}

	@Override
	public void setAddExternalRepoButtonVisible(boolean visibile) {
		addExternalRepo.setVisible(visibile);
	}

	@Override
	public void addExternalRepoModal(IsWidget addExternalRepoModel) {
		this.addExternalRepoModalPanel.add(addExternalRepoModel);
	}
}
