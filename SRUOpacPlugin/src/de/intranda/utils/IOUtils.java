package de.intranda.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import org.apache.log4j.Logger;

/**
 * General input/output utilities and file operations
 * 
 * @author florian
 *
 */
public class IOUtils {
	
	private static final Logger logger = Logger.getLogger(IOUtils.class);
	
	/**
	 * Writing serializable objects to a file
	 * 
	 * @param file
	 * @param obj
	 */
	public static void writeFile(File file, Object obj) {
		try {
			FileOutputStream fs = new FileOutputStream(file);
			ObjectOutputStream os = new ObjectOutputStream(fs);
			os.writeObject(obj);
			os.close();
		} catch (IOException e) {
			logger.error("Error writing binary file", e);
		}
	}

	/**
	 * Reading serializable objects from a file
	 * 
	 * @param file
	 * @return
	 */
	public static Object readFile(File file) {
		FileInputStream fis;
		Object obj = null;
		try {
			fis = new FileInputStream(file);
			ObjectInputStream ois = new ObjectInputStream(fis);
			obj = ois.readObject();
			ois.close();
		} catch (FileNotFoundException e) {
			logger.warn("No binary file exists to read. Aborting.");
		} catch (IOException e) {
			logger.error("Error reading binary file", e);
		} catch (ClassNotFoundException e) {
			logger.error("Error reading object from binary file", e);
		}
		return obj;
	}
	
	/**
	 * Moves a file to another directory, either by renaming it, or, failing that, by copying it and deleting the old file. *
	 * 
	 * @param sourceFile
	 *            The file to be moved
	 * @param destFile
	 *            The path to move the file to. If this denotes an existing directory, the file will be moved into this directory under the original
	 *            filename
	 * @param force
	 *            Overwrites any possibly existing old file, and creates directories if necessary
	 * @return
	 */
	public static void moveFile(File sourceFile, File destFile, boolean force) throws FileNotFoundException, IOException {

		String destFileName = null;
		File destDir = null;

		if (sourceFile == null || !sourceFile.isFile()) {
			throw new FileNotFoundException("Invalid source file specified");
		}

		if (destFile == null) {
			throw new FileNotFoundException("Invalid destination file specified");
		}

		if (destFile.isDirectory()) {
			destDir = destFile;
			destFileName = sourceFile.getName();
		} else {
			destDir = destFile.getParentFile();
			destFileName = destFile.getName();
		}

		if (destDir == null || !destDir.isDirectory()) {
			if (!force) {
				throw new FileNotFoundException("Invalid destination directory specified");
			} else {
				destDir.mkdirs();
				if (!destDir.isDirectory()) {
					throw new IOException("Unable to create destination file");
				}
			}
		}

		File targetFile = new File(destDir, destFileName);
		if (targetFile.isFile() && !force) {
			throw new IOException("Destination file already exists");
		} else {
			if (!sourceFile.renameTo(targetFile)) {
				// renaming failed, try copying and deleting
				if (targetFile.isFile()) {
					if (!targetFile.delete()) {
						throw new IOException("Unable to overwrite destination file");
					}
				}
				copyFile(sourceFile, targetFile);
				if (targetFile.exists()) {
					sourceFile.delete();
				} else {
					throw new IOException("Copy operation failed");
				}
			}
		}
	}

