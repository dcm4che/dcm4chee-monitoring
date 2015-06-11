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

package org.dcm4chee.archive.monitoring.impl.core.context;

class FilterResultImpl implements MonitoringContextFilter.FilterResult {
	private final boolean matches;
	private final boolean ignoreChildContexts;
	
	FilterResultImpl(boolean matches, boolean ignoreChildContexts) {
		this.matches = matches;
		this.ignoreChildContexts = ignoreChildContexts;
	}
	
	@Override
	public boolean matches() {
		return matches;
	}

	@Override
	public boolean ignoreChildContexts() {
		return ignoreChildContexts;
	}
	
	private static final FilterResultImpl NO_MATCH_AND_NOT_IGNORE_CHILDS = new FilterResultImpl(false, false);
	private static final FilterResultImpl NO_MATCH_AND_IGNORE_CHILDS = new FilterResultImpl(false, true);
	private static final FilterResultImpl MATCH_AND_NOT_IGNORE_CHILDS = new FilterResultImpl(true, false);
	
	public static FilterResultImpl getNoMatchAndNotIgnoreChildContextFilter() {
		return NO_MATCH_AND_NOT_IGNORE_CHILDS;
	}
	
	public static FilterResultImpl getNoMatchAndIgnoreChildContextFilter() {
		return NO_MATCH_AND_IGNORE_CHILDS;
	}
	
	public static FilterResultImpl getMatchAndNotIgnoreChildContextFilter() {
		return MATCH_AND_NOT_IGNORE_CHILDS;
	}
	
}