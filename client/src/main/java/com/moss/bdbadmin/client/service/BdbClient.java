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
package com.moss.bdbadmin.client.service;

import java.io.IOException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.OptionsMethod;
import org.apache.commons.httpclient.methods.PutMethod;

import com.moss.bdbadmin.api.db.DbInfo;
import com.moss.bdbadmin.api.entry.EntryInfo;
import com.moss.bdbadmin.api.map.Category;
import com.moss.bdbadmin.api.util.AuthenticationHeader;
import com.moss.identity.IdProof;

public class BdbClient {
	
	private static JAXBContext context;
	
	public static void init() {
		try {
			context = JAXBContext.newInstance(Category.class, DbInfo.class, EntryInfo.class);
		}
		catch (JAXBException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	public static void dispose() {
		context = null;
	}
	
	private final HttpClient httpClient;
	private final String baseUrl;
	
	public BdbClient(HttpClient httpClient, String baseUrl) {
		if (context == null) {
			init();
		}
		this.httpClient = httpClient;
		this.baseUrl = baseUrl;
	}
	
	public String url() {
		return baseUrl;
	}

	public Category map(IdProof assertion) throws ServiceException {
		try {
			OptionsMethod method = new OptionsMethod(baseUrl);
			method.setRequestHeader(AuthenticationHeader.HEADER_NAME, AuthenticationHeader.encode(assertion));
			
			int result = httpClient.executeMethod(method);
			if (result != 200) {
				throw new ServiceException(result);
			}
			else {
				Unmarshaller u = context.createUnmarshaller();
				return (Category)u.unmarshal(method.getResponseBodyAsStream());
			}
		}
		catch (Exception ex) {
			throw new ServiceFailure(ex);
		}
	}
	
	public void head(IdProof assertion, String path) throws ServiceException {
		try {
			HeadMethod method = new HeadMethod(baseUrl + "/" + path);
			method.setRequestHeader(AuthenticationHeader.HEADER_NAME, AuthenticationHeader.encode(assertion));
			
			int result = httpClient.executeMethod(method);
			if (result != 200) {
				throw new ServiceException(result);
			}
		}
		catch (IOException ex) {
			throw new ServiceFailure(ex);
		}
	}
	
	public DbInfo dbInfo(IdProof assertion, String path) throws ServiceException {
		try {
			GetMethod method = new GetMethod(baseUrl + "/" + path);
			method.setRequestHeader(AuthenticationHeader.HEADER_NAME, AuthenticationHeader.encode(assertion));
			
			int result = httpClient.executeMethod(method);
			if (result != 200) {
				throw new ServiceException(result);
			}
			else {
				Unmarshaller u = context.createUnmarshaller();
				return (DbInfo)u.unmarshal(method.getResponseBodyAsStream());
			}
		}
		catch (Exception ex) {
			throw new ServiceFailure(ex);
		}
	}
	
	public EntryInfo entryInfo(IdProof assertion, String path) throws ServiceException {
		try {
			OptionsMethod method = new OptionsMethod(baseUrl + "/" + path);
			method.setRequestHeader(AuthenticationHeader.HEADER_NAME, AuthenticationHeader.encode(assertion));
			
			int result = httpClient.executeMethod(method);
			if (result != 200) {
				throw new ServiceException(result);
			}
			else {
				Unmarshaller u = context.createUnmarshaller();
				return (EntryInfo)u.unmarshal(method.getResponseBodyAsStream());
			}
		}
		catch (Exception ex) {
			throw new ServiceFailure(ex);
		}
	}
	
	public BdbResourceHandle get(IdProof assertion, String path) throws ServiceException {
		try {
			GetMethod method = new GetMethod(baseUrl + "/" + path);
			method.setRequestHeader(AuthenticationHeader.HEADER_NAME, AuthenticationHeader.encode(assertion));
			
			int result = httpClient.executeMethod(method);
			if (result != 200) {
				throw new ServiceException(result);
			}
			else {
				return new BdbMemoryResourceHandle(method.getResponseBody());
			}
		}
		catch (IOException ex) {
			throw new ServiceFailure(ex);
		}
	}

	public void put(IdProof assertion, String path, BdbResourceHandle handle) throws ServiceException {
		try {
			PutMethod method = new PutMethod(baseUrl + "/" + path);
			method.setRequestHeader(AuthenticationHeader.HEADER_NAME, AuthenticationHeader.encode(assertion));
			method.setRequestEntity(new InputStreamRequestEntity(handle.read()));
			
			int result = httpClient.executeMethod(method);
			if (result != 200) {
				throw new ServiceException(result);
			}
		}
		catch (IOException ex) {
			throw new ServiceFailure(ex);
		}
	}
	
	public void delete(IdProof assertion, String path) throws ServiceException {
		try {
			DeleteMethod method = new DeleteMethod(baseUrl + "/" + path);
			method.setRequestHeader(AuthenticationHeader.HEADER_NAME, AuthenticationHeader.encode(assertion));
			
			int result = httpClient.executeMethod(method);
			if (result != 200) {
				throw new ServiceException(result);
			}
		}
		catch (IOException ex) {
			throw new ServiceFailure(ex);
		}
	}
}
