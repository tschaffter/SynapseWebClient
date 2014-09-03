package org.sagebionetworks.web.client.widget.table.v2;

import org.sagebionetworks.web.client.SynapseClientAsync;
import org.sagebionetworks.web.client.widget.table.v2.results.QueryInputListener;
import org.sagebionetworks.web.client.widget.table.v2.results.QueryResultListener;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

/**
 * This widget provides a text box for query input and a button to execute a query.
 * 
 * @author John
 *
 */
public class QueryInputWidget implements QueryInputView.Presenter, IsWidget, QueryResultListener{
	
	QueryInputView view;
	SynapseClientAsync synapseClient;
	QueryInputListener queryInputListener;
	String startQuery;
	
	@Inject
	public QueryInputWidget(QueryInputView view, SynapseClientAsync synapseClient){
		this.view = view;
		this.synapseClient = synapseClient;
		this.view.setPresenter(this);
	}

	/**
	 * Configure this widget.
	 * @param startQuery
	 * @param queryInputListener
	 */
	public void configure(String startQuery, QueryInputListener queryInputListener){
		this.startQuery = startQuery;
		this.queryInputListener = queryInputListener;
		onReset();
	}

	@Override
	public Widget asWidget() {
		return this.view.asWidget();
	}

	@Override
	public void onExecuteQuery() {
		// Get the query from the view
		final String sql = view.getInputQueryString();
		if(sql == null || "".equals(sql.trim())){
			view.showInputError(true);
			view.setInputErrorMessage("An empty query is not valid.");
		}else{
			// validate the query
			synapseClient.validateTableQuery(sql, new AsyncCallback<Void>() {
				
				@Override
				public void onSuccess(Void result) {
					view.showInputError(false);
					queryInputListener.onExecuteQuery(sql);
				}
				
				@Override
				public void onFailure(Throwable caught) {
					view.showInputError(true);
					view.setInputErrorMessage(caught.getMessage());
				}
			});
		}
	}
	
	@Override
	public void queryExecutionStarted() {
		view.setQueryInputLoading(true);
	}

	@Override
	public void queryExecutionFinished() {
		view.setQueryInputLoading(false);
	}

	@Override
	public void onReset() {
		view.setInputQueryString(startQuery);
		view.showInputError(false);
	}

}
