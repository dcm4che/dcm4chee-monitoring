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


import org.junit.Assert;
import org.junit.Test;

/**
 * @author Alexander Hoermandinger <alexander.hoermandinger@agfa.com>
 *
 */
public class MonitoringContextNodeTest {

	@Test
	public void testContextRetrieval() {
		MonitoringContext rootContext = new MonitoringContextTree(null).getRoot();
		MonitoringContext serviceNode = rootContext.getOrCreateContext("service");
		Assert.assertArrayEquals(new String[] { "service" }, serviceNode.getPath());
	}
	
	@Test
	public void testContextRetrieval2() {
		MonitoringContext rootContext = new MonitoringContextTree(null).getRoot();
		MonitoringContext serviceNode = rootContext.getOrCreateContext("service");
		MonitoringContext subServiceNode1 = serviceNode.getOrCreateContext("sub1");
		Assert.assertArrayEquals(new String[] { "service", "sub1" }, subServiceNode1.getPath());
	}
	
	@Test
	public void testContextRetrieval3() {
		MonitoringContext rootContext = new MonitoringContextTree(null).getRoot();
		MonitoringContext subServiceNode1 = rootContext.getOrCreateContext("service", "sub1");
		Assert.assertArrayEquals(new String[] { "service", "sub1" }, subServiceNode1.getPath());
	}
	
	@Test
	public void testContextRetrieval4() {
		MonitoringContext rootContext = new MonitoringContextTree(null).getRoot();
		MonitoringContext subServiceNode1 = rootContext.getOrCreateContext("service", "sub1");
		MonitoringContext subServiceNode2 = rootContext.getOrCreateContext("service").getOrCreateContext("sub1");
		Assert.assertSame(subServiceNode1, subServiceNode2);
	}
	
	@Test
	public void testContextRetrieval5() {
		MonitoringContext rootContext = new MonitoringContextTree(null).getRoot();
		MonitoringContext level3Node1 = rootContext.getOrCreateContext("level1", "level2", "level3");
		MonitoringContext level3Node2 = rootContext.getOrCreateContext("level1").getOrCreateContext("level2", "level3");
		Assert.assertSame(level3Node1, level3Node2);
	}
	
	@Test
	public void testSetEnableIsPropagated() {
	    MonitoringContext rootContext = new MonitoringContextTree(null).getRoot();
	    
	    MonitoringContext level1 = rootContext.getOrCreateContext("level1");
	    Assert.assertTrue(level1.isEnabled());
	    
	    MonitoringContext level3 = rootContext.getOrCreateContext("level1", "level2", "level3");
	    Assert.assertTrue(level3.isEnabled());
	    
	    level1.setEnabled(false);
	    Assert.assertFalse(level1.isEnabled());
	    Assert.assertFalse(level3.isEnabled());
	}
	
	@Test
    public void testSetEnablePropagationDoesNotOverrideSet() {
        MonitoringContext rootContext = new MonitoringContextTree(null).getRoot();
        
        MonitoringContext level1 = rootContext.getOrCreateContext("level1");
        Assert.assertTrue(level1.isEnabled());
        
        MonitoringContext level2 = rootContext.getOrCreateContext("level1", "level2");
        Assert.assertTrue(level2.isEnabled());
        
        MonitoringContext level3 = rootContext.getOrCreateContext("level1", "level2", "level3");
        // explicitly enable level3
        level3.setEnabled(true);
        Assert.assertTrue(level3.isEnabled());
        
        level1.setEnabled(false);
        Assert.assertFalse(level1.isEnabled());
        Assert.assertFalse(level2.isEnabled());
        // level3 is still enabled as it was explicitly set
        Assert.assertTrue(level3.isEnabled());
    }
	
	@Test
    public void testGlobalEnable() {
	    MonitoringContextTree tree = new MonitoringContextTree(null);
        MonitoringContext rootContext = tree.getRoot();
        
        MonitoringContext level1 = rootContext.getOrCreateContext("level1");
        Assert.assertTrue(level1.isEnabled());
        
        MonitoringContext level3 = rootContext.getOrCreateContext("level1", "level2", "level3");
        level3.setEnabled(true);
        Assert.assertTrue(level3.isEnabled());
        
        tree.setGlobalEnabled(false);
        Assert.assertFalse(level1.isEnabled());
        Assert.assertFalse(level3.isEnabled());
    }
	
	
}