	/**
	 * Moves (either by simply renaming or by copy and delete) all files (and directories) within directory dir to another directory destDir
	 * 
	 * @param sourcedir
	 * @param destDir
	 * @param overwrite
	 *            Forces files that are already in the destDir to be overwritten by the corresponding file in sourceDir. If false, the file will not
	 *            be moved an remain in the sourceDir
	 * @return
	 */
	public static void moveDir(File sourcedir, File destDir, boolean overwrite) throws FileNotFoundException, IOException {
		if (sourcedir == null || !sourcedir.isDirectory()) {
			throw new FileNotFoundException("Cannot move from a nonexisting directory");
		}
		if (destDir.getAbsolutePath().startsWith(sourcedir.getAbsolutePath())) {
			throw new IOException("Cannot move into its own subdirectory");
		}
		File[] files = sourcedir.listFiles();

		if (files == null || files.length == 0) {

			// don't move if destDir already exists - the source Dir is empty anyway
			if (destDir != null && destDir.isDirectory()) {
				sourcedir.delete();
				return;
			}

			boolean success = false;
			try {
				success = sourcedir.renameTo(destDir);
			} catch (NullPointerException e) {
				throw new FileNotFoundException(e.getMessage());
			}
			if (!success) {
				if (destDir.mkdir()) {
					sourcedir.delete();
				} else {
					throw new IOException("Failed moving directory " + sourcedir.getAbsolutePath());
				}
			}
			return;
		}

		destDir.mkdirs();
		if (!destDir.isDirectory()) {
			throw new IOException("Failed creating destination directories");
		}
		for (File file : files) {
			if (file.isDirectory()) {
				moveDir(file, new File(destDir, file.getName()), overwrite);
			} else {
				File destFile = new File(destDir, file.getName());
				if (overwrite || !destFile.isFile()) {

					try {
						moveFile(file, destDir, overwrite);
					} catch (IOException e) {
						throw new IOException("Unable to move file " + file.getAbsolutePath() + " to directory " + destDir.getAbsolutePath());
					}
					// if (!file.renameTo(new File(destDir, file.getName()))) {
					// throw new IOException("Unable to move file " + file.getAbsolutePath() + " to directory " + destDir.getAbsolutePath());
					//
					// }
				}
			}
		}
		if (sourcedir.listFiles().length == 0) {
			sourcedir.delete();
		}
		return;
	}

	/**
	 * Deletes a directory with all included files and subdirectories. If the argument is a file, it will simply delete this
	 * 
	 * @param dir
	 */
	public static boolean deleteAllFiles(File dir) {
		if (dir == null) {
			return false;
		}
		if (dir.isFile()) {
			logger.error("Unable to delete directory " + dir.getAbsolutePath());
			return dir.delete();
		}
		boolean success = true;
		if (dir.isDirectory()) {
			File[] fileList = dir.listFiles();
			if (fileList != null) {
				for (File file : fileList) {
					if (file.isDirectory()) {
						if (!deleteAllFiles(file)) {
							logger.error("Unable to delete directory " + file.getAbsolutePath());
							success = false;
						}

					} else {
						if (!file.delete()) {
							logger.error("Unable to delete directory " + file.getAbsolutePath());
							success = false;
						}
					}
				}
			}
			if (!dir.delete()) {
				logger.error("Unable to delete directory " + dir.getAbsolutePath());
				success = false;
			}
		}
		return success;
	}

	/**
	 * Copies the content of file source to file dest
	 * 
	 * @param source
	 * @param dest
	 * @throws IOException
	 */
	public static void copyFile(File source, File dest) throws IOException {

		if (!dest.exists()) {
			dest.createNewFile();
		}
		InputStream in = null;
		OutputStream out = null;
		try {
			in = new FileInputStream(source);
			out = new FileOutputStream(dest);

			// Transfer bytes from in to out
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
		} finally {
			if(in!=null) {				
				in.close();
			}
			if(out!=null) {				
				out.close();
			}
		}
	}
	
	public static ArrayList<File> getAllFilesRecursive(File dir, FileFilter filter) {
		ArrayList<File> fileList = new ArrayList<File>();
		File[] subFiles = dir.listFiles(filter);
		if(dir == null || !dir.isDirectory() || subFiles == null || subFiles.length == 0) {
			return fileList;
		}
		
		for (File file : subFiles) {
			if(file.isFile()) {
				fileList.add(file);
			} else if(file.isDirectory()) {
				fileList.addAll(getAllFilesRecursive(file, filter));
			}
		}
		
		return fileList;
	}
	
	public static String getRelativePath(File file, File relativeParent) {
		String path = "";
		
		while(!relativeParent.getAbsolutePath().contentEquals(file.getAbsolutePath())) {
			String filename = file.getName();
			if(!file.isFile()) {
				filename = filename.concat("/");
			}
			path = filename.concat(path);
			file = file.getParentFile();
			if(file == null) {
				break;
			}
		}
		return path;
	}

}
