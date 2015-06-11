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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Alexander Hoermandinger <alexander.hoermandinger@agfa.com>
 *
 */
public class PathMatcher {
    private static final String GLOBAL_WILDCARD = "**";
    private static final String WILDCARD = "*";
    private static final char REGEXP_START = '<';
    
	private final ParsedPatternSegment[] parsedPattern;
	
	public PathMatcher(String... pattern) {
		this.parsedPattern = new ParsedPatternSegment[pattern.length];
		for(int i = 0; i < pattern.length; i++) {
		    parsedPattern[i] = parseInputPattern(pattern[i]);
		}
	}
	
	public Result match(String... path) {
		Map<String,String> varMappings = new HashMap<>();
		
		int initialPathPos = 0;
		ParsedPatternSegment patternSeg = parsedPattern[0];
		
		int patternPos = 0; 
		// wildcard at beginning of pattern
		if ( GLOBAL_WILDCARD.equals(patternSeg.exp)) {
			patternPos++;
			patternSeg = parsedPattern[patternPos];
			initialPathPos = findMatch(patternSeg, path, 0, path.length, varMappings);
			if ( initialPathPos == -1) {
				return Result.FALSE;
			}
		}
		
		for (int pathPos = initialPathPos; pathPos < path.length; pathPos++, patternPos++) {
			if ( patternPos >= parsedPattern.length) {
				return Result.FALSE;
			}
			
			patternSeg = parsedPattern[patternPos];
			
			// wildcard at end of pattern -> success
			if ( GLOBAL_WILDCARD.equals(patternSeg.exp)) {
				break;
			}
			
 			if (!match(patternSeg, path[pathPos], varMappings)) {
				return Result.FALSE;
			}
		}

		return new Result(true, varMappings);
	}
	
	private int findMatch(ParsedPatternSegment patternSegment, String[] path, int start, int end, Map<String, String> varMappings) {
		for (int i = start; i < end; i++) {
			if (match(patternSegment, path[i], varMappings)) {
				return i;
			}
		}

		return -1;
	}
	
	private boolean match(ParsedPatternSegment patternSegment, String pathSegment,Map<String,String> varMappings) {
		if (WILDCARD.equals(patternSegment.exp)) {
			return true;
		}
		
		if ( !patternSegment.isRegexp ) {
			if(!patternSegment.exp.equals(pathSegment)) {
				return false;
			}
		} else {
			Matcher m = patternSegment.regexpPattern.matcher(pathSegment);
			if (!m.matches()) {
				return false;
			}
			String varName = patternSegment.varName;
			if(varName != null) {
				/*
				 * if the user specified a grouping in the regexp
				 * -> put the latest group found into the variable
				 * 
				 * if no grouping is specified 
				 * -> no problem as group(0) is the entire matched pattern
				 */
				String varValue = m.group(m.groupCount());
				varMappings.put(varName, varValue);
			}
		}
		return true;
	}
	
	/*
	 * Parses input pattern where valid patterns are
	 * 1) default input pattern: path:variableName
	 * 2) regular expression input pattern: <regexp>:variableName
	 * In both cases the variableName is optional 
	 */
	private static ParsedPatternSegment parseInputPattern(String pattern) {
        String[] parsed = pattern.split(":");
        String exp = parsed[0];
        
        boolean isRegexp = false;
        Pattern regexp = null;
        if(exp.charAt(0) == REGEXP_START) {
            isRegexp = true;
            exp = exp.substring(1, exp.length() - 1);
            regexp = Pattern.compile(exp);
        }
        
        switch (parsed.length) {
        case 1:
            return new ParsedPatternSegment(isRegexp, regexp, exp, null);
        case 2:
            return new ParsedPatternSegment(isRegexp, regexp, exp, parsed[1]);
        default:
            throw new IllegalArgumentException("Invalid path pattern: " + pattern);
        }
    }
	
	/**
	 * Represents a parsed segment of the pattern
	 * If the pattern contains a regular expression then the "<"">" tags are stripped
	 * from the expression;
	 * @author AXJRD
	 *
	 */
	private final static class ParsedPatternSegment {
	    private final boolean isRegexp;
	    private final Pattern regexpPattern;
	    private final String exp;
	    private final String varName;
	    
        private ParsedPatternSegment(boolean regexp, Pattern regexpPattern, String exp, String varName) {
            this.isRegexp = regexp;
            this.regexpPattern = regexpPattern;
            this.exp = exp;
            this.varName = varName;
        }
	    
	}
	
	public static class Result implements VariableResolver {
		private static final Result FALSE = new Result(false, null);
		
		private final boolean match;
		private final Map<String,String> varMappings;
		
		public Result(boolean match, Map<String,String> varMappings) {
			this.match = match;
			this.varMappings = varMappings == null ? Collections.<String,String>emptyMap() : varMappings;
		}
		
		public boolean isMatch() {
			return match;
		}
		
		@Override
		public String getVariable(String name) {
			return varMappings.get(name);
		}
		
	}
}
