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

package org.dcm4chee.archive.monitoring.impl.config;


import org.junit.Assert;
import org.junit.Test;

/**
 * @author Alexander Hoermandinger <alexander.hoermandinger@agfa.com>
 *
 */
public class PathMatcherTest {
	
	@Test
	public void testTrivialMatch1() {
		String[] path = new String[] { "node1" };
		PathMatcher pm = new PathMatcher("node1");
		Assert.assertTrue(pm.match(path).isMatch());
	}
	
	@Test
	public void testTrivialMatch2() {
		String[] path = new String[] { "node1", "service" };
		PathMatcher pm = new PathMatcher("node1", "service");
		Assert.assertTrue(pm.match(path).isMatch());
	}
	
	@Test
	public void testTrivialNoMatch1() {
		String[] path = new String[] { "node1", "service" };
		PathMatcher pm = new PathMatcher("node2", "service");
		Assert.assertFalse(pm.match(path).isMatch());
	}
	
	@Test
	public void testTrivialNoMatch2() {
		String[] path = new String[] { "node1", "service", "cstore" };
		PathMatcher pm = new PathMatcher("node1", "service");
		Assert.assertFalse(pm.match(path).isMatch());
	}
	
	@Test
	public void testWildcardMatch() {
		String[] path = new String[] { "node1", "service" };
		// null corresponds to wildcard
		PathMatcher pm = new PathMatcher("*", "service");
		Assert.assertTrue(pm.match(path).isMatch());
	}
	
	@Test
	public void testMultiWildcardMatch() {
		String[] path = new String[] { "node1", "service", "db", "statement" };
		// null corresponds to wildcard
		PathMatcher pm = new PathMatcher("**", "db", "statement");
		Assert.assertTrue(pm.match(path).isMatch());
	}
	
	@Test
	public void testMultiWildcardMatch2() {
		String[] path = new String[] { "node1", "service", "db", "statement" };
		// null corresponds to wildcard
		PathMatcher pm = new PathMatcher("**", "db", "statement", "**");
		Assert.assertTrue(pm.match(path).isMatch());
	}
	
	@Test
	public void testMultiWildcardMatch3() {
		String[] path = new String[] { "node1", "service", "db", "statement" };
		// null corresponds to wildcard
		PathMatcher pm = new PathMatcher("node1", "service", "**");
		Assert.assertTrue(pm.match(path).isMatch());
	}
	
	@Test
	public void testWildcardMatch2() {
		String[] path = new String[] { "node1", "service" };
		// null corresponds to wildcard
		PathMatcher pm = new PathMatcher("*", "*");
		Assert.assertTrue(pm.match(path).isMatch());
	}
	
	@Test
	public void testRegexpMatch() {
		String[] path = new String[] { "node1", "service" };
		PathMatcher pm = new PathMatcher("<node[\\d]+>", "service");
		Assert.assertTrue(pm.match(path).isMatch());
	}
	
	@Test
	public void testRegexpMatch2() {
		String[] path = new String[] { "node1", "service" };
		PathMatcher pm = new PathMatcher("<(?<var1>node1)>", "service");
		Assert.assertTrue(pm.match(path).isMatch());
	}
	
	@Test
	public void testRegexpMatchAndVariableMapping() {
		String[] path = new String[] { "node1", "service" };
		PathMatcher pm = new PathMatcher("<node1>:var1", "service");
		PathMatcher.Result matchResult = pm.match(path);
		Assert.assertTrue(matchResult.isMatch());
		Assert.assertEquals("node1", matchResult.getVariable("var1"));
	}
	
	@Test
	public void testRegexpMatchAndVariableMapping2() {
		String[] path = new String[] { "node1", "service" };
		PathMatcher pm = new PathMatcher("<node(1)>:var1", "service");
		PathMatcher.Result matchResult = pm.match(path);
		Assert.assertTrue(matchResult.isMatch());
		Assert.assertEquals("1", matchResult.getVariable("var1"));
	}
	
}
