/*
 * Copyright 1999–2025 ViaOA (info@viaoa.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.viaoa.template;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import com.viaoa.hub.Hub;
import com.viaoa.model.oa.VString;
import com.viaoa.object.*;
import com.viaoa.util.*;

/**
 * A lightweight, high-performance template engine used throughout OA for
 * generating dynamic strings, HTML fragments, and code-generation output.
 *
 * <p>
 * OATemplate processes a template string that contains zero or more variable
 * placeholders of the form <code>${name}</code> (configurable start/end tokens).
 * During evaluation, each placeholder is resolved using either a callback
 * interface or a supplied map of variable values. The resulting output is 
 * generated in a single forward pass with minimal allocations, making it
 * suitable for large templates and repeated use in server-side rendering
 * (OA-Web), OABuilder code generation, and dynamic runtime substitution.
 * </p>
 *
 * <p>
 * The template engine itself performs only simple index scanning and substring
 * slicing. It does <b>not</b> use reflection internally. However, many callers
 * — notably OA-Web and OAPropertyPath-based evaluations — may use reflection
 * when resolving template variables. This allows template placeholders to
 * reference dynamic object graph values (e.g. <code>${customer.address.city}</code>)
 * while keeping the template parser extremely lightweight.
 * </p>
 *
 * <ul>
 *   <li><b>Fast:</b> no regex and no recursive parsing.</li>
 *   <li><b>Deterministic:</b> parsed strictly left-to-right.</li>
 *   <li><b>Flexible:</b> variable resolution is caller-defined.</li>
 *   <li><b>Low overhead:</b> designed for high-throughput scenarios.</li>
 *   <li><b>Integrates cleanly with OAPropertyPath:</b> callers can resolve
 *       variables via reflection-based property path evaluation.</li>
 * </ul>
 *
 * <p>
 * Example:
 * </p>
 *
 * <pre>
 *  OATemplate t = new OATemplate("Hello ${name}, today is ${day}");
 *  String s = t.process((var) -> {
 *      if ("name".equals(var)) return "Vince";
 *      if ("day".equals(var)) return "Friday";
 *      return null;
 *  });
 *  // Result: "Hello Vince, today is Friday"
 * </pre>
 *
 * <p>
 * This class is intentionally self-contained and forms the core of OA's 
 * template processing pipeline. It is used extensively by OA-Web to merge 
 * HTML templates with object-graph state, and by OABuilder to generate 
 * boilerplate code from metadata. Reflection-based evaluation is delegated 
 * to callers, allowing the template engine to remain lean and efficient.
 * </p>
 */
public class OATemplate<F extends OAObject> {
	private static Logger LOG = Logger.getLogger(OATemplate.class.getName());
	
	/**
	 * Internal map of template-level properties referenced using the
	 * <%= $name %> syntax. These override or supplement values supplied
	 * through {@link OAProperties}.
	 */
	private Properties propInternal;
	
	/**
	 * Root of the parsed template tree representing all literal text and
	 * template tokens. Lazily created on first call to {@link #process(...)}.
	 */
	private TreeNode rootTreeNode;
	
	/**
	 * Raw template text supplied by callers. Parsed into a tree structure
	 * when processing begins.
	 */
	private String template;
	
	/**
	 * Counter incremented whenever {@link #stopProcessing()} is invoked.
	 * Used to cancel in-progress template generation.
	 */
	private final AtomicInteger aiStopCalled = new AtomicInteger();
	
	/**
	 * Optional output-text transformation settings:
	 * <ul>
	 *   <li>{@code fromText} and {@code toText} — used for string substitution
	 *       in {@link #getOutputText(String)}.</li>
	 *   <li>{@code hiliteText} — optional highlight directive applied using
	 *       {@link OAString#hilite(String, String)}.</li>
	 * </ul>
	 */
	protected String fromText, toText, hiliteText;
	
	/**
	 * Counter for tracking template-parsing errors. Limits warning output to
	 * avoid excessive logging.
	 */
	private int parseErrorCnt = 0;

	/**
	 * Creates an empty template instance. Call {@link #setTemplate(String)}
	 * before processing.
	 */
	public OATemplate() {
	}

	/**
	 * Creates a template instance initialized with the supplied template text.
	 *
	 * @param htmlTemplate the full template content to parse and evaluate
	 */
	public OATemplate(String htmlTemplate) {
		setTemplate(htmlTemplate);
	}

	/**
	 * Assigns the template text to use for processing. Resets parse state so
	 * that the template will be re-parsed on next evaluation.
	 *
	 * @param temp the raw template string
	 */
	public void setTemplate(String temp) {
		this.template = temp;
		this.rootTreeNode = null;
		this.parseErrorCnt = 0;
	}

	/**
	 * Returns the raw template text assigned to this instance.
	 *
	 * @return template string, or null if not set
	 */
	public String getTemplate() {
		return this.template;
	}

	/**
	 * Processes the template with no root objects, hub, or external properties.
	 *
	 * @return rendered template output
	 */
	public String process() {
		String s = process(null, null, null, null);
		return s;
	}

	/**
	 * Processes the template using the supplied root object.
	 *
	 * @param objRoot primary root object for property evaluation
	 * @return rendered template output
	 */
	public String process(F objRoot) {
		String s = process(objRoot, null, null, null);
		return s;
	}

	/**
	 * Processes the template with two possible root objects. Selection is made
	 * internally based on template property-path evaluation.
	 *
	 * @param objRoot1 first root object
	 * @param objRoot2 second root object
	 * @return rendered template output
	 */
	public String process(F objRoot1, F objRoot2) {
		String s = process(objRoot1, objRoot2, null, null);
		return s;
	}

	/**
	 * Processes the template with two root objects and an external properties
	 * map used to resolve <%= $var %> values.
	 *
	 * @param objRoot1 first root object
	 * @param objRoot2 second root object
	 * @param props    external property map
	 * @return rendered output
	 */
	public String process(F objRoot1, F objRoot2, OAProperties props) {
		String s = process(objRoot1, objRoot2, null, props);
		return s;
	}

