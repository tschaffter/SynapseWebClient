package org.sagebionetworks.web.client.widget.login;

import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.SynapseJSNIUtils;
import org.sagebionetworks.web.client.context.SynapseContextPropsProvider;
import org.sagebionetworks.web.client.jsni.SynapseContextProviderPropsJSNIObject;
import org.sagebionetworks.web.client.place.Profile;
import org.sagebionetworks.web.client.place.Synapse.ProfileArea;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.view.users.RegisterAccountViewImpl;
import org.sagebionetworks.web.client.widget.ReactComponentDiv;

import com.google.gwt.dom.client.Element;
import com.google.gwt.place.shared.Place;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class LoginWidgetViewImpl implements LoginWidgetView, IsWidget {
	public interface LoginWidgetViewImplUiBinder extends UiBinder<Widget, LoginWidgetViewImpl> {
	}

	@UiField
	ReactComponentDiv srcLoginContainer;
	Widget widget;
	SynapseJSNIUtils jsniUtils;
	GlobalApplicationState globalAppState;
	AuthenticationController authController;
	SynapseContextPropsProvider propsProvider;

	@Inject
	public LoginWidgetViewImpl(LoginWidgetViewImplUiBinder binder, SynapseJSNIUtils jsniUtils,
							   GlobalApplicationState globalAppState, AuthenticationController authController,
							   SynapseContextPropsProvider propsProvider) {
		widget = binder.createAndBindUi(this);
		this.jsniUtils = jsniUtils;
		this.globalAppState = globalAppState;
		this.authController = authController;
		this.propsProvider = propsProvider;
		widget.addAttachHandler(event -> {
			if (event.isAttached()) {
				_createSRCLogin(srcLoginContainer.getElement(), this,
						RegisterAccountViewImpl.OAUTH_CALLBACK_URL, propsProvider.getJsniContextProps());
			}
		});
	}

	public void postLogin() {
		Place defaultPlace = new Profile(Profile.VIEW_PROFILE_TOKEN, ProfileArea.PROJECTS);
		globalAppState.gotoLastPlace(defaultPlace);
		authController.checkForUserChange(null);
	}

	private static native void _createSRCLogin(Element el, LoginWidgetViewImpl loginWidgetView,
	   String ssoRedirectUrl, SynapseContextProviderPropsJSNIObject wrapperProps) /*-{
		try {
			function sessionCallback() {
				loginWidgetView.@org.sagebionetworks.web.client.widget.login.LoginWidgetViewImpl::postLogin()();
			}

			var props = {
				ssoRedirectUrl : ssoRedirectUrl,
				sessionCallback : sessionCallback
			};

			var component = $wnd.React.createElement($wnd.SRC.SynapseComponents.LoginPage, props, null)
			var wrapper = $wnd.React.createElement($wnd.SRC.SynapseContext.SynapseContextProvider, wrapperProps, component)

			$wnd.ReactDOM.render(wrapper, el);
		} catch (err) {
			console.error(err);
		}
	}-*/;

	@Override
	public Widget asWidget() {
		return widget;
	}

	@Override
	public void clear() {
	}

	@Override
	public void setVisible(boolean visible) {
		widget.setVisible(visible);
	}
}
