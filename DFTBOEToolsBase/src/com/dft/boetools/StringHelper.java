package com.dft.boetools;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringHelper {
	
	// Used to parse white space separated Key Value Pairs in the form "K1=V1" "K2=V2"
	// where the double quotes surrounding key value pairs are optional unless the key or 
	// value contains white space.
	// Group 1 is quoted strings, group 2 is non quoted strings that contain = 
	// and group three is remaining non-whitespace strings. 
	private static final String OPTIONS_REGEX = "\"([^\"]*)\"|(\\S+\\s*=\\s*\\S+)|(\\S+)";
	
	/**
	 * Regex to split comma separated Strings allowing for optional white space on either side of the comma.  
	 * Example String: 
	 * X1,X2, X3 , X4	,	X5
	 */
	public static final String COMMA_SEPARATED_VALUES = "\\s*,\\s*";
	
	
	/**
	 * Pass through implementation that turns String in String[] assuming comma separated values
	 */
	public static String inClause(String values) {
		return StringHelper.inClause(values.split(COMMA_SEPARATED_VALUES));
	}
	
	/**
	 * pass through implementation that turn String[] into Iterable<String>
	 */
	public static String inClause(String[] values) {
		return StringHelper.inClause(Arrays.asList(values), true);
	}
	
	/**
	 * Pass through implementation that turns Iterable into Iterator
	 * @see #inClause(Iterator, boolean)
	 */
	public static String inClause(Iterable<?> collection, boolean useQuotes) {
		return inClause(collection.iterator(), useQuotes);
	}

	/**
	 * Creates a String that can be used as a IN Clause in a BOE Query.  Supports
	 * string type and numeric type values with the useQuotes parameter.  
	 * 
	 * Example Output: " ('A Value','Another Value', 'And another') "
	 * @param iterator the iterator over the set of values to put in the IN clause
	 * @param useQuotes if the values in the Iterator are character types then useQuotes should be true.
	 * @return a string that can be added to a query.
	 */
	public static String inClause(Iterator<?> iterator, boolean useQuotes) {
		return StringHelper.join(iterator,",","'","''",useQuotes, " (", ") ");
	}
	
	
	/**
	 * Version of join that takes a string array.
	 * @param values
	 * @param separator
	 * @return
	 */
	public static String join(String[] values, String separator) {
		return join(values, separator, false);
	}
	
	/**
	 * Version of join that takes a string array.
	 * @param values
	 * @param separator
	 * @return
	 */
	public static String join(String[] values, String separator, boolean reversedList) {
		List l = Arrays.asList(values);
		if (reversedList) Collections.reverse(l);
		return join(l.iterator(), separator);
	}
		
	/**
	 * Shortcut join for non-quoted strings
	 * @param collection
	 * @param separator
	 * @return
	 */
	public static String join (Iterator<?> iterator, String separator){
		return join(iterator, separator, null, null, false, null, null);
	}
	
	/**
	 * Code adapted from Commons-Lang to add handling of quoted values
	 * @param collection
	 * @param separator
	 * @param quoteString
	 * @param useQuotes
	 * @return
	 */
	public static String join(Iterator<?> iterator, String separator, String quoteString, String escapedQuote,  boolean useQuotes, String prefix, String postfix) {
		// handle null, zero and one elements before building a buffer
        if (iterator == null) {
	    	return null;
	    }
	    
		if (prefix == null) prefix = "";
		if (postfix == null) postfix = "";

        StringBuilder buf = new StringBuilder(256); 
        buf.append(prefix);
        
		Object first = (iterator.hasNext()) ? iterator.next() : "";
        if (first ==null) first = "";
        if (useQuotes) {
        	String value = first.toString().replace(quoteString, escapedQuote);
        	buf.append(quoteString).append(value).append(quoteString);
        } else {
        	buf.append(first);        		
        }
        
        while (iterator.hasNext()) {
            buf.append(separator);            
            Object obj = iterator.next();
            if (obj == null) obj = "";
            if (useQuotes) {
            	String value = obj.toString().replace(quoteString, escapedQuote);
            	buf.append(quoteString).append(value).append(quoteString);
            } else {
            	buf.append(obj);        		
            }
        }
        
        buf.append(postfix);
        
        return buf.toString();
    }
	
	/**
	 * Modifies the passed in Collection by parsing the values string
	 * for comma separated tokens and adding each token to the passed in 
	 * Collection.  For method chaining the Collection is also returned.
	 * @param String of "," separated fid ids
	 * @return Set<String> containing parsed fids.
	 */	
	public static <T extends Collection<String>> T parseTo(T output, String values) {
		if (values != null && values.length() > 0) {						
			output.addAll(Arrays.asList(values.split(COMMA_SEPARATED_VALUES)));
		} 
		return output;
	}
	
	public static List<String> parseTo(String values) {
		return Arrays.asList(values.split(COMMA_SEPARATED_VALUES));
	}
	
	
	/**
	 * Generic String command parsing method.  
	 * Accepts name=value style parameters and allows for spaces if name value pairs are quoted.
	 * @param options
	 * @param props
	 */
	public static void parseOptions(String options, Properties props) {
		if (options != null && options.length() > 0) {
			Matcher m = Pattern.compile(OPTIONS_REGEX).matcher(options);
			List<String> output = new ArrayList<String>();
			while (m.find()) {
				// add the first non-null group from the match.
				output.add(m.group(1) != null ? m.group(1) : // Group 1 if not null 
					m.group(2) != null ? m.group(2) : // Group 2 if not null
						m.group(3)); // otherwise group 3
			}
			
			for (String token : output) {
				if (token.contains("=")){
					String[] nameValuePair = token.split("=");
					if (nameValuePair.length >= 2) {
						props.put(nameValuePair[0].trim(),nameValuePair[1].trim());
					}
					
				} else {
					props.put(token.trim(), "");
				}				
			}
		}		
	}
	
	public static String escQteBOE(String in) {
		return in.replace("'", "''");
	}

}
