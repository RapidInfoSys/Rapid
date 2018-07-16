package com.rapid.update;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JFrame;

import com.rapid.utils.Files;
import com.rapid.utils.ZipFile;

public class Main {

	private static UpdateOutput _output;

	public static void main(String[] args) {

		// check if we have graphics support
		if (GraphicsEnvironment.isHeadless()) {

			// create the console output
			ConsoleOutput consoleOutput = new ConsoleOutput();

			// assign to the output object
			_output = consoleOutput;

		} else {

			// Create and set up the window.
	        JFrame frame = new JFrame("Rapid Update");
	        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

	        // create the gui output
	        GUIOutput guiOutput = new GUIOutput();

	        // Add content to the window.
	        frame.add(guiOutput);

	        // Display the window.
	        frame.pack();
	        frame.setVisible(true);

	        // assign to output object
	        _output = guiOutput;

		}

		try {

			// say we've started
			_output.log("Rapid Update started");

			_output.log("Identifying update location");

			// get the current path
			String path = ClassLoader.getSystemClassLoader().getResource(".").getPath();

			_output.log("Path is " + path);

			// url decode it (for spaces on windoze)
			path = URLDecoder.decode(path,"UTF-8");

			File location = new File (path).getParentFile();

			_output.log("Update location found");

			_output.log("Locating update");

			File updateFolder = null;
			for (File folder : location.listFiles()) {
				if (folder.getName().equals("update") && folder.isDirectory()) {
					updateFolder = folder;
					break;
				}
			}
			if (updateFolder == null) throw new Exception("'update' folder not found");

			File unzipFile = null;
			for (File file : updateFolder.listFiles()) {
				if (file.getName().endsWith(".war") || file.getName().endsWith(".zip")) {
					unzipFile = file;
				}
			}
			if (unzipFile == null) throw new Exception("update .war or .zip file not found");

			// a simple date formatter
			SimpleDateFormat df = new SimpleDateFormat("yyMMdd-HHmmss");

			// add current date and time to update folder
			updateFolder = new File(updateFolder + "/" + df.format(new Date()) );

			_output.log("Found update file " + unzipFile + ", unzipping to " + updateFolder);

			ZipFile zipFile = new ZipFile(unzipFile);

			zipFile.unZip(updateFolder);

			// the root will be the parent of the WEB-INF folder
			File root = location.getParentFile();

			// now loop the folders looking for ones we recognise and updating as per rules
			for (File file : updateFolder.listFiles()) {

				// identify the file
				if (file.isDirectory() && file.getName().equals("applications")) {

					// look for the rapid folder
					File rapidFolder = new File(file + "/rapid");
					// if we've found the rapid folder
					if (rapidFolder.exists() && rapidFolder.isDirectory()) {
						// log
						_output.log("Updating Rapid application web resources");
						// copy
						Files.copyFolder(rapidFolder, new File(root + "/applications/rapid"));
					}

				} else if (file.isDirectory() && file.getName().equals("WEB-INF")) {

					// list the subfolders of the WEB-INF folder
					for (File folder : file.listFiles()) {

						// we're only interested in visible folders - this spares the web.xml and update.jar
						if (folder.isDirectory() && !folder.isHidden()) {

							// if we've found the applications folder
							if (folder.getName().equals("applications")) {

								// look for the rapid folder - NOTE we are assuming update will always be version 1!!!!
								File rapidFolder = new File(folder + "/rapid/1");
								// if we've found the rapid folder
								if (rapidFolder.exists() && rapidFolder.isDirectory()) {

									// get the application.xml file
									File appXmlFile = new File(rapidFolder + "/application.xml");
									// get the update xml file
									File appXmlFileUpdate =  new File(root + "/WEB-INF/applications/rapid/1/application.xml");
									// if it exists, copy it
									if (appXmlFile.exists() && appXmlFileUpdate.exists()) {
										// log
										_output.log("Updating Rapid application xml file");
										// copy
										Files.copyFile(appXmlFile, appXmlFileUpdate);
									}

									// get the pages folder
									File pagesFolder = new File(rapidFolder + "/pages");
									// get the update pages folder
									File pagesFolderUpdate = new File(root + "/WEB-INF/applications/rapid/1/pages");
									// if it exists, copy it
									if (pagesFolder.exists() && pagesFolderUpdate.exists()) {
										// log
										_output.log("Updating Rapid application pages");
										// copy
										Files.copyFolder(pagesFolder, pagesFolderUpdate);
									}
								}

							} else if (folder.getName().equals("classes")) {

								// this is the classes folder - ignore any files in it's root as they are likely to be log4j config files

								// list the sub-folders of the classes folder
								for (File classFolder : folder.listFiles()) {

									// we're only interested in visible folders - this spares the files that may be in here like the log4j config file
									if (classFolder.isDirectory() && !classFolder.isHidden()) {

										// log that we're merging
										_output.log("Merging " + classFolder.getName() + " folder");

										// merge the classes sub-folders
										Files.copyFolder(classFolder, new File(root + "/WEB-INF/classes/" + classFolder.getName()));

									}

								}

							} else if (folder.getName().equals("update")) {

								// get just the update.jar file
								File updateJarFile = new File(folder + "/update.jar");
								// if we got one
								if (updateJarFile.exists()) {

									// log that we're copying
									_output.log("Copying update.jar file");

									// copy it
									Files.copyFile(updateJarFile, new File(root + "/WEB-INF/update/update.jar"));

								}

							} else if (folder.getName().equals("custombuild") ||
									folder.getName().equals("database") ||
									folder.getName().equals("devices") ||
									folder.getName().equals("logs") ||
									folder.getName().equals("processes") ||
									folder.getName().equals("temp")) {

								// log that we're ignoring
								_output.log("Ignoring " + folder.getName() + " folder");

							} else {

								// log that we're merging
								_output.log("Merging " + folder.getName() + " folder");

								// merge all other folders
								Files.copyFolder(folder, new File(root + "/WEB-INF/" + folder.getName()));

							}

						} else {

							// log that we're ignoring
							_output.log("Ignoring " + folder.getName() + " " + (folder.isDirectory() ? "folder" : "file"));

						} // directory / not hidden check

					}

				} else {

					// merge/copy all unrecognised folders/files

					if (file.isDirectory()) {

						_output.log("Merging " + file.getName() + " folder");

						Files.copyFolder(file, new File(root + "/" + file.getName()));

					} else {

						_output.log("Copying file " + file.getName());

						Files.copyFile(file, new File(root + "/" + file.getName()));

					}

				}
			}

			// finally some cleanup

			// get the logs folder
			File logsFolder = new File(root + "/WEB-INF/logs");
			// check it exists
			if (logsFolder.exists()) {
				_output.log("Clearing logs folder");
				// loop the files
				for (File file : logsFolder.listFiles()) {
					// delete
					Files.deleteRecurring(file);
				}
			}

			// get the temp folder
			File tempFolder = new File(root + "/WEB-INF/temp");
			// check it exists
			if (tempFolder.exists()) {
				_output.log("Clearing temp folder");
				// loop the files
				for (File file : tempFolder.listFiles()) {
					// delete
					Files.deleteRecurring(file);
				}
			}

			_output.log("Done!");

		} catch (Exception ex) {

			_output.log("Error : " + ex.getMessage());

		}

	}

}
