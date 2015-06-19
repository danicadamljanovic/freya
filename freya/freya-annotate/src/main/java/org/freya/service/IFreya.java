package org.freya.service;

import javax.servlet.http.HttpServletRequest;

public interface IFreya {

	public String ask(String query, HttpServletRequest request) throws Exception;

	public String refine(String query, String[] voteId, HttpServletRequest request) throws Exception;
}
