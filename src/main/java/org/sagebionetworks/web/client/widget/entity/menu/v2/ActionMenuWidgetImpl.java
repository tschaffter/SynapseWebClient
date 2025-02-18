package org.sagebionetworks.web.client.widget.entity.menu.v2;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.gwtbootstrap3.client.ui.constants.IconType;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

/**
 * This ActionMenuWidget implementation will synchronize with its view to find the ActionViews
 * associated with each action. The synchronize is done once during construction.
 * 
 * @author John
 * 
 */
public class ActionMenuWidgetImpl implements ActionMenuWidget, ActionListener {

	ActionMenuWidgetView view;

	Map<Action, ActionView> actionViewMap;
	Map<Action, List<ActionListener>> actionListenerMap;

	private boolean isLoading = false;

	@Inject
	public ActionMenuWidgetImpl(ActionMenuWidgetView view) {
		this.view = view;
		this.actionViewMap = new HashMap<Action, ActionView>();
		this.actionListenerMap = new HashMap<Action, List<ActionListener>>();
		// synchronize with the view
		for (ActionView av : this.view.listActionViews()) {
			Action action = av.getAction();
			if (action == null) {
				throw new IllegalArgumentException("ActionView has a null action");
			}
			if (this.actionViewMap.containsKey(action)) {
				throw new IllegalArgumentException("Action " + action + " was applied to more than one ActionView");
			}
			this.actionViewMap.put(av.getAction(), av);
			av.addActionListener(this);
			this.actionListenerMap.put(av.getAction(), new LinkedList<ActionListener>());
		}
		reset();
	}

	@Override
	public Widget asWidget() {
		return view.asWidget();
	}

	@Override
	public void setActionVisible(Action action, boolean visible) {
		getActionView(action).setVisible(visible);
		updateViewWithNumberOfActionsVisible();
	}

	@Override
	public void setActionEnabled(Action action, boolean enabled) {
		getActionView(action).setEnabled(enabled);
		updateViewWithNumberOfActionsVisible();
	}


	@Override
	public void setActionText(Action action, String text) {
		getActionView(action).setText(text);

	}

	@Override
	public void setActionIcon(Action action, IconType icon) {
		getActionView(action).setIcon(icon);
	}

	/**
	 * Get the view for the given action from the map.
	 * 
	 * @param action
	 * @throws IllegalArgumentException When there is no view mapped to the given action.
	 * @return
	 */
	private ActionView getActionView(Action action) {
		ActionView actionView = this.actionViewMap.get(action);
		if (actionView == null) {
			throw new IllegalArgumentException("No action view found for action: " + action);
		}
		return actionView;
	}

	private List<ActionListener> getActionListeners(Action action) {
		List<ActionListener> list = this.actionListenerMap.get(action);
		if (list == null) {
			throw new IllegalArgumentException("No action list found for action: " + action);
		}
		return list;
	}

	@Override
	public void setActionListener(Action action, ActionListener listner) {
		List<ActionListener> actionListeners = getActionListeners(action);
		actionListeners.clear();
		actionListeners.add(listner);
	}

	@Override
	public void addActionListener(Action action, ActionListener listener) {
		getActionListeners(action).add(listener);
	}

	@Override
	public void reset() {
		// Hide all widgets and clear all Listeners
		hideAllActions();
		for (Action action : this.actionListenerMap.keySet()) {
			getActionListeners(action).clear();
		}
	}

	@Override
	public void hideAllActions() {
		for (Action action : this.actionListenerMap.keySet()) {
			getActionView(action).setVisible(false);
		}
		setACTDividerVisible(false);
		updateViewWithNumberOfActionsVisible();
	}

	private void updateViewWithNumberOfActionsVisible() {
		int numVisible = 0;
		Action singleAction = null;
		for (Action action : this.actionListenerMap.keySet()) {
			if (getActionView(action).isVisible()) {
				numVisible++;
				singleAction = action;
			}
		}

		if (numVisible == 1) {
			final Action finalSingleAction = singleAction;
			view.setSingleActionButton(actionViewMap.get(singleAction).getText(), clickEvent -> onAction(finalSingleAction));
		}

		view.updateVisibleActions(numVisible, isLoading);
	}

	@Override
	public void onAction(Action action) {
		// forward to the listeners
		List<ActionListener> listeners = getActionListeners(action);
		if (listeners.size() == 0) {
			throw new IllegalArgumentException("Attempted to invoke action " + action.name() + " with no listeners present");
		}
		for (ActionListener listener : listeners) {
			listener.onAction(action);
		}
	}

	@Override
	public void addControllerWidget(IsWidget controllerWidget) {
		view.addControllerWidget(controllerWidget);
	}

	@Override
	public void setACTDividerVisible(boolean visible) {
		view.setACTDividerVisible(visible);
	}

	@Override
	public void setToolsButtonIcon(String text, IconType icon) {
		view.setToolsButtonIcon(text, icon);
	}

	@Override
	public void setEditTableDataTooltipText(String tooltipText) {
		view.setEditTableDataTooltipText(tooltipText);
	}

	@Override
	public void setIsLoading(boolean isLoading) {
		this.isLoading = isLoading;
		updateViewWithNumberOfActionsVisible();
	}
}
