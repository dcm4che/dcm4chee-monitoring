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

package org.dcm4chee.archive.monitoring.impl.config.json;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import org.codehaus.jackson.map.ObjectMapper;
import org.dcm4chee.archive.monitoring.impl.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Alexander Hoermandinger <alexander.hoermandinger@agfa.com>
 */
public class JsonMonitoringConfigurationProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonMonitoringConfigurationProvider.class);
    
    public Configuration createConfiguration(String jsonConfigFileName) {
        File jsonCfgFile = new File(jsonConfigFileName);
        return createConfiguration(jsonCfgFile);
    }
    
    public Configuration createConfiguration(Path jsonCfgPath) {
        return createConfiguration(jsonCfgPath.toFile());
    }
    
    public Configuration createConfiguration(File jsonCfgFile) {
        try(FileInputStream fin = new FileInputStream(jsonCfgFile)) {
            return createConfiguration(fin);
        } catch (IOException e) {
            LOGGER.error("Error while reading configuration file {}", jsonCfgFile.getAbsolutePath(), e);
            return null;
        }
    }
    
    public Configuration createConfiguration(InputStream inputStream) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            Configuration cfg = mapper.readValue(inputStream, Configuration.class);
            return cfg;
        } catch (IOException e) {
            throw new RuntimeException("I/O error while reading monitoring configuration", e);
        }
    }
    
    public Configuration createConfigurationFromClasspath(ClassLoader classloader, String cfgFileName) {
        InputStream inputStream = classloader.getResourceAsStream(cfgFileName);
        if(inputStream == null) {
            return null;
        }
        
        try {
            return createConfiguration(inputStream);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                LOGGER.error("Error while trying to close input stream", e);
            }
        }
    }
    
}
