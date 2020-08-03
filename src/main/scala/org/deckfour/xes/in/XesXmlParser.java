/*
 * OpenXES
 * 
 * The reference implementation of the XES meta-model for event 
 * log data management.
 * 
 * Copyright (c) 2008 Christian W. Guenther (christian@deckfour.org)
 * 
 * 
 * LICENSE:
 * 
 * This code is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 * 
 * EXEMPTION:
 * 
 * The use of this software can also be conditionally licensed for
 * other programs, which do not satisfy the specified conditions. This
 * requires an exemption from the general license, which may be
 * granted on a per-case basis.
 * 
 * If you want to license the use of this software with a program
 * incompatible with the LGPL, please contact the author for an
 * exemption at the following email address: 
 * christian@deckfour.org
 * 
 */
package org.deckfour.xes.in;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.deckfour.xes.classification.XEventAttributeClassifier;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.extension.XExtension;
import org.deckfour.xes.extension.XExtensionManager;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XAttributable;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.buffered.XTraceBufferedImpl;
import org.deckfour.xes.util.XsDateTimeConversion;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Parser for the XES XML serialization.
 * 
 * @author Christian W. Guenther (christian@deckfour.org)
 * 
 */
public class XesXmlParser extends XParser {

	protected XsDateTimeConversion xsDateTimeConversion = new XsDateTimeConversion();

	/**
	 * Unique URI for the format definition.
	 */
	protected static final URI XES_URI = URI
			.create("http://code.deckfour.org/xes");

	/**
	 * XES model factory used to build model.
	 */
	protected XFactory factory;

	/**
	 * Creates a new parser instance.
	 * 
	 * @param factory
	 *            The XES model factory instance used to build the model from
	 *            the serialization.
	 */
	public XesXmlParser(XFactory factory) {
		this.factory = factory;
	}

