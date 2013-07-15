package de.intranda.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Scanner;

import org.apache.commons.io.output.FileWriterWithEncoding;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * Utilities for conversion of text data between different formats
 * 
 * @author florian
 *
 */
public class DocumentUtils {
	
	private static final Logger logger = Logger.getLogger(DocumentUtils.class);
	public static final String encoding = "utf-8"; // "iso-8859-15"
	
	/**
	 * Read a text file and return content as String
	 * 
	 * @param file
	 * @param encoding
	 *            The character encoding to use. If null, a standard utf-8 encoding will be used
	 * @return
	 */
	public static String getStringFromFile(File file, String encoding) {
		String result = "";

		if (encoding == null)
			encoding = DocumentUtils.encoding;

		Scanner scanner = null;
		StringBuilder text = new StringBuilder();
		String NL = System.getProperty("line.separator");
		try {
			scanner = new Scanner(new FileInputStream(file), encoding);
			while (scanner.hasNextLine()) {
				text.append(scanner.nextLine() + NL);
			}
		} catch (FileNotFoundException e) {
			logger.error(e.toString());
		} finally {
			scanner.close();
		}
		result = text.toString();
		return result.trim();
	}

	/**
	 * Reads a String from a  byte array
	 * 
	 * @param bytes
	 * @param encoding
	 * @return
	 */
	public static String getStringFromByteArray(byte[] bytes, String encoding) {
		String result = "";

		if (encoding == null)
			encoding = DocumentUtils.encoding;

		Scanner scanner = null;
		StringBuilder text = new StringBuilder();
		String NL = System.getProperty("line.separator");
		try {
			scanner = new Scanner(new ByteArrayInputStream(bytes), encoding);
			while (scanner.hasNextLine()) {
				text.append(scanner.nextLine() + NL);
			}
		} finally {
			scanner.close();
		}
		result = text.toString();
		return result.trim();
	}

	/**
	 * Simply write a String into a text file.
	 * 
	 * @param string
	 *            The String to write
	 * @param file
	 *            The file to write to (will be created if it doesn't exist
	 * @param encoding
	 *            The character encoding to use. If null, a standard utf-8 encoding will be used
	 * @param append
	 *            Whether to append the text to an existing file (true), or to overwrite it (false)
	 * @return
	 * @throws IOException
	 */
	public static File getFileFromString(String string, File file, String encoding, boolean append) throws IOException {

		if (encoding == null)
			encoding = DocumentUtils.encoding;

		FileWriterWithEncoding writer = null;
		writer = new FileWriterWithEncoding(file, encoding, append);
		writer.write(string);
		if (writer != null)
			writer.close();

		return file;
	}

	/**
	 * Writes the Document doc into an xml File file
	 * 
	 * @param file
	 * @param doc
	 * @throws IOException
	 */
	public static void getFileFromDocument(File file, Document doc) throws IOException {
		getFileFromString(getStringFromDocument(doc, encoding), file, encoding, false);
	}

	/**
	 * 
	 * Creates a single String out of the Document document
	 * 
	 * @param document
	 * @param encoding
	 *            The character encoding to use. If null, a standard utf-8 encoding will be used
	 * @return
	 */
	public static String getStringFromDocument(Document document, String encoding) {
		if (document == null) {
			logger.warn("Trying to convert null document to String. Aborting");
			return null;
		}
		if (encoding == null)
			encoding = DocumentUtils.encoding;

		XMLOutputter outputter = new XMLOutputter();
		Format xmlFormat = outputter.getFormat();
		if (!(encoding == null) && !encoding.isEmpty())
			xmlFormat.setEncoding(encoding);
		// xmlFormat.setOmitDeclaration(true);
		xmlFormat.setExpandEmptyElements(true);
		outputter.setFormat(xmlFormat);
		String docString = outputter.outputString(document);

		return docString;
	}

	/**
	 * Load a jDOM document from an xml file
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 * @throws JDOMException
	 */
	public static Document getDocumentFromFile(File file) throws JDOMException, IOException {
		SAXBuilder builder = new SAXBuilder(false);
		Document document = null;

		document = builder.build(file);

		return document;
	}

	/**
	 * Create a jDOM document from an xml string
	 * 
	 * @param string
	 * @return
	 * @throws IOException 
	 * @throws JDOMException 
	 */
	public static Document getDocumentFromString(String string, String encoding) throws JDOMException, IOException {
		
		if(encoding==null) {
			encoding = DocumentUtils.encoding;
		}
		
		byte[] byteArray = null;
		try {
			byteArray = string.getBytes(encoding);
		} catch (UnsupportedEncodingException e1) {
		}
		ByteArrayInputStream baos = new ByteArrayInputStream(byteArray);

		// Reader reader = new StringReader(hOCRText);
		SAXBuilder builder = new SAXBuilder(false);
		Document document = null;

			document = builder.build(baos);

		return document;
	}

}
