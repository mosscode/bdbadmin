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
package com.moss.bdbadmin.api.util;

import org.apache.commons.codec.binary.Base64;

import com.moss.identity.IdProof;
import com.moss.identity.veracity.VeracityId;
import com.moss.identity.veracity.VeracityIdProof;
import com.moss.identity.veracity.VeracityProfileDescription;

public class AuthenticationHeader {
	
	public static final String HEADER_NAME = "VeracityAuth";

	public static String encode(IdProof a) {
		
		VeracityIdProof assertion = (VeracityIdProof)a; // only veracity is supported at present
		
		String profileName = "";
		String profileLastModified = "";
		if (assertion.getProfileDesc() != null && assertion.getProfileDesc().getName() != null) {
			profileName = assertion.getProfileDesc().getName();
			profileLastModified = Long.toString(assertion.getProfileDesc().getWhenLastModified());
		}
		
		return new StringBuilder()
		.append(assertion.getIdentity().toString())
		.append(":")
		.append(assertion.getExpiration())
		.append(":")
		.append(new String(Base64.encodeBase64(assertion.getKeyDigest())))
		.append(":")
		.append(profileName)
		.append(":")
		.append(profileLastModified)
		.append(":")
		.append(new String(Base64.encodeBase64(assertion.getSignature())))
		.toString();
	}
	
	public static VeracityIdProof decode(String encoded) {
		String[] pieces = encoded.split(":");
		if (pieces.length != 6) {
			throw new RuntimeException("Unexpected number of segments: " + pieces.length);
		}
		
		VeracityProfileDescription desc = null;
		if (pieces[3].length() != 0) {
			desc = new VeracityProfileDescription();
			desc.setName(pieces[3]);
			desc.setWhenLastModified(Long.valueOf(pieces[4]));
		}
		
		VeracityIdProof assertion = new VeracityIdProof();
		assertion.setIdentity(new VeracityId(pieces[0]));
		assertion.setExpiration(Long.valueOf(pieces[1]));
		assertion.setKeyDigest(Base64.decodeBase64(pieces[2].getBytes()));
		assertion.setProfileDesc(desc);
		assertion.setSignature(Base64.decodeBase64(pieces[5].getBytes()));
		
		return assertion;
	}
}
