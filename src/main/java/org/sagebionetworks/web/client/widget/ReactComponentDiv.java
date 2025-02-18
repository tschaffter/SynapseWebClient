package org.sagebionetworks.web.client.widget;

import org.gwtbootstrap3.client.ui.html.Div;

/**
 * Automatically unmounts the ReactComponent (if any) inside this div when this container is detached/unloaded.
 */
public class ReactComponentDiv extends Div {
	@Override
	protected void onUnload() {
		ReactComponentLifecycleUtils.onUnload(this.getElement());
		super.onUnload();
	}

	@Override
	public void clear() {
		ReactComponentLifecycleUtils.clear(this.getElement());
		super.clear();
	}
}
