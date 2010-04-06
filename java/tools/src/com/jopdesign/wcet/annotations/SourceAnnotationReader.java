/*
  This file is part of JOP, the Java Optimized Processor
    see <http://www.jopdesign.com/>

  Copyright (C) 2006-2008, Martin Schoeberl (martin@jopdesign.com)
  Copyright (C) 2006, Rasmus Ulslev Pedersen
  Copyright (C) 2008, Benedikt Huber (benedikt.huber@gmail.com)

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.jopdesign.wcet.annotations;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.jopdesign.build.ClassInfo;
import com.jopdesign.wcet.Project;

/**
 * Parsing source annotations for WCET analysis.
 * 
 * @author Benedikt Huber (benedikt.huber@gmail.com)
 * @author Martin Schoeberl (martin@jopdesign.com)
 * @author Rasmus Ulslev Pedersen
 */
public class SourceAnnotationReader {
	private static final Logger logger = Logger.getLogger(SourceAnnotationReader.class);
	
	private Project project;

	public SourceAnnotationReader(Project p) {
		this.project = p;
	}

	/**
	 * Extract loop bound annotations for one class
	 * 
	 * @return a FlowFacts object encapsulating the annotations found in the source file of the given class
	 * @throws IOException 
	 * @throws BadAnnotationException 
	 * 
	 */
	public SourceAnnotations readAnnotations(ClassInfo ci) 
		throws IOException, BadAnnotationException {
		
		SourceAnnotations flowFacts = new SourceAnnotations();
		File fileName = getSourceFile(ci);
		BufferedReader reader = new BufferedReader(new FileReader(fileName));
		String line = null;
		int lineNr = 1;

		while ((line = reader.readLine()) != null) {
			LoopBound loopBound = SourceAnnotationReader.extractAnnotation(line);	
			if (loopBound != null) {
				flowFacts.addLoopBound(lineNr,loopBound);
			}
			lineNr++;
		}
		logger.debug("Read WCA annotations for "+fileName);
		return flowFacts;
	}

	private File getSourceFile(ClassInfo ci) throws FileNotFoundException, BadAnnotationException {
		
		File src = project.getSourceFile(ci);
		try {
			File klazz = project.getClassFile(ci);
			if(src.lastModified() > klazz.lastModified()) {
				throw new BadAnnotationException("Timestamp error: source file modified after compilation");
			}
		} catch(IOException ex) {
			Project.logger.warn("Could not find class file for " + ci);
		}
		return src;
	}

	/**
	 * Return the loop bound for a java source lines. 
	 * <p>Loop bounds are tagged with one of the following annotations: 
	 * <ul> 
	 *  <li/>{@code @WCA loop &lt;?= [0-9]+}
	 *  <li/>{@code @WCA [0-9]+ &lt;= loop &lt;= [0-9]+}
	 * </ul>
	 * e.g. {@code // @WCA loop = 100} or {@code \@WCA 50 &lt;= loop &lt;= 60}.
	 * 
	 * @param line a java source code line (possibly annotated)
	 * @return the loop bound limit or null if no annotation was found
	 * @throws BadAnnotationException if the loop bound annotation has syntax errors or is
	 * invalid
	 */
	public static LoopBound extractAnnotation(String wcaA)
		throws BadAnnotationException {

		int ai = wcaA.indexOf("@WCA");
		if(ai != -1 ){

			String annotString = wcaA.substring(ai+"@WCA".length());
			if(annotString.indexOf("loop") < 0) return null;
			
			Pattern pattern1 = Pattern.compile(" *loop *(<?=) *([0-9]+) *");
			Pattern pattern2 = Pattern.compile(" *([0-9]+) *<= *loop *<= *([0-9]+) *");
			Matcher matcher1 = pattern1.matcher(annotString);
			if(matcher1.matches()) {				
				int ub = Integer.parseInt(matcher1.group(2));
				int lb = (matcher1.group(1).equals("=")) ? ub : 0;
				return new LoopBound(lb,ub);		
			}
			Matcher matcher2 = pattern2.matcher(annotString);
			if(matcher2.matches()) {
				int lb = Integer.parseInt(matcher2.group(1));
				int ub = Integer.parseInt(matcher2.group(2));
				return new LoopBound(lb,ub);		
			}
			throw new BadAnnotationException("Syntax error in loop bound annotation: "+annotString);
		}
		return null;
	}

}
