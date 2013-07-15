package de.intranda.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;

/**
 * Utility class containing filters for several common file types
 * 
 * @author florian
 *
 */
public class Filters {
	public static FilenameFilter ZipFilter = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String name) {
			boolean validImage = false;
			if (name.endsWith("zip") || name.endsWith("ZIP")) {
				validImage = true;
			}
			return validImage;
		}
	};
	public static FilenameFilter PdfFilter = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String name) {
			boolean validImage = false;
			if (name.endsWith("pdf") || name.endsWith("PDF")) {
				validImage = true;
			}
			return validImage;
		}
	};
	public static FilenameFilter XmlFilter = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String name) {
			boolean validImage = false;
			// jpeg
			if (name.endsWith("xml") || name.endsWith("xml")) {
				validImage = true;
			}
			return validImage;
		}
	};
	public static FilenameFilter ImageFilter = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String name) {
			boolean validImage = false;
			// jpeg
			if (name.endsWith("jpg") || name.endsWith("JPG") || name.endsWith("jpeg") || name.endsWith("JPEG")) {
				validImage = true;
			}
			if (name.endsWith(".tif") || name.endsWith(".TIF")) {
				validImage = true;
			}
			// png
			if (name.endsWith(".png") || name.endsWith(".PNG")) {
				validImage = true;
			}
			// gif
			if (name.endsWith(".gif") || name.endsWith(".GIF")) {
				validImage = true;
			}
			// jpeg2000
			if (name.endsWith(".jp2") || name.endsWith(".JP2")) {
				validImage = true;
			}

			return validImage;
		}
	};

	// Filters for file searches
	public static FileFilter DirFilter = new FileFilter() {
		public boolean accept(File file) {
			return file.isDirectory();
		}
	};
	

	public static FilenameFilter TiffFilter = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String name) {
			boolean validImage = false;
			// tif
			if (name.endsWith("tif") || name.endsWith("TIF")) {
				validImage = true;
			}
			return validImage;
		}
	};
	public static FilenameFilter JpegFilter = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String name) {
			boolean validImage = false;
			if (name.endsWith("jpg") || name.endsWith("JPG")) {
				validImage = true;
			}
			if (name.endsWith("jpeg") || name.endsWith("JPEG")) {
				validImage = true;
			}
			return validImage;
		}
	};

	public static FilenameFilter TextFilter = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String name) {
			boolean validImage = false;
			// tif
			if (name.endsWith("txt") || name.endsWith("TXT")) {
				validImage = true;
			}
			return validImage;
		}
	};
}
