/**
 * Copyright (C) 2013, Moss Computing Inc.
 *
 * This file is part of bdbadmin.
 *
 * bdbadmin is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * bdbadmin is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with bdbadmin; see the file COPYING.  If not, write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole
 * combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent
 * modules, and to copy and distribute the resulting executable under
 * terms of your choice, provided that you also meet, for each linked
 * independent module, the terms and conditions of the license of that
 * module.  An independent module is a module which is not derived from
 * or based on this library.  If you modify this library, you may extend
 * this exception to your version of the library, but you are not
 * obligated to do so.  If you do not wish to do so, delete this
 * exception statement from your version.
 */
package com.moss.bdbadmin.openejb;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.openejb.server.httpd.HttpListener;
import org.apache.openejb.server.httpd.HttpRequest;
import org.apache.openejb.server.httpd.HttpResponse;

import com.moss.bdbadmin.api.util.AuthenticationHeader;
import com.moss.bdbadmin.core.BdbCategory;
import com.moss.bdbadmin.core.BdbDb;
import com.moss.bdbadmin.core.BdbEntityResource;
import com.moss.bdbadmin.core.BdbEnv;
import com.moss.bdbadmin.core.BdbMapResource;
import com.moss.bdbadmin.core.BdbService;
import com.moss.bdbadmin.core.NotAuthorizedException;
import com.moss.bdbadmin.core.ResourcePathException;
import com.moss.bdbadmin.core.ServiceResource;
import com.moss.bdbadmin.core.ServiceResourceVisitor;
import com.moss.identity.IdProof;
import com.moss.identity.tools.IdProovingException;

public class BdbAdminOpenEjbAdapter implements HttpListener {
	
	private final String contextPath;
	private final BdbService service;
	
	public BdbAdminOpenEjbAdapter(String contextPath, BdbService service) {
		this.contextPath = contextPath;
		this.service = service;
	}
	
	public void onMessage(final HttpRequest request, final HttpResponse response) throws Exception {
		
		final IdProof assertion;
		{
			IdProof a = null;
			
			String value = request.getHeader(AuthenticationHeader.HEADER_NAME);
			if (value != null && value.length() > 0) {
				try {
					a = AuthenticationHeader.decode(value);
				}
				catch (Exception ex) {
					ex.printStackTrace();
					a = null;
				}
			}
			else {
				System.out.println("No assertion included in request header");
				a = null;
			}
			
			assertion = a;
		}
		
		final ServiceResource resource;
		{
			String path;
			if (request.getURI().getPath().length() >= contextPath.length()) {
				path = request.getURI().getPath().substring(contextPath.length()).trim();
			}
			else {
				path = request.getURI().getPath();
			}

			ServiceResource r = null;;
			try {
				r = service.resolve(path);
			}
			catch (ResourcePathException ex) {
				ex.printStackTrace();
			}
			
			resource = r;
		}
		
		if (assertion == null || resource == null) {
			response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
		}
		else {
			abstract class Handler {
				abstract void handle() throws Exception;
			}

			Handler handler = resource.acceptVisitor(new ServiceResourceVisitor<Handler>() {
				public Handler visit(BdbMapResource map) {
					return new Handler() {
						public void handle() throws IdProovingException, NotAuthorizedException, IOException {
							if ("OPTIONS".equals(request.getMethod().name())) {
								byte[] data = service.map(assertion);
								response.setHeader("Content-Length", Integer.toString(data.length));
								response.getOutputStream().write(data);
								response.setStatusCode(HttpStatus.SC_OK);
							}
							else {
								response.setStatusCode(HttpStatus.SC_METHOD_NOT_ALLOWED);
							}
						}
					};
				}
				public Handler visit(BdbCategory category) {
					return null;
				}
				public Handler visit(BdbEnv env) {
					return null;
				}
				public Handler visit(final BdbDb db) {
					return new Handler() {
						public void handle() throws IdProovingException, NotAuthorizedException, IOException {
							if ("GET".equals(request.getMethod().name())) {
								byte[] data = service.dbInfo(assertion, db);
								response.setHeader("Content-Length", Integer.toString(data.length));
								response.getOutputStream().write(data);
								response.setStatusCode(HttpStatus.SC_OK);
							}
							else if ("DELETE".equals(request.getMethod().name())) {
								service.clearDb(assertion, db);
								response.setStatusCode(HttpStatus.SC_OK);
							}
							else {
								response.setStatusCode(HttpStatus.SC_METHOD_NOT_ALLOWED);
							}
						}
					};
				}
				public Handler visit(final BdbEntityResource entity) {
					return new Handler() {
						public void handle() throws IdProovingException, NotAuthorizedException, IOException {
							if ("OPTIONS".equals(request.getMethod().name())) {
								byte[] data = service.entryInfo(assertion, entity);
								if (data == null) {
									response.setStatusCode(HttpStatus.SC_NOT_FOUND);
								}
								else {
									response.setHeader("Content-Length", Integer.toString(data.length));
									response.getOutputStream().write(data);
									response.setStatusCode(HttpStatus.SC_OK);
								}
							}
							else if ("GET".equals(request.getMethod().name())) {
								byte[] data = service.getEntry(assertion, entity);
								if (data == null) {
									response.setStatusCode(HttpStatus.SC_NOT_FOUND);
								}
								else {
									response.setHeader("Content-Length", Integer.toString(data.length));
									response.getOutputStream().write(data);
									response.setStatusCode(HttpStatus.SC_OK);
								}
							}
							else if ("HEAD".equals(request.getMethod().name())) {
								byte[] data = service.getEntry(assertion, entity);
								if (data == null) {
									response.setStatusCode(HttpStatus.SC_NOT_FOUND);
								}
								else {
									response.setStatusCode(HttpStatus.SC_OK);
								}
							}
							else if ("PUT".equals(request.getMethod().name())) {
								
								byte[] input;
								{
									InputStream in = request.getInputStream();
									ByteArrayOutputStream out = new ByteArrayOutputStream();
									
									byte[] buffer = new byte[1023 * 10]; //10k buffer
									for(int numRead = in.read(buffer); numRead!=-1; numRead = in.read(buffer)){
										out.write(buffer, 0, numRead);
									}

									in.close();
									out.close();
									
									input = out.toByteArray();
								}
								
								service.putEntry(assertion, entity, input);
								response.setStatusCode(HttpStatus.SC_OK);
							}
							else if ("DELETE".equals(request.getMethod().name())) {
								if (service.deleteEntry(assertion, entity)) {
									response.setStatusCode(HttpStatus.SC_OK);									
								}
								else {
									response.setStatusCode(HttpStatus.SC_NOT_FOUND);
								}
							}
							else {
								response.setStatusCode(HttpStatus.SC_METHOD_NOT_ALLOWED);
							}
						}
					};
				}
			});

			if (handler == null) {
				System.out.println("Cannot perform any methods on requested path");
				response.setStatusCode(HttpStatus.SC_METHOD_NOT_ALLOWED);
			}
			else {
				try {
					handler.handle();
				}
				catch (IdProovingException ex) {
					ex.printStackTrace();
					response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
				}
				catch (NotAuthorizedException ex) {
					ex.printStackTrace();
					response.setStatusCode(HttpStatus.SC_UNAUTHORIZED);
				}
				catch (Exception ex) {
					throw new ServletException(ex);
				}
			}
		}
    	
    	response.getOutputStream().close();
	}
}
