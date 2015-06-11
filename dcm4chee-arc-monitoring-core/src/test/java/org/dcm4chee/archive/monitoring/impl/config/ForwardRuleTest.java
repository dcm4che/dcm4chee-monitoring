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

import java.util.Iterator;
import java.util.List;






import org.junit.Assert;
import org.junit.Test;

/**
 * @author Alexander Hoermandinger <alexander.hoermandinger@agfa.com>
 *
 */
public class ForwardRuleTest {
	
	@Test
	public void testTrivialForwardRule() {
		ForwardRule rule = new ForwardRule();
		rule.setSourcePathPattern("node1", "*");
		rule.addTargetPathTemplate("cluster", "nodes");
		
		List<String[]> targets = rule.getTargets("node1", "1");
		Assert.assertEquals(1, targets.size());
		Assert.assertArrayEquals(new String[] {"cluster", "nodes"}, targets.iterator().next());
	}
	
	@Test
	public void testTrivialForwardRule2() {
		ForwardRule rule = new ForwardRule();
		rule.setSourcePathPattern("node1", "*");
		rule.addTargetPathTemplate("cluster1", "nodes");
		rule.addTargetPathTemplate("cluster2", "nodes");
		
		List<String[]> targets = rule.getTargets("node1", "1");
		Assert.assertEquals(2, targets.size());
		Iterator<String[]> it = targets.iterator();
		Assert.assertArrayEquals(new String[] {"cluster1", "nodes"}, it.next());
		Assert.assertArrayEquals(new String[] {"cluster2", "nodes"}, it.next());
	}
	
	@Test
	public void testForwardRuleWithVariables() {
		ForwardRule rule = new ForwardRule();
		rule.setSourcePathPattern("node1", "<(\\d)>:number1");
		rule.addTargetPathTemplate("cluster1", "nodes", "$number1");
		rule.addTargetPathTemplate("cluster2", "nodes", "$number1");
		
		List<String[]> targets = rule.getTargets("node1", "1");
		Assert.assertEquals(2, targets.size());
		Iterator<String[]> it = targets.iterator();
		Assert.assertArrayEquals(new String[] {"cluster1", "nodes", "1"}, it.next());
		Assert.assertArrayEquals(new String[] {"cluster2", "nodes", "1"}, it.next());
	}
	
	@Test
	public void testForwardRuleWithVariables2() {
		ForwardRule rule = new ForwardRule();
		rule.setSourcePathPattern("**","db","connection","statement","<\\d>");
		rule.addTargetPathTemplate("db", "connection");
		
		List<String[]> targets = rule.getTargets("dicom", "service", "cstore", "db", "connection", "statement", "1");
		Assert.assertEquals(1, targets.size());
		Iterator<String[]> it = targets.iterator();
		Assert.assertArrayEquals(new String[] {"db", "connection"}, it.next());
	}
	
	@Test
	public void testForwardRuleWithVariables3() {
		ForwardRule rule = new ForwardRule();
		rule.setSourcePathPattern("**","db","connection","statement","<\\d>:statement_nr");
		rule.addTargetPathTemplate("db", "connection", "$statement_nr");
		
		List<String[]> targets = rule.getTargets("dicom", "service", "cstore", "db", "connection", "statement", "1");
		Assert.assertEquals(1, targets.size());
		Iterator<String[]> it = targets.iterator();
		Assert.assertArrayEquals(new String[] {"db", "connection", "1"}, it.next());
	}
	
	@Test
	public void testForwardRuleWithVariables4() {
		ForwardRule rule = new ForwardRule();
		rule.setSourcePathPattern("**","connection", "*", "statement","*");
		rule.addTargetPathTemplate("db", "connection");
		
		List<String[]> targets = rule.getTargets("undefined", "connection", "1234", "statement", "18978394");
		Assert.assertEquals(1, targets.size());
		Iterator<String[]> it = targets.iterator();
		Assert.assertArrayEquals(new String[] {"db", "connection"}, it.next());
	}
	
	@Test
    public void testForwardRuleWithVariables5() {
	    VariableResolver resolver = new VariableResolver() {
            
            @Override
            public String getVariable(String name) {
                return ("node".equals(name)) ? "clusterNode1" : null;
            }
        };
	    
        ForwardRule rule = new ForwardRule();
        rule.setSourcePathPattern("**","connection", "*", "statement","*");
        rule.addTargetPathTemplate("$node", "db", "connection");
        rule.addVariableResolver(resolver);
        
        List<String[]> targets = rule.getTargets("undefined", "connection", "1234", "statement", "18978394");
        Assert.assertEquals(1, targets.size());
        Iterator<String[]> it = targets.iterator();
        Assert.assertArrayEquals(new String[] {"clusterNode1", "db", "connection"}, it.next());
    }
	
	
}