	/**
	 * Creates a new parser instance, using the currently-set standard factory
	 * for building the model.
	 */
	public XesXmlParser() {
		this(XFactoryRegistry.instance().currentDefault());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.deckfour.xes.in.XParser#author()
	 */
	@Override
	public String author() {
		return "Christian W. Günther";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.deckfour.xes.in.XParser#canParse(java.io.File)
	 */
	@Override
	public boolean canParse(File file) {
		String filename = file.getName();
		String suffix = filename.substring(filename.length() - 3);
		return suffix.equalsIgnoreCase("xes");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.deckfour.xes.in.XParser#description()
	 */
	@Override
	public String description() {
		return "Reads XES models from plain XML serializations";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.deckfour.xes.in.XParser#name()
	 */
	@Override
	public String name() {
		return "XES XML";
	}

	/**
	 * Parses a log from the given input stream, which is supposed to deliver an
	 * XES log in XML representation.
	 * 
	 * @param is
	 *            Input stream, which is supposed to deliver an XES log in XML
	 *            representation.
	 * @return The parsed log.
	 */
	public List<XLog> parse(InputStream is) throws Exception {
		BufferedInputStream bis = new BufferedInputStream(is);
		// set up a specialized SAX2 handler to fill the container
		XesXmlHandler handler = new XesXmlHandler();
		// set up SAX parser and parse provided log file into the container
		SAXParserFactory parserFactory = SAXParserFactory.newInstance();
		parserFactory.setNamespaceAware(false);
		SAXParser parser = parserFactory.newSAXParser();
		parser.parse(bis, handler);
		bis.close();
		ArrayList<XLog> wrapper = new ArrayList<XLog>();
		wrapper.add(handler.getLog());
		return wrapper;
	}

	/**
	 * SAX handler class for XES in XML representation.
	 * 
	 * @author Christian W. Guenther (christian@deckfour.org)
	 * 
	 */
	protected class XesXmlHandler extends DefaultHandler {

		/**
		 * Buffer log.
		 */
		protected XLog log;
		/**
		 * Buffer trace.
		 */
		protected XTrace trace;
		/**
		 * Buffer event.
		 */
		protected XEvent event;
		/**
		 * Buffer for attributes.
		 */
		protected Stack<XAttribute> attributeStack;
		/**
		 * Buffer for attributables.
		 */
		protected Stack<XAttributable> attributableStack;
		/**
		 * Buffer for extensions.
		 */
		protected HashSet<XExtension> extensions;
		/**
		 * Buffer for globals.
		 */
		protected List<XAttribute> globals;

		/**
		 * Creates a new handler instance.
		 */
		public XesXmlHandler() {
			log = null;
			trace = null;
			event = null;
			attributeStack = new Stack<XAttribute>();
			attributableStack = new Stack<XAttributable>();
			extensions = new HashSet<XExtension>();
			globals = null;
		}

		/**
		 * Retrieves the parsed log.
		 * 
		 * @return The parsed log.
		 */
		public XLog getLog() {
			return log;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String,
		 * java.lang.String, java.lang.String, org.xml.sax.Attributes)
		 */
		@Override
		public void startElement(String uri, String localName, String qName,
				Attributes attributes) throws SAXException {
			// resolve tag name
			String tagName = localName.trim();
			if (tagName.length() == 0) {
				tagName = qName;
			}
			// parse content
			if (tagName.equalsIgnoreCase("string")
					|| tagName.equalsIgnoreCase("date")
					|| tagName.equalsIgnoreCase("int")
					|| tagName.equalsIgnoreCase("float")
					|| tagName.equalsIgnoreCase("boolean")) {
				// attribute tag.
				String key = attributes.getValue("key");
				String value = attributes.getValue("value");
				if (value != null) {
					// derive extension, if attribute key hints that
					XExtension extension = null;
					int colonIndex = key.indexOf(':');
					if (colonIndex > 0 && colonIndex < (key.length() - 1)) {
						String prefix = key.substring(0, colonIndex);
						extension = XExtensionManager.instance().getByPrefix(
								prefix);
					}
					// create attribute of correct type
					XAttribute attribute = null;
					if (tagName.equalsIgnoreCase("string")) {
						attribute = factory.createAttributeLiteral(key, value,
								extension);
					} else if (tagName.equalsIgnoreCase("date")) {
						Date date = xsDateTimeConversion.parseXsDateTime(value);
						if (date != null) {
							attribute = factory.createAttributeTimestamp(key,
									date, extension);
						} else {
							return;
						}
					} else if (tagName.equalsIgnoreCase("int")) {
						attribute = factory.createAttributeDiscrete(key, Long
								.parseLong(value), extension);
					} else if (tagName.equalsIgnoreCase("float")) {
						attribute = factory.createAttributeContinuous(key,
								Double.parseDouble(value), extension);
					} else if (tagName.equalsIgnoreCase("boolean")) {
						attribute = factory.createAttributeBoolean(key, Boolean
								.parseBoolean(value), extension);
					}
					// add to current attributable and push to stack
					attributeStack.push(attribute);
					attributableStack.push(attribute);
				}
			} else if (tagName.equalsIgnoreCase("event")) {
				// event element
				event = factory.createEvent();
				attributableStack.push(event);
			} else if (tagName.equalsIgnoreCase("trace")) {
				// trace element
				trace = factory.createTrace();
				attributableStack.push(trace);
			} else if (tagName.equalsIgnoreCase("log")) {
				// log element
				log = factory.createLog();
				attributableStack.push(log);
			} else if (tagName.equalsIgnoreCase("extension")) {
				// extension element
				XExtension extension = null;
				String uriString = attributes.getValue("uri");
				if (uriString != null) {
					extension = XExtensionManager.instance().getByUri(
							URI.create(uriString));
				} else {
					String prefixString = attributes.getValue("prefix");
					if (prefixString != null) {
						extension = XExtensionManager.instance().getByPrefix(
								prefixString);
					}
				}
				if (extension != null) {
					log.getExtensions().add(extension);
				}
			} else if (tagName.equalsIgnoreCase("global")) {
				// global element
				String scope = attributes.getValue("scope");
				if (scope.equalsIgnoreCase("trace")) {
					this.globals = log.getGlobalTraceAttributes();
				} else if (scope.equalsIgnoreCase("event")) {
					this.globals = log.getGlobalEventAttributes();
				}
			} else if (tagName.equalsIgnoreCase("classifier")) {
				// classifier element
				String name = attributes.getValue("name");
				String keys = attributes.getValue("keys");
				if (name != null && keys != null && name.length() > 0
						&& keys.length() > 0) {
					String keysArr[] = keys.split("\\s+");
					XEventClassifier classifier = new XEventAttributeClassifier(
							name, keysArr);
					log.getClassifiers().add(classifier);
				}
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String,
		 * java.lang.String, java.lang.String)
		 */
		@Override
		public void endElement(String uri, String localName, String qName)
				throws SAXException {
			// resolve tag name
			String tagName = localName.trim();
			if (tagName.length() == 0) {
				tagName = qName;
			}
			// parse content
			if (tagName.equalsIgnoreCase("global")) {
				// close globals
				this.globals = null;
			} else if (tagName.equalsIgnoreCase("string")
					|| tagName.equalsIgnoreCase("date")
					|| tagName.equalsIgnoreCase("int")
					|| tagName.equalsIgnoreCase("float")
					|| tagName.equalsIgnoreCase("boolean")) {
				XAttribute attribute = attributeStack.pop();
				attributableStack.pop(); // remove self from top
				if (globals != null) {
					globals.add(attribute);
				} else {
					attributableStack.peek().getAttributes().put(
							attribute.getKey(), attribute);
				}
			} else if (tagName.equalsIgnoreCase("event")) {
				trace.add(event);
				event = null;
				attributableStack.pop(); // remove self from top
			} else if (tagName.equalsIgnoreCase("trace")) {
				if (trace instanceof XTraceBufferedImpl) {
					((XTraceBufferedImpl) trace).consolidate();
				}
				log.add(trace);
				trace = null;
				attributableStack.pop(); // remove self from top
			} else if (tagName.equalsIgnoreCase("log")) {
				// add all extensions
				for (XExtension ext : extensions) {
					log.getExtensions().add(ext);
				}
				attributableStack.pop(); // remove self from top
			}
		}

	}

}