	/**
	 * Processes the template using one root object and an external property map.
	 *
	 * @param objRoot main root object
	 * @param props   external properties for variable substitution
	 * @return rendered output
	 */
	public String process(F objRoot, OAProperties props) {
		String s = process(objRoot, null, null, props);
		return s;
	}

	/**
	 * Processes the template with a hub as the primary data source, along with
	 * optional external property values.
	 *
	 * @param hub   hub used for foreach operations
	 * @param props external properties
	 * @return rendered output
	 */
	public String process(Hub<F> hub, OAProperties props) {
		String s = process(null, null, hub, props);
		return s;
	}

	/**
	 * Processes the template using the supplied hub without external properties.
	 *
	 * @param hub hub used for foreach operations
	 * @return rendered output
	 */
	public String process(Hub<F> hub) {
		String s = process(null, null, hub, null);
		return s;
	}

	/*
	 * qqqqqqq TODO:
	 * protected final ArrayList<String> alDependentProperties = new ArrayList<>();
	 * public String[] getDependentProperties() {
	 * 	if (alDependentProperties.size() == 0) parse ...?? String[] ss = new String[alDependentProperties.size()];
	 * alDependentProperties.toArray(ss); return ss; }
	 */

	/**
	 * Requests cancellation of any active template processing operation. Causes
	 * {@link #generate(...)} to stop early.
	 */
	public void stopProcessing() {
		aiStopCalled.incrementAndGet();
	}

	/**
	 * Tracks which of the two root object classes was selected during template
	 * evaluation so that subsequent calls reuse the same decision.
	 */
	private Class classChoosen;
	
	/**
	 * Stores a sampled property path extracted during parsing. Used to determine
	 * which of the two root objects should be used during evaluation.
	 */
	private String ppSample;

	/**
	 * Delegates to the main process method that accepts two root objects, a hub,
	 * and external properties.
	 *
	 * @param objRoot1 root object used for evaluation
	 * @param hubRoot hub supplying data for foreach processing
	 * @param props external properties for $name substitution
	 * @return rendered template output
	 */
	public String process(F objRoot1, Hub<F> hubRoot, OAProperties props) {
		return process(objRoot1, null, hubRoot, props);
	}

	/**
	 * Core template evaluation method that selects the correct root object,
	 * ensures the template is parsed, initializes standard date/time properties,
	 * and then delegates to the generate routine.
	 *
	 * @param objRoot1 first candidate root object
	 * @param objRoot2 second candidate root object
	 * @param hubRoot hub used for foreach evaluation
	 * @param props external property values
	 * @return rendered output, or "cancelled" if stopped
	 */
	public String process(F objRoot1, F objRoot2, Hub<F> hubRoot, OAProperties props) {
		final int cntStopCalled = aiStopCalled.get();
		this.parseErrorCnt = 0;

		setProperty("DATETIME", new OADateTime());
		setProperty("DATE", new OADate());
		setProperty("TIME", new OATime());

		if (rootTreeNode == null) {
			rootTreeNode = createTree(template);
			
			if (template.toLowerCase().indexOf("<html") >= 0) {
    			// check for case where foreach has it's own table row
    	        removeRowTags(rootTreeNode);
			}
		}

		// need to find out which object to use
		OAObject obj;
		if (objRoot1 == objRoot2) {
			obj = objRoot1;
		} else if (objRoot2 == null) {
			obj = objRoot1;
		} else if (objRoot1 == null) {
			obj = objRoot2;
		} else if (classChoosen != null) {
			if (objRoot1.getClass().equals(classChoosen)) {
				obj = objRoot1;
			} else {
				obj = objRoot2;
			}
		} else {
			// both are != null, need to know which one is needed by the template's properyPath(s)
			if (ppSample == null) {
				obj = objRoot1;
			} else {
				try {
					OAPropertyPath pp = new OAPropertyPath<>(objRoot1.getClass(), ppSample);
					obj = objRoot1;
					classChoosen = objRoot1.getClass();
				} catch (Exception e) {
					obj = objRoot2;
					classChoosen = objRoot2.getClass();
				}
			}
		}

		StringBuilder sb = new StringBuilder(1024 * 4);
		boolean b = generate(rootTreeNode, obj, hubRoot, sb, props, cntStopCalled);
		if (!b) {
			return "cancelled";
		}
		String s = new String(sb);
		sb = null;
		return s;
	}

	/**
	 * Sets or removes an internal template variable referenced using the
	 * $name placeholder. Leading '$' is removed if present.
	 *
	 * @param name property name
	 * @param value value to assign, or null to remove the property
	 */
	public void setProperty(String name, Object value) {
		if (name == null) {
			return;
		}
		if (name.startsWith("$")) {
			name = name.substring(1);
		}

		if (propInternal == null) {
			propInternal = new Properties();
		}
		if (value == null) {
			propInternal.remove(name);
		} else {
			propInternal.put(name, value);
		}
	}

	/**
	 * Parses the template text into a tree structure by preprocessing the text,
	 * tokenizing it, and recursively building TreeNode instances.
	 *
	 * @param doc raw template text
	 * @return root of the parsed tree
	 */
	protected TreeNode createTree(String doc) {
		//qqqq   alDependentProperties.clear();
		if (doc == null) {
			doc = "";
		}
		TreeNode root = new TreeNode();
		String html = preprocess(doc);
		if (html.indexOf("&lt;%=") >= 0) {
			html = OAString.convert(html, "&lt;%=", "<%=");
			html = OAString.convert(html, "%&gt;", "%>");
		}
		alToken = parseTokens(html);
		posToken = 0;
		parse(root);
		
		return root;
	}

