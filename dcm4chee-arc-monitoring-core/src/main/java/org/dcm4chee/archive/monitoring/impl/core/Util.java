//
/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is part of dcm4che, an implementation of DICOM(TM) in
 * Java(TM), hosted at https://github.com/gunterze/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * Agfa Healthcare.
 * Portions created by the Initial Developer are Copyright (C) 2011
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * See @authors listed below
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.dcm4chee.archive.monitoring.impl.core;

import java.util.Arrays;

import org.dcm4chee.archive.monitoring.impl.core.context.MonitoringContext;

/**
 * @author Alexander Hoermandinger <alexander.hoermandinger@agfa.com>
 *
 */
public final class Util {

	private Util() {
		// NOOOP
	}
	
	public static String createPath(String name, String... names) {
		final StringBuilder builder = new StringBuilder();
		append(builder, name);
		if (names != null) {
			for (String s : names) {
				append(builder, s);
			}
		}
		return builder.toString();
	}

	public static String createPath(String... ss) {
		StringBuilder builder = new StringBuilder();
		for (String s : ss) {
			append(builder, s);
		}

		return builder.toString();
	}
	
	public static String[] createPathArray(String path) {
		return path.split(Constants.CONTEXT_DELIMITER_REGEXP);
	}

	private static void append(StringBuilder builder, String part) {
		if (part != null && !part.isEmpty()) {
			if (builder.length() > 0) {
				builder.append(Constants.CONTEXT_DELIMITER_CHAR);
			}
			builder.append(part);
		}
	}
	
	public static boolean isRootContext(MonitoringContext cxt) {
		return Arrays.equals(new String[0], cxt.getPath());
	}
	
	public static long getTimeInMinuteResolution(long timeMillis) {
	   // use truncating nature of long division to remove everything smaller than a minute (60000msec)
       return timeMillis / 60000 * 60000;
    }
	
	public static long getTimeInSecondResolution(long timeMillis) {
	    // use truncating nature of long division to remove everything smaller than a second (1000msec)
	       return timeMillis / 1000 * 1000;
	}
	
	public static String getJBossNodeName() {
        return System.getProperty(Constants.JBOSS_NODE_NAME_SYS_PROPERTY, Constants.FALLBACK_JBOSS_NODE_NAME);
    }

}
