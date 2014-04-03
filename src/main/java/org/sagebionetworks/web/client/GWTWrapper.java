package org.sagebionetworks.web.client;

import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.xhr.client.XMLHttpRequest;

public interface GWTWrapper {
	String getHostPageBaseURL();

	String getModuleBaseURL();

	void replaceThisWindowWith(String url);

	String encodeQueryString(String queryString);

	XMLHttpRequest createXMLHttpRequest();

	NumberFormat getNumberFormat(String pattern);
	
	String getHostPrefix(); 
	
	/**
	 * nextInt(upperbound) selects a value of x where 0 <= x < upperbound
	 * @param upperbound
	 * @return
	 */
	int getRandomNextInt(int upperbound);
}