	/**
	 * Recursively trims HTML row tags surrounding foreach blocks by delegating
	 * to helper methods that adjust sibling node text.
	 *
	 * @param treeNode node to process
	 */
	protected void removeRowTags(TreeNode treeNode) {
	    if (treeNode == null) return;
	    
        final int x = treeNode.alChildren.size();
	    
	    for (int i=0; i<x; i++) {
	        final TreeNode tn = treeNode.alChildren.get(i);
	        
	        if (tn.tagType == TagType.ForEach) {
	            _removeRowTags(treeNode, i);
	        }
	        removeRowTags(tn);
        }
	}

	
	/**
	 * Removes HTML table row markup around the foreach node located at the given
	 * child index within its parent node.
	 *
	 * @param treeNode parent node
	 * @param childPos index of the foreach child
	 */
	protected void _removeRowTags(TreeNode treeNode, int childPos) {
	    if (treeNode == null) return;
	    if (childPos <= 0) return;
	    if (childPos + 1 >= treeNode.alChildren.size()) return;
	    
	    TreeNode tn = treeNode.alChildren.get(childPos-1);
        _removeRowTagsBefore(tn);

        tn = treeNode.alChildren.get(childPos).alChildren.get(0).alChildren.get(0);
        _removeRowTagsAfter(tn);
        
        int x = treeNode.alChildren.get(childPos).alChildren.get(0).alChildren.size();
        tn = treeNode.alChildren.get(childPos).alChildren.get(0).alChildren.get(x-1);
        _removeRowTagsBefore(tn);
        
        tn = treeNode.alChildren.get(childPos+1);
        _removeRowTagsAfter(tn);
        
        int qq = 4;
        qq++;
           
	}
	
	/**
	 * Scans backward through a node’s text and trims leading sequences such as
	 * "<tr><td>" until a stopping condition is reached.
	 *
	 * @param treeNode node whose text is modified
	 */
	protected void _removeRowTagsBefore(TreeNode treeNode) {
        /*        
             <tr>
                <td>
        */        

        String find = "<tr><td>";
        int findPos = find.length() - 1;
        
        String find2 = "<table";
        int findPos2 = find2.length() - 1;

        String find3 = "</tr";
        int findPos3 = find3.length() - 1;
        
        String s = treeNode.arg1;
        
        for (int i=s.length()-1; i>=0; i--) {
            char ch = s.charAt(i);
            ch = Character.toLowerCase(ch);
            char ch2 = find.charAt(findPos);
            if (ch == ch2) {
                findPos--;
                if (findPos < 0) {
                    s = s.substring(0, i);
                    break;
                }
            }
            
            ch2 = find2.charAt(findPos2);
            if (ch == ch2) {
                findPos2--;
                if (findPos2 < 0) {
                    break;
                }
            }

            ch2 = find3.charAt(findPos3);
            if (ch == ch2) {
                findPos3--;
                if (findPos3 < 0) {
                    break;
                }
            }
        }
        treeNode.arg1 = s;
	}        
	
	/**
	 * Scans forward through a node’s text and removes trailing table tags like
	 * "</td></tr>" up to a boundary condition.
	 *
	 * @param treeNode node whose text is modified
	 */
    protected void _removeRowTagsAfter(TreeNode treeNode) {
        
        String s = treeNode.arg1;
        
/*  remove beginning qqqqqq      
        ForeachEND
                </td>
              </tr>
  
*/        
        String find = "</td></tr>";
        int findPos = 0;
        int x = s.length();
        
        String find2 = "<tr>";
        int findPos2 = 0;

        String find3 = "</table";
        int findPos3 = 0;
        
        for (int i=0; i<x; i++) {
            char ch = s.charAt(i);
            ch = Character.toLowerCase(ch);
            char ch2 = find.charAt(findPos);
            if (ch == ch2) {
                findPos++;
                if (findPos == find.length()) {
                    s = s.substring(i+1);
                    break;
                }
            }
            
            ch2 = find2.charAt(findPos2);
            if (ch == ch2) {
                findPos2++;
                if (findPos2 == find2.length()) {
                    break;
                }
            }
            
            ch2 = find3.charAt(findPos3);
            if (ch == ch2) {
                findPos3++;
                if (findPos3 == find3.length()) {
                    break;
                }
            }
        }
        treeNode.arg1 = s;
    }
	
	
    /**
     * Preprocesses the template text by delegating to the overload that accepts an
     * include-tracking list.
     *
     * @param doc raw template text
     * @return processed text
     */
	protected String preprocess(String doc) {
		return preprocess(doc, null);
	}

	/**
	 * Expands <%=include %> directives by replacing them with the text returned
	 * by getIncludeText. Prevents recursive includes using the tracking list.
	 *
	 * @param doc text to preprocess
	 * @param alInclude names already included
	 * @return processed template text
	 */
	protected String preprocess(String doc, ArrayList<String> alInclude) {
		if (alInclude == null) {
			alInclude = new ArrayList<String>();
		}

		int pos = 0;
		for (;;) {
			int posHold = pos;
			pos = doc.indexOf("<%=include ", pos);
			if (pos < 0) {
				break;
			}
			int pos1 = doc.indexOf(" ", pos) + 1;
			int pos2 = doc.indexOf("%>", pos1);
			if (pos2 < 0) {
				if (parseErrorCnt++ < 5) {
					LOG.warning("Error: missing end tag for include %>");
				}
				break;
			}
			String text = doc.substring(pos1, pos2).trim();
			if (alInclude.contains(text)) {
				text = " ERROR: recursive include for " + text + " ";
			} else {
				alInclude.add(text);
				text = getIncludeText(text);
			}
			doc = doc.substring(0, pos) + text + doc.substring(pos2 + 2);
		}
		return doc;
	}

	/**
	 * Returns replacement text for an include directive. The default implementation
	 * returns an error message and can be overridden by subclasses.
	 *
	 * @param name include identifier
	 * @return replacement text for the include
	 */
	protected String getIncludeText(String name) {
		return " ERROR: no text for include " + name + " ";
	}

	/**
	 * Converts tokens into a hierarchy of TreeNode instances by repeatedly calling
	 * parseA for each token until input is exhausted.
	 *
	 * @param root root node receiving parsed children
	 */
	protected void parse(TreeNode root) {
		ppSample = null;
		for (;;) {
			TreeNode node = new TreeNode();
			root.alChildren.add(node);
			Token tok = getNextToken();
			if (tok == null) {
				break;
			}
			parseA(tok, node);
		}
	}

	/**
	 * Returns whether any parse errors were encountered while building the template
	 * tree.
	 *
	 * @return true if parse errors occurred
	 */
	public boolean getHasParseError() {
		return parseErrorCnt > 0;
	}

