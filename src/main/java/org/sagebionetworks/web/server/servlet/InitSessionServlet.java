package org.sagebionetworks.web.server.servlet;

import static org.sagebionetworks.web.client.cookie.CookieKeys.USER_LOGIN_TOKEN;

import java.io.BufferedReader;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.web.client.StackEndpoints;
import org.sagebionetworks.web.shared.WebConstants;

/**
 * Servlet for setting the session token HttpOnly cookie.
 */
public class InitSessionServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	protected static final ThreadLocal<HttpServletRequest> perThreadRequest = new ThreadLocal<HttpServletRequest>();
	
	private SynapseProvider synapseProvider = new SynapseProviderImpl();
	/**
	 * Unit test can override this.
	 *
	 * @param fileHandleProvider
	 */
	public void setSynapseProvider(SynapseProvider synapseProvider) {
		this.synapseProvider = synapseProvider;
	}
	
	@Override
	protected void service(HttpServletRequest arg0, HttpServletResponse arg1)
			throws ServletException, IOException {
		InitSessionServlet.perThreadRequest.set(arg0);
		super.service(arg0, arg1);
	}

	@Override
	public void service(ServletRequest arg0, ServletResponse arg1)
			throws ServletException, IOException {
		super.service(arg0, arg1);
	}
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

	}
	
	/**
	 * Create a new Synapse client.
	 *
	 * @return
	 */
	private SynapseClient createNewClient(String sessionToken) {
		SynapseClient client = synapseProvider.createNewClient();
		client.setAuthEndpoint(StackEndpoints.getAuthenticationServicePublicEndpoint());
		client.setRepositoryEndpoint(StackEndpoints.getRepositoryServiceEndpoint());
		client.setFileEndpoint(StackEndpoints.getFileServiceEndpoint());
		if (sessionToken != null)
			client.setSessionToken(sessionToken);
		return client;
	}

	@Override
	public void doPost(final HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		// return the Set-Cookie response with the session token
		try {
			StringBuffer jb = new StringBuffer();
			String line = null;
			BufferedReader reader = request.getReader();
			while ((line = reader.readLine()) != null)
				jb.append(line);
			JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jb.toString());
			Session s = new Session(adapter);
			String sessionToken = s.getSessionToken();
			if (sessionToken == null || sessionToken.isEmpty()) {
				sessionToken = WebConstants.EXPIRE_SESSION_TOKEN;
			}
			Cookie cookie = new Cookie(USER_LOGIN_TOKEN, sessionToken);
			
			if (!WebConstants.EXPIRE_SESSION_TOKEN.equals(sessionToken)) {
				// validate session token is valid
				createNewClient(sessionToken).getUserSessionData();
				cookie.setMaxAge(60*60*24);
			} else {
				cookie.setMaxAge(0);
			}
			boolean isSecure = "https".equals(request.getScheme().toLowerCase());
			cookie.setSecure(isSecure);
			cookie.setHttpOnly(true);
			cookie.setPath("/");
			
			String domain = request.getServerName();
			String lowerCaseDomain = domain.toLowerCase();
			if (lowerCaseDomain.contains(".synapse.org")) {
				cookie.setDomain(".synapse.org");
			}
			response.addCookie(cookie);
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.getOutputStream().write("Invalid session token".getBytes("UTF-8"));
			response.getOutputStream().flush();
		}
	}
}
