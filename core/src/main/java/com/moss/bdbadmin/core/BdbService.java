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
package com.moss.bdbadmin.core;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import com.moss.bdbadmin.api.db.DbInfo;
import com.moss.bdbadmin.api.entry.EntryInfo;
import com.moss.bdbadmin.api.map.Category;
import com.moss.bdbadmin.api.util.KeyFactory;
import com.moss.identity.IdProof;
import com.moss.identity.IdVerifier;
import com.moss.identity.tools.IdProovingException;
import com.moss.identity.tools.IdTool;
import com.moss.identity.veracity.VeracityCachedKey;
import com.moss.identity.veracity.VeracityCachedKeyProvider;
import com.moss.identity.veracity.VeracityIdToolPlugin;
import com.moss.rpcutil.proxy.ProxyFactory;
import com.moss.veracity.api.util.HexUtil;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Transaction;

public final class BdbService {
	
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
	
	private final IdTool agent;
	private final BdbCategory map;
	private final Authorizer authorizer;
	
	public BdbService(BdbCategory map, Authorizer authorizer, ProxyFactory proxyFactory) {
		
		if (context == null) {
			init();
		}
		
		if (map == null) {
			throw new NullPointerException();
		}
		if (authorizer == null) {
			throw new NullPointerException();
		}
		
		VeracityIdToolPlugin provider = new VeracityIdToolPlugin(proxyFactory);
		provider.setKeyProvider(new HashMapVeracityCachedKeyProvider());
		
		agent = new IdTool();
		agent.addHandler(provider);
		
		this.map = map;
		this.authorizer = authorizer;
	}
	