	/**
	 * Interprets a token and configures the Node's tag type and arguments.
	 * For block-style tags, delegates further parsing to parseB.
	 *
	 * @param tok token to interpret
	 * @param node node to populate
	 */
	private void parseA(Token tok, TreeNode node) {
		if (tok.hasEndToken()) {
			Token tokB = parseB(tok, node);
			if (tokB == null || tokB.tagType == null || tokB.tagType != TagType.End) {
				node.errorMsg = "Error: missing end tag for " + tok.data;
				if (parseErrorCnt++ < 5) {
					LOG.warning(node.errorMsg + ", Template=" + getTemplate());
				}
			}
		} else if (tok.tagType == null) {
			node.arg1 = tok.data;
		} else if (tok.tagType == TagType.GetProp) {
			node.tagType = TagType.GetProp;
			String s = OAString.field(tok.data, ",", 1).trim();
			node.arg1 = s;
			if (ppSample == null && s != null && !s.startsWith("$")) {
				ppSample = s;
			}
			String fmt = OAString.field(tok.data, ",", 2, 99);
			if (!OAString.isEmpty(fmt)) {
				fmt = fmt.trim();
				fmt = OAString.convert(fmt, '\'', "");
				fmt = OAString.convert(fmt, '\"', "");
				node.arg2 = fmt;
			}
		}
		if (tok.tagType != TagType.Command) {
			return;
		}

		String s = OAString.field(tok.data, ",", 1).trim();
		if (s == null) {
			s = tok.data;
			if (s == null) {
				s = "";
			}
		}
		String s1 = OAString.field(s, " ", 2);
		if (s1 == null) {
			s1 = "";
		}
		s = OAString.field(s, " ", 1);

		String fmt = OAString.field(tok.data, ",", 2, 99); // fmt
		if (fmt == null) {
			fmt = "";
		} else {
			fmt = fmt.trim();
			fmt = OAString.convert(fmt, '\'', "");
			fmt = OAString.convert(fmt, '\"', "");
		}

		if (s.equalsIgnoreCase("#counter")) {
			node.tagType = TagType.Counter;
		} else if (s.equalsIgnoreCase("#count")) {
			node.tagType = TagType.Count;
		} else if (s.equalsIgnoreCase("#sum")) {
			node.tagType = TagType.Sum;
		}

		node.arg1 = s1; // name
		node.arg2 = fmt;
	}

	/**
	 * Handles tags that span a block with a matching end tag. Sets tag metadata
	 * and parses enclosed content using parseC.
	 *
	 * @param tok opening token
	 * @param node node to configure
	 * @return end token or null
	 */
	private Token parseB(Token tok, TreeNode node) {
		if (tok.tagType == TagType.Format) {
			node.tagType = TagType.Format;
			String fmt = OAString.field(tok.data, ",", 2, 99);
			if (fmt == null) {
				fmt = "";
			}
			fmt = fmt.trim();
			fmt = OAString.convert(fmt, '\'', "");
			fmt = OAString.convert(fmt, '\"', "");
			node.arg1 = fmt;
		} else if (tok.tagType == TagType.ForEach) {
			node.tagType = TagType.ForEach;
			node.arg1 = OAString.field(tok.data, " ", 2);
			if (node.arg1 == null) {
				node.arg1 = "";
			}
		} else if (tok.tagType == TagType.IfNot) {
			node.tagType = TagType.IfNot;
			node.arg1 = OAString.field(tok.data, " ", 2);
		} else if (tok.tagType == TagType.IfNotEquals) {
			node.tagType = TagType.IfNotEquals;
			node.arg1 = OAString.field(tok.data, " ", 2);
			node.arg2 = OAString.field(tok.data, " ", 3);
		} else if (tok.tagType == TagType.If) {
			node.arg1 = OAString.field(tok.data, " ", 2);
			node.tagType = TagType.If;

			// see if this is an expanded if, using operator
			if (OAString.dcount(tok.data, " ") == 4) {
				String op = OAString.field(tok.data, " ", 3);
				node.arg2 = OAString.field(tok.data, " ", 4);
				if (op.equals("==") || op.equals("=")) {
					node.tagType = TagType.IfEquals;
				} else if (op.equals("!=")) {
					node.tagType = TagType.IfNotEquals;
				} else if (op.equals(">")) {
					node.tagType = TagType.IfGt;
				} else if (op.equals(">=")) {
					node.tagType = TagType.IfGte;
				} else if (op.equals("<")) {
					node.tagType = TagType.IfLt;
				} else if (op.equals("<=")) {
					node.tagType = TagType.IfLte;
				}
			}
		} else if (tok.tagType == TagType.IfEquals) {
			node.tagType = TagType.IfEquals;
			node.arg1 = OAString.field(tok.data, " ", 2);
			node.arg2 = OAString.field(tok.data, " ", 3);
		} else if (tok.tagType == TagType.IfGt) {
			node.tagType = TagType.IfGt;
			node.arg1 = OAString.field(tok.data, " ", 2);
			node.arg2 = OAString.field(tok.data, " ", 3);
		} else if (tok.tagType == TagType.IfGte) {
			node.tagType = TagType.IfGte;
			node.arg1 = OAString.field(tok.data, " ", 2);
			node.arg2 = OAString.field(tok.data, " ", 3);
		} else if (tok.tagType == TagType.IfLt) {
			node.tagType = TagType.IfLt;
			node.arg1 = OAString.field(tok.data, " ", 2);
			node.arg2 = OAString.field(tok.data, " ", 3);
		} else if (tok.tagType == TagType.IfLte) {
			node.tagType = TagType.IfLte;
			node.arg1 = OAString.field(tok.data, " ", 2);
			node.arg2 = OAString.field(tok.data, " ", 3);
		}

		// go to end tag
		TreeNode nodex = new TreeNode();
		node.alChildren.add(nodex);
		Token tokx = parseC(tok, nodex);

		return tokx;
	}

