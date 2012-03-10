package se.j4j.argumentparser.utils;

import static java.security.AccessController.doPrivileged;

import java.security.PrivilegedAction;

public final class Lines
{
	private Lines(){};

	/**
	 * Contains the line.separator property string
	 */
	public static final String NEWLINE = doPrivileged(new PrivilegedAction<String>(){
		@Override public String run(){ return System.getProperty("line.separator");}});
}
