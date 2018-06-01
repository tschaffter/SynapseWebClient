package org.sagebionetworks.web.server.servlet.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

/**
 * prevent any communications from being sent over HTTP to this domain.
 * 
 * @author jayhodgson
 * 
 */
public class HSTSFilter implements Filter {
	
	public static final long MAX_AGE_SECONDS=60*60*24;  //a day
	
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain filterChain) throws IOException, ServletException {
		HttpServletResponse httpResponse = (HttpServletResponse) response;
		//PLFM-4966: add hsts header to response.  It only has an effect if connection is https, so this will have no impact in the client dev environment.
		httpResponse.setHeader("Strict-Transport-Security", "max-age=" + MAX_AGE_SECONDS);
		filterChain.doFilter(request, response);
	}
	
	public void init(FilterConfig config) throws ServletException {
	}

	@Override
	public void destroy() {
	}
}