	/**
	 * Reads tokens until an end tag matching the supplied opening token is found,
	 * adding each intermediate token as a child TreeNode.
	 *
	 * @param tok opening tag token
	 * @param node node that receives child nodes
	 * @return end token or null if input ends prematurely
	 */
	private Token parseC(Token tok, TreeNode node) {
		Token tokX;
		for (;;) {
			tokX = getNextToken();
			if (tokX == null || (tokX.tagType != null && tokX.tagType == TagType.End)) {
				break;
			}
			TreeNode nodex = new TreeNode();
			node.alChildren.add(nodex);
			parseA(tokX, nodex);
		}
		return tokX;
	}

	/**
	 * Internal list of tokens produced by parseTokens and consumed during tree
	 * construction.
	 */
	private ArrayList<Token> alToken;
	
	/**
	 * Current read position within the token list, advanced by getNextToken during
	 * parsing.
	 */
	private int posToken;

	/**
	 * Enumeration of all tag types supported by the template engine, including
	 * property lookups, conditionals, loops, counters, and formatting directives.
	 */
	static enum TagType {
		GetProp, // arg1=prop, arg2=fmt
		Format, // arg1=fmt
		If, // arg1=prop
		IfNot, // arg1=prop
		IfEquals, // arg1=prop, arg2=value
		IfNotEquals, // arg1=prop, arg2=value
		IfGt, // arg1=prop, arg2=num
		IfGte, // arg1=prop, arg2=num
		IfLt, // arg1=prop, arg2=num
		IfLte, // arg1=prop, arg2=num
		ForEach, // arg1=prop
		Equals, // arg1=prop, arg2=value
		NotEquals, // arg1=prop, arg2=value
		End,
		Command, // arg1=prop
		Counter, // arg1=prop name (hub) to count, arg2=fmt
		Count, // arg1=prop, arg2=fmt
		Sum // arg1=prop, arg2=prop, arg3=fmt
	}

	/**
	 * Represents a parsed element of the template. Holds tag type information,
	 * arguments, error state, and a list of child nodes used during evaluation.
	 */
	static class TreeNode {
		TagType tagType;
		String arg1, arg2, arg3;
		String errorMsg;
		final ArrayList<TreeNode> alChildren = new ArrayList<TreeNode>(5);
	}

	/**
	 * Represents a token extracted from the template during parseTokens. Contains
	 * raw text, tag metadata, and end-tag identification flags.
	 */
	static class Token {
		String data;
		TagType tagType;
		boolean missingEnd;

		public boolean hasEndToken() {
			boolean b;
			if (tagType != null) {
				b = (tagType == TagType.Format || tagType == TagType.If || tagType == TagType.IfNot
						|| tagType == TagType.IfNotEquals || tagType == TagType.ForEach
						|| tagType == TagType.Equals || tagType == TagType.NotEquals
						|| tagType == TagType.IfGt || tagType == TagType.IfGte
						|| tagType == TagType.IfLt || tagType == TagType.IfLte);
			} else {
				b = false;
			}
			return b;
		}
	}

	/**
	 * Returns the next token in the token list or null if no more tokens remain.
	 *
	 * @return next token or null
	 */
	private Token getNextToken() {
		int x = alToken.size();
		if (posToken >= x) {
			return null;
		}
		Token t = alToken.get(posToken++);
		return t;
	}

	/**
	 * Tokenizes raw template text into literal segments and tagged expressions.
	 * Assigns tag types where applicable.
	 *
	 * @param doc raw template text
	 * @return list of parsed tokens
	 */
	protected ArrayList<Token> parseTokens(String doc) {
		ArrayList<Token> alToken = new ArrayList<OATemplate.Token>();
		int pos = 0;
		for (;;) {
			int posHold = pos;
			pos = doc.indexOf("<%=", pos);
			if (pos < 0) {
				if (posHold < doc.length()) {
					Token tok = new Token();
					alToken.add(tok);
					tok.data = doc.substring(posHold);
				}
				break; // done
			}

			Token tok = new Token();
			alToken.add(tok);

			int pos2 = doc.indexOf("%>", pos + 3);
			if (pos2 < 0) {
				tok.missingEnd = true;
				tok.data = doc.substring(pos);
				break;
			}

			if (posHold < pos) {
				tok.data = doc.substring(posHold, pos);
				tok = new Token();
				alToken.add(tok);
			}

			String tag = doc.substring(pos + 3, pos2);
			String tag2 = doc.substring(pos + 3, pos2 + 1);

			pos2 += 2; // after %>
			tag = OAString.trimWhitespace(tag);
			tok.data = tag;
			tag2 = OAString.trimWhitespace(tag2);

			pos = pos2;

			tag = tag.toLowerCase();
			tag2 = tag2.toLowerCase();

			if (tag.startsWith("#")) {
				tok.tagType = TagType.Command;
			} else if (tag2.startsWith("end %")) {
				tok.tagType = TagType.End;
			} else if (tag2.startsWith("end%")) {
				tok.tagType = TagType.End;
			} else if (tag2.contains("end%")) {
				tok.tagType = TagType.End;
			} else if (tag2.contains("end ")) {
				tok.tagType = TagType.End;
			} else if (tag.startsWith("format ")) {
				tok.tagType = TagType.Format;
			} else if (tag.startsWith("foreach")) {
				tok.tagType = TagType.ForEach;
			} else if (tag.startsWith("ifnot ")) {
				tok.tagType = TagType.IfNot;
			} else if (tag.startsWith("if ")) {
				tok.tagType = TagType.If;
			} else if (tag.startsWith("ifequals ")) {
				tok.tagType = TagType.IfEquals;
			} else if (tag.startsWith("ifnotequals ")) {
				tok.tagType = TagType.IfNotEquals;
			} else if (tag.startsWith("ifgt ")) {
				tok.tagType = TagType.IfGt;
			} else if (tag.startsWith("ifgte ")) {
				tok.tagType = TagType.IfGte;
			} else if (tag.startsWith("iflt ")) {
				tok.tagType = TagType.IfLt;
			} else if (tag.startsWith("iflte ")) {
				tok.tagType = TagType.IfLte;
			} else { // get property value
				tok.tagType = TagType.GetProp;
			}
		}
		return alToken;
	}

	/**
	 * Tracks iteration counters for active foreach loops. Each loop name maps to
	 * its current iteration index, allowing ${#counter} style expressions to
	 * resolve numeric positions.
	 */
	private HashMap<String, Integer> hmForEachCounter = new HashMap<String, Integer>();