	/**
	 * Exposes a map of the resources available on this service.
	 */
	public byte[] map(IdProof assertion) throws IdProovingException, NotAuthorizedException {

		checkCreds(assertion, AuthorizationLevel.READ);
		
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			Marshaller m = context.createMarshaller();
			m.marshal(map.toDto(), out);
			return out.toByteArray();
		}
		catch (JAXBException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	public byte[] dbInfo(IdProof assertion, BdbDb dbResource) throws IdProovingException, NotAuthorizedException {

		checkCreds(assertion, AuthorizationLevel.READ);
		
		DbInfo info = new DbInfo();
		
		Transaction t = null;
		Cursor c = null;
		try {
			Database db = dbResource.getDb();
			long count = db.count();
			
			byte[] firstKey, lastKey;
			if (count == 0) {
				firstKey = null;
				lastKey = null;
			}
			else {
				t = db.getEnvironment().beginTransaction(null, null);

				CursorConfig cursorConf = new CursorConfig();
				cursorConf.setReadUncommitted(true);

				c = db.openCursor(t, cursorConf);

				DatabaseEntry key = new DatabaseEntry();
				DatabaseEntry value = new DatabaseEntry();
				value.setPartial(0, 0, true);
			
				if (OperationStatus.SUCCESS == c.getFirst(key, value, LockMode.DEFAULT)) {
					firstKey = key.getData();
				}
				else {
					firstKey = null;
				}
			
				if (OperationStatus.SUCCESS == c.getFirst(key, value, LockMode.DEFAULT)) {
					lastKey = key.getData();
				}
				else {
					lastKey = null;
				}
				
				c.close();
				c = null;
				
				t.commit();
				t = null;
			}

			info = new DbInfo();
			info.setEntryCount(count);
			info.setFirstKey(firstKey);
			info.setLastKey(lastKey);
		}
		catch (Exception ex) {
			
			if (c != null) {
				try {
					c.close();
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			if (t != null) {
				try {
					t.abort();
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			throw new RuntimeException(ex);
		}
		
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			Marshaller m = context.createMarshaller();
			m.marshal(info, out);
			return out.toByteArray();
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	public byte[] entryInfo(IdProof assertion, BdbEntityResource resource) throws IdProovingException, NotAuthorizedException {
		
		checkCreds(assertion, AuthorizationLevel.READ);
		
		EntryInfo info = null;

		Transaction t = null;
		Cursor c = null;
		try {
			Database db = resource.db().getDb();
			t = db.getEnvironment().beginTransaction(null, null);
			
			boolean exists;
			{
				DatabaseEntry key = new DatabaseEntry(resource.id());
				DatabaseEntry value = new DatabaseEntry();
				
				exists = OperationStatus.SUCCESS == db.get(t, key, value, LockMode.READ_UNCOMMITTED);
			}
			
			if (exists) {
			
				CursorConfig cursorConf = new CursorConfig();
				cursorConf.setReadUncommitted(true);

				c = db.openCursor(t, cursorConf);

				byte[] previousKey, nextKey;

				DatabaseEntry key = new DatabaseEntry(resource.id());
				DatabaseEntry value = new DatabaseEntry();
				value.setPartial(0, 0, true);

				if (OperationStatus.SUCCESS != c.getSearchKey(key, value, LockMode.DEFAULT)) {
					throw new RuntimeException("Found entry before, but cannot find it now");
				}

				if (OperationStatus.SUCCESS != c.getPrev(key, value, LockMode.DEFAULT)) {
					previousKey = null;
				}
				else {
					previousKey = key.getData();
				}

				key.setData(resource.id());
				if (OperationStatus.SUCCESS != c.getSearchKey(key, value, LockMode.DEFAULT)) {
					throw new RuntimeException("Found before, but cannot find it now");
				}

				if (OperationStatus.SUCCESS != c.getNext(key, value, LockMode.DEFAULT)) {
					nextKey = null;
				}
				else {
					nextKey = key.getData();
				}
			
				info = new EntryInfo();
				info.setKeyNext(nextKey);
				info.setKeyPrevious(previousKey);
			
				c.close();
				c = null;
			}
			
			t.commit();
			t = null;
		}
		catch (Exception ex) {
			
			if (c != null) {
				try {
					c.close();
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			if (t != null) {
				try {
					t.abort();
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			throw new RuntimeException(ex);
		}
		
		if (info == null) {
			return null;
		}
		else {
			try {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				Marshaller m = context.createMarshaller();
				m.marshal(info, out);
				return out.toByteArray();
			}
			catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
	}
	
	public byte[] getEntry(IdProof assertion, BdbEntityResource resource) throws IdProovingException, NotAuthorizedException {
		
		checkCreds(assertion, AuthorizationLevel.READ);

		Database db = resource.db().getDb();
		
		try {
			byte[] keyData = resource.id();
			
			DatabaseEntry key = new DatabaseEntry(keyData);
			DatabaseEntry value = new DatabaseEntry();
			
			OperationStatus status = db.get(null, key, value, LockMode.DEFAULT);
			
			if (OperationStatus.SUCCESS == status) {
				return value.getData();
			}
			else {
				return null;
			}
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public void putEntry(IdProof assertion, BdbEntityResource resource, byte[] data) throws IdProovingException, NotAuthorizedException {
		
		checkCreds(assertion, AuthorizationLevel.READ_WRITE);
		
		Database db = resource.db().getDb();
		
		try {
			byte[] keyData = resource.id();
			
			DatabaseEntry key = new DatabaseEntry(keyData);
			DatabaseEntry value = new DatabaseEntry(data);
			
			db.put(null, key, value);
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	public boolean deleteEntry(IdProof assertion, BdbEntityResource resource) throws IdProovingException, NotAuthorizedException {
		
		checkCreds(assertion, AuthorizationLevel.READ_WRITE);
		
		Database db = resource.db().getDb();
		
		try {
			byte[] keyData = resource.id();
			DatabaseEntry key = new DatabaseEntry(keyData);
			OperationStatus status = db.delete(null, key);
			
			if (OperationStatus.SUCCESS == status) {
				return true;
			}
			else {
				return false; // NOTFOUND
			}
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	public void clearDb(IdProof assertion, BdbDb dbResource) throws IdProovingException, NotAuthorizedException {

		checkCreds(assertion, AuthorizationLevel.READ);
		
		Transaction t = null;
		Cursor c = null;
		try {
			Database db = dbResource.getDb();
			
			t = db.getEnvironment().beginTransaction(null, null);

			CursorConfig cursorConf = new CursorConfig();
			cursorConf.setReadUncommitted(true);

			c = db.openCursor(t, cursorConf);

			DatabaseEntry key = new DatabaseEntry();
			DatabaseEntry value = new DatabaseEntry();
			value.setPartial(0, 0, true);

			while (OperationStatus.SUCCESS == c.getNext(key, value, LockMode.READ_UNCOMMITTED)) {
				c.delete();
			}
			
			c.close();
			c = null;

			t.commit();
			t = null;
		}
		catch (Exception ex) {
			
			if (c != null) {
				try {
					c.close();
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			if (t != null) {
				try {
					t.abort();
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * Permits the adapter exposing this service to the world to resolve a path
	 * to a resource on which to operate.
	 */
	public ServiceResource resolve(String resourcePath) throws ResourcePathException {
		
		if (resourcePath.trim().equals("/") || resourcePath.trim().length() == 0) {
			return new BdbMapResource();
		}
		
		List<String> elements = new ArrayList<String>(Arrays.asList(resourcePath.split("\\/")));
		Object current = map;
		
		while (!elements.isEmpty()) {
			
			String element = elements.remove(0);
			
			if (element.length() == 0) {
				continue;
			}
			
			String decodedElement;
			try {
				decodedElement = URLDecoder.decode(element, "UTF8");
			}
			catch (UnsupportedEncodingException ex) {
				throw new RuntimeException(ex);
			}
			
			if (current instanceof BdbCategory) {

				BdbCategory category = (BdbCategory)current;
				boolean found = false;
				
				for (BdbEnv e : category.environments()) {
					if (e.getName().equals(decodedElement)) {
						current = e;
						found = true;
						break;
					}
				}
				
				if (found) {
					continue;
				}
				
				for (BdbCategory c : category.categories()) {
					if (c.getName().equals(decodedElement)) {
						current = c;
						found = true;
						break;
					}
				}
				
				if (found) {
					continue;
				}
				else {
					throw new ResourcePathException("Cannot resolve path: " + resourcePath);
				}
			}
			else if (current instanceof BdbEnv) {
				
				BdbEnv env = (BdbEnv)current;
				boolean found = false;
				
				for (BdbDb d : env.databases()) {
					if (d.getName().equals(decodedElement)) {
						current = d;
						found = true;
						break;
					}
				}
				
				if (found) {
					continue;
				}
				else {
					throw new ResourcePathException("Cannot resolve path: " + resourcePath);
				}
			}
			else if (current instanceof BdbDb) {
				
				byte[] key = KeyFactory.decode(element);
				
				BdbDb db = (BdbDb)current;
				current = new BdbEntityResource(db, key);
			}
			else {
				throw new ResourcePathException("Cannot resolve path " + resourcePath);
			}
		}
		
		if (current == null) {
			throw new ResourcePathException("Cannot resolve path " + resourcePath);
		}
		
		if (!(current instanceof ServiceResource)) {
			throw new RuntimeException("Path resolves to a type that does not implement ServiceResource: " + current);
		}
		
		return (ServiceResource)current;
	}
	
	private final class HashMapVeracityCachedKeyProvider implements VeracityCachedKeyProvider<VeracityCachedKeyImpl> {
		private HashMap<String, VeracityCachedKeyImpl> cache = new HashMap<String, VeracityCachedKeyImpl>();
		public VeracityCachedKeyImpl create() {
			return new VeracityCachedKeyImpl();
		}
		public synchronized VeracityCachedKeyImpl getKey(String serviceName, byte[] t) {
			return cache.get(serviceName + HexUtil.toHex(t));
		}
		public synchronized void putKey(VeracityCachedKeyImpl key) {
			cache.put(key.getServiceName(), key);
		}
		
	}
	
	private final class VeracityCachedKeyImpl implements VeracityCachedKey {
		private byte[] data;
		private long expiration;
		private byte[] digest;
		private String serviceName;
		
		public byte[] getDigest() {
			return digest;
		}
		public void setDigest(byte[] digest) {
			this.digest = digest;
		}
		public String getServiceName() {
			return serviceName;
		}
		public void setServiceName(String serviceName) {
			this.serviceName = serviceName;
		}
		public byte[] getData() {
			return data;
		}
		public void setData(byte[] data) {
			this.data = data;
		}
		public long getExpiration() {
			return expiration;
		}
		public void setExpiration(long expiration) {
			this.expiration = expiration;
		}
	}
	
	private void checkCreds(IdProof assertion, AuthorizationLevel required) throws IdProovingException, NotAuthorizedException {
		
		IdVerifier verifier = agent.getVerifier(assertion);
		if (!verifier.verify()) {
			throw new IdProovingException("Assertion is invalid: " + assertion.toString());
		}
		
		AuthorizationLevel level = authorizer.authorize(verifier.id());

		if (level.ordinal() < required.ordinal()) {
			throw new NotAuthorizedException(required);
		}
	}
}