	/**
	 * Drives template evaluation by recursively walking the parsed tree and
	 * appending output to the provided StringBuilder. Returns false if
	 * stopProcessing() was invoked during generation.
	 *
	 * @param node current tree node to evaluate
	 * @param obj current root object used for property lookup
	 * @param hub hub used for foreach evaluation
	 * @param sb StringBuilder that receives output
	 * @param props external properties for $name resolution
	 * @param cntStop initial stop counter used to detect cancellations
	 * @return true if generation completed normally, false if cancelled
	 */
	protected boolean generate(TreeNode node, OAObject obj, Hub hub, StringBuilder sb, OAProperties props, final int cntStop) {
		boolean b = false;
		OASiblingHelper siblingHelper = null;
		try {
			if (hub != null) {
				siblingHelper = new OASiblingHelper(hub);
			}
			OAThreadLocalDelegate.addSiblingHelper(siblingHelper);
			b = _generate(node, obj, hub, sb, props, cntStop);
		} finally {
			if (siblingHelper != null) {
				OAThreadLocalDelegate.removeSiblingHelper(siblingHelper);
			}
		}
		return b;
	}
	
	
	/**
	 * Tracks whether template processing is occurring within a data grid expansion.
	 * Used to modify behavior for nested property resolution.
	 */
	private int cntInDataGrid;

	/**
	 * Internal recursive implementation of template generation. Handles all tag
	 * types—including foreach, formatting, conditionals, counters, and property
	 * lookups—and appends results to the provided StringBuilder.
	 *
	 * @param node tree node to process
	 * @param obj root object used for property evaluation
	 * @param hub hub used for foreach iteration
	 * @param sb accumulated output buffer
	 * @param props external property map
	 * @param cntStop stop counter used to detect cancellation
	 * @return true if processing completed without interruption
	 */
	protected boolean _generate(final TreeNode node, final OAObject obj, final Hub hub, StringBuilder sb, final OAProperties props,
			final int cntStop) {

		if (aiStopCalled.get() != cntStop) {
			return false;
		}
		boolean bNot = false;
		boolean bProcessChildren = true;

		if (node.errorMsg != null) {
			String s = getOutputText(node.errorMsg);
			sb.append(s);
		}
		if (node.tagType == null) {
			String s = node.arg1;
			if (!OAString.isEmpty(node.arg2)) {
				s = OAString.format(s, node.arg2);
			}
			if (s != null) {
				sb.append(s);
			}
		} else {
			switch (node.tagType) {
			case ForEach:
				bProcessChildren = false;
				Object objValue;
				if (obj != null && !OAString.isEmpty(node.arg1)) {
					objValue = obj.getProperty(node.arg1);
				} else {
					objValue = hub;
				}

				if (objValue instanceof Hub) {
	                final OAObjectGrid og = createObjectGrid(node, (Hub) objValue);
	                if (og != null) {
	                    cntInDataGrid++;
	                    int x = og.getRowCount();
	                    
	                    final Map<String, Integer> hmPropertyToColumn = new HashMap();
                        int col = 0;
                        for (OAObjectGrid.Column colx : og.getColumns()) {
                            String spp = og.getPropertyPathFromRoot(colx, "");
                            hmPropertyToColumn.put(spp, col);
                            col++;
                        }
	                    
	                    for (int row = 0; row < x; row++) {
	                        hmForEachCounter.put(node.arg1, row + 1); //qqqq might want counter to be object counter, not row counter
	                        TreeNode node2 = node.alChildren.get(0);
	                        for (TreeNode dn : node2.alChildren) {
                                OAObject oa = null; 
	                            if (dn.tagType == TagType.GetProp || dn.tagType == TagType.ForEach) {
	                                // find column in dataGrid
	                                OAPropertyPath pp = new OAPropertyPath( ((Hub) objValue).getObjectClass(), dn.arg1);
	                                final String sppLinks = pp.getPropertyPathLinksOnly();
	                                col = hmPropertyToColumn.get(sppLinks);
                                    oa = (OAObject) og.getObject(row, col);
	                            }
	                            if (!generate(dn, oa, hub, sb, props, cntStop)) {
	                                return false;
	                            }
	                        }
	                    }
                        cntInDataGrid--;
	                }
	                else {
	                    final Hub h = (Hub) objValue;
    					for (int i = 0;; i++) {
    						hmForEachCounter.put(node.arg1, i + 1);
    						OAObject oa = (OAObject) h.elementAt(i);
    						if (oa == null) {
    							break;
    						}
    						for (TreeNode dn : node.alChildren) {
    							if (!generate(dn, oa, hub, sb, props, cntStop)) {
    								return false;
    							}
    						}
    					}
	                }
				} else {
					if (obj != null) {
						LOG.warning("Hub for 'Foreach' not found");
					}
				}
				break;

			case Format:
				bProcessChildren = false;

				StringBuilder sbHold = sb;
				sb = new StringBuilder(1024 * 4);

				for (TreeNode dn : node.alChildren) {
					if (!generate(dn, obj, hub, sb, props, cntStop)) {
						return false;
					}
				}

				String s = new String(sb);
				s = OAString.format(s, node.arg1);
				s = getOutputText(s);
				// s = OAString.convert(s, "  ", "&nbsp;&nbsp;");
				sb = sbHold;
				sb.append(s);

				break;

			case IfNot:
				bNot = true;
			case If:
				// if not null, blank or 0.0
				s = getValue(obj, node.arg1, 0, null, props, false);

				bProcessChildren = false;
				if (s != null) {
					if (s.length() > 0) {
						if (OAString.isNumber(s)) {
							bProcessChildren = (OAConv.toDouble(s) != 0.0);
						} else {
							// bProcessChildren = OAConv.toBoolean(s);
							if (s == null || s.length() == 0) {
								bProcessChildren = false;
							} else {
								if (s.equalsIgnoreCase("false")) {
									bProcessChildren = false;
								} else {
									bProcessChildren = true;
								}
							}
						}
					}
				}
				if (bNot) {
					bProcessChildren = !bProcessChildren;
				}
				break;

			case IfEquals:
				s = getValue(obj, node.arg1, 0, null, props, false);

				bProcessChildren = OAString.isEqual(s, node.arg2);
				break;

			case IfGt:
				s = getValue(obj, node.arg1, 0, null, props, false);
				if (OAString.isNumber(s) && OAString.isNumber(node.arg2)) {
					double d1 = OAConv.toDouble(s);
					double d2 = OAConv.toDouble(node.arg2);
					bProcessChildren = d1 > d2;
				} else {
					bProcessChildren = false;
				}
				break;
			case IfGte:
				s = getValue(obj, node.arg1, 0, null, props, false);
				if (OAString.isNumber(s) && OAString.isNumber(node.arg2)) {
					double d1 = OAConv.toDouble(s);
					double d2 = OAConv.toDouble(node.arg2);
					bProcessChildren = d1 >= d2;
				} else {
					bProcessChildren = false;
				}
				break;

			case IfLt:
				s = getValue(obj, node.arg1, 0, null, props, false);
				if (OAString.isNumber(s) && OAString.isNumber(node.arg2)) {
					double d1 = OAConv.toDouble(s);
					double d2 = OAConv.toDouble(node.arg2);
					bProcessChildren = d1 < d2;
				} else {
					bProcessChildren = false;
				}
				break;

			case IfLte:
				s = getValue(obj, node.arg1, 0, null, props, false);
				if (OAString.isNumber(s) && OAString.isNumber(node.arg2)) {
					double d1 = OAConv.toDouble(s);
					double d2 = OAConv.toDouble(node.arg2);
					bProcessChildren = d1 <= d2;
				} else {
					bProcessChildren = false;
				}
				break;

			case GetProp:
				String prop = node.arg1;
				String fmt = node.arg2;

				int width = 0;
				if (!OAString.isEmpty(fmt)) {
					if (OAString.isNumber(fmt)) {
						width = OAConv.toInt(fmt);
						fmt = null;
					} else {
						fmt = fmt.trim();
						fmt = OAString.convert(fmt, '\'', "");
						fmt = OAString.convert(fmt, '\"', "");
					}
				}
				s = getValue(obj, prop, width, fmt, props, true);
				s = getOutputText(s);
				sb.append(s);
				break;

			case Counter:
				prop = node.arg1; // from open forEach loop
				fmt = node.arg2;
				Integer ix = hmForEachCounter.get(prop);
				if (ix == null) {
					sb.append("Error: " + prop + ".counter not valid");
				} else {
					s = ix.toString();
					if (!OAString.isEmpty(fmt)) {
						s = OAString.format(s, fmt);
					}
					s = getOutputText(s);
					sb.append(s);
				}
				break;
			case Count:
				prop = node.arg1;
				fmt = node.arg2;
				if (obj == null) {
					break;
				}
				Object objx = obj.getProperty(prop);
				if (!(objx instanceof Hub)) {
					return true;
				}
				s = OAConv.toString(((Hub) objx).getSize(), fmt);
				s = getOutputText(s);
				sb.append(s);
				break;
			case Sum:
				prop = node.arg1;
				String prop2 = node.arg2;
				fmt = node.arg3;
				if (obj == null) {
					break;
				}
				objx = obj.getProperty(prop);
				if (!(objx instanceof Hub)) {
					return true;
				}
				double d = 0.0d;
				for (Object objz : ((Hub) objx)) {
					if (!(objz instanceof OAObject)) {
						continue;
					}
					objx = ((OAObject) objz).getProperty(prop2);
					if (!(objx instanceof Number)) {
						continue;
					}
					d += OAConv.toDouble(objx);
				}
				s = OAConv.toString(d, fmt);
				s = getOutputText(s);
				sb.append(s);
				break;
			}
		}
		if (bProcessChildren && node.alChildren != null) {
			for (TreeNode dn : node.alChildren) {
				if (!generate(dn, obj, hub, sb, props, cntStop)) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Constructs an OAObjectGrid based on the property paths referenced within a
	 * foreach block. Columns are added according to link traversal, and the grid
	 * is materialized before iteration.
	 *
	 * @param node foreach node whose children define the required property paths
	 * @param hub hub providing source objects for grid expansion
	 * @return an OAObjectGrid instance or null if not required
	 */
	protected OAObjectGrid createObjectGrid(TreeNode node, Hub hub) {
        OAObjectGrid og = new OAObjectGrid();
        final OAObjectGrid.Column colRoot = og.addColumn(hub);
        
        node = node.alChildren.get(0); // nodes between foreach .. end
        boolean bRequired = false;
        for (TreeNode cn : node.alChildren) {
            if (cn.tagType != TagType.GetProp) {
                if (cn.tagType != TagType.ForEach) continue;
            }
        
            // make columns for pp
            OAPropertyPath pp = new OAPropertyPath(hub.getObjectClass(), cn.arg1);
            OALinkInfo[] lis = pp.getLinkInfos();
            if (lis == null || lis.length == 0) {
                continue; // root column
            }
            
            OAObjectGrid.Column colParent = colRoot;
            OAObjectGrid.Column colFound = null;
            for (OALinkInfo li : lis) {
                if (li.getType() == OALinkInfo.TYPE_MANY) {
                    if (cn.tagType != TagType.ForEach) {
                        bRequired = true;
                    }
                }
                boolean bFound = false;
                for (OAObjectGrid.Column colx : og.getColumns()) {
                    if (colFound != null) {
                        if (colx.getFromColumn() != colFound) continue;
                    }
                    String sppx = colx.getPropertyPath();
                    if (OAStr.isEmpty(sppx)) continue;
                    if (sppx.equalsIgnoreCase(li.getName())) {
                        colFound = colx;
                        bFound = true;
                        break;
                    }
                }
                if (!bFound) {
                    colFound = og.addDetailColumn(colParent, li.getName());
                }
                colParent = colFound;
            }
        }
        if (!bRequired) {
            if (cntInDataGrid == 0) return null;
        }
        og.createGrid();
        return og;
    }

	/**
	 * Applies optional output transformations to the supplied string, including
	 * a replace-from/replace-to conversion and text highlighting if configured.
	 *
	 * @param s text to convert
	 * @return transformed output text
	 */
	protected String getOutputText(String s) {
		if (OAString.isNotEmpty(fromText)) {
			s = OAString.convert(s, fromText, toText);
		}
		if (OAString.isNotEmpty(hiliteText)) {
			s = OAString.hilite(s, hiliteText);
		}
		return s;
	}

	/**
	 * Assigns conversion parameters used by getOutputText to replace occurrences
	 * of fromText with toText in the generated output.
	 *
	 * @param fromText source sequence to replace
	 * @param toText replacement sequence
	 */
	public void setOutputTextConversion(String fromText, String toText) {
		this.fromText = fromText;
		this.toText = toText;
	}

	/**
	 * Configures a highlight expression applied by getOutputText, allowing matching
	 * segments of output to be emphasized.
	 *
	 * @param text highlight directive
	 */
	public void setHiliteOutputText(String text) {
		this.hiliteText = text;
	}

	/*qqqqqqqqq
	 * Called to get the value of a property.
	 * @param obj Object parameter from getHtml()
	 * @param propertyName name of property parsed between <%=XX%> parameters.
	 * @return
	 */
	/*qqqqqqq
	protected String getValue(OAObject obj, String propertyName, int width, String fmt, OAProperties props) {
	    return getValue(obj, propertyName, width, fmt, props, false);
	}
	*/

	/**
	 * Resolves a template variable or object property value as a string. Handles
	 * $name variables, property paths, hubs, booleans, dates, formatting, and
	 * fixed-width truncation.
	 *
	 * @param obj active object used for property resolution
	 * @param propertyName name of the variable or property
	 * @param width optional max width to truncate the result
	 * @param fmt optional output format
	 * @param props external property map for $name resolution
	 * @param bUseFormat true to apply default format from OAPropertyPath
	 * @return resolved value as a string, never null
	 */
	protected String getValue(OAObject obj, String propertyName, int width, String fmt, OAProperties props, boolean bUseFormat) {
		if (propertyName == null) {
			return "";
		}
		String result = null;

		boolean bFmt = true;
		if (propertyName.startsWith("$")) {
			if (propertyName.length() > 1) {
				propertyName = propertyName.substring(1);
			}

			if (fmt != null && fmt.length() > 0) {
				Object objx = null;
				if (props != null) {
					objx = props.get(propertyName);
				}
				if (objx == null) {
					if (propInternal != null) {
						objx = propInternal.get(propertyName);
					}
				}
				if (objx != null) {
					if (objx instanceof OADateTime) {
						result = ((OADateTime) objx).toString(fmt);
						bFmt = false;
					} else {
						if (objx != null) {
							result = objx.toString();
						}
					}
				}
			} else {
				if (props != null) {
					result = props.getString(propertyName);
				}
				if (result == null) {
					if (propInternal != null) {
						Object objx = propInternal.get(propertyName);
						if (objx == null) {
							result = null;
						} else {
							result = objx.toString();
						}
					}
				}
			}
		} else {
			if (obj != null && propertyName.length() > 0) {
			    
			    if (cntInDataGrid > 0) {
			        int x = OAStr.dcount(propertyName, '.');
			        if (x > 1) propertyName = OAStr.field(propertyName, '.', x);
			    }
			    
				Object objx;
				if (obj != null) {
					objx = this.getProperty(obj, propertyName);
				} else {
					objx = null;
				}
				if (objx instanceof Boolean && fmt != null && fmt.indexOf(';') >= 0) {
					result = OAConv.toString(objx, fmt);
					bFmt = false;
				} else {
					if (objx instanceof Hub) {
						objx = ((Hub) objx).getSize(); // default is to get size of hub
					}

					String fmtx = null;
					if (bUseFormat && OAString.isEmpty(fmt) && obj != null) {
						bFmt = false;
						OAPropertyPath pp = new OAPropertyPath(obj.getClass(), propertyName, true);
						fmtx = pp.getFormat();
					}

					result = OAConv.toString(objx, fmtx);

					// if not html, then convert [lf] to <br>
					boolean b = true;
					if (result.indexOf('<') >= 0 && result.indexOf('>') >= 0) {
						String s = result.toLowerCase();
						if (s.indexOf("<p") >= 0 || s.indexOf("<span") >= 0 || s.indexOf("<b") >= 0 || s.indexOf("<i") >= 0) {
							b = false;
						}
					}

					if (b && result.indexOf("\n") >= 0) {
						result = OAString.convert(result, "\r\n", "<br>");
						result = OAString.convert(result, "\n", "<br>");
					}
				}
			}
		}
		if (result == null) {
			result = "";
		}
		if (width > 0) {
			result = OAString.truncate(result, width);
		}

		if (bFmt && fmt != null && fmt.length() > 0) {
			result = OAString.format(result, fmt);
			// result = OAString.convert(result, "  ", "&nbsp;&nbsp;");
		}

		return result;
	}

	/**
	 * Retrieves the value of a property from the supplied OAObject. Supports
	 * nested property paths and automatically resolves multi-valued properties
	 * using OAFinder to concatenate values.
	 *
	 * @param oaObj object from which to read the property
	 * @param propertyName simple or nested property name
	 * @return property value or null
	 */
	protected Object getProperty(OAObject oaObj, String propertyName) {
		if (oaObj == null) {
			return null;
		}

		if (OAString.isNotEmpty(propertyName) && propertyName.indexOf('.') >= 0) {
			OAPropertyPath pp = new OAPropertyPath(oaObj.getClass(), propertyName, true);
			if (pp.getHasHubProperty()) {
				// 20190131 useFinder for pp with hubs
				final VString vs = new VString();
				OAFinder finder = new OAFinder(pp.getPropertyPathLinksOnly()) {
					@Override
					protected void onFound(OAObject obj) {
						Object objx = obj.getProperty(pp.getLastPropertyName());
						String s = OAConv.toString(objx);
						vs.setValue(OAString.concat(vs.getValue(), s, ", "));
					}
				};
				finder.find(oaObj);
				return vs.getValue();
			}
		}
		return oaObj.getProperty(propertyName);
	}
	
}
