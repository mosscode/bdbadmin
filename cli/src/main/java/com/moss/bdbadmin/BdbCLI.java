/**
 * Copyright (C) 2013, Moss Computing Inc.
 *
 * This file is part of bdbadmin.
 *
 * bdbadmin is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * bdbadmin is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with bdbadmin; see the file COPYING.  If not, write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole
 * combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent
 * modules, and to copy and distribute the resulting executable under
 * terms of your choice, provided that you also meet, for each linked
 * independent module, the terms and conditions of the license of that
 * module.  An independent module is a module which is not derived from
 * or based on this library.  If you modify this library, you may extend
 * this exception to your version of the library, but you are not
 * obligated to do so.  If you do not wish to do so, delete this
 * exception statement from your version.
 */
package com.moss.bdbadmin;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jline.Completor;
import jline.ConsoleReader;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

public final class BdbCLI {

	private final BdbConnection c;

	public BdbCLI(BdbConnection c) {
		this.c = c;
	}

	public void start() throws IOException {

		ConsoleReader reader = new ConsoleReader();
		reader.setBellEnabled(false);
		// reader.setDebug(new PrintWriter(new FileWriter("writer.debug", true)));
		reader.getHistory().setHistoryFile(new File(System.getProperty("user.home"), ".bdbadmin-history"));
		reader.setUseHistory(true);
		
		reader.addCompletor(new Completor() {
			public int complete(String buffer, int cursor, List candidates) {

				if (buffer == null || buffer.trim().length()==0) {
					candidates.add("quit");
					candidates.add("exit");
					candidates.add("history");
					candidates.add("help");
					candidates.add("ls");
					candidates.add("rm");
					Collections.sort(candidates);
				}
				else {

					String start = (buffer == null) ? "" : buffer;

					String[] pieces = start.split("\\s");

					if (pieces.length > 0) {

						if (pieces[0].equals("rm")) {
							if (pieces.length == 1) {
								for (String name : c.listDatabases()) {
									candidates.add(name);
								}
							}
							else if (pieces.length == 2) {
								for (String name : c.listDatabases()) {
									if (name.startsWith(pieces[1])) {
										candidates.add(name);
									}
								}
							}
						}
						else if (pieces[0].equals("ls")) {
							if (pieces.length == 1) {
								for (String name : c.listDatabases()) {
									candidates.add(name);
								}
							}
							else if (pieces.length == 2) {
								for (String name : c.listDatabases()) {
									if (name.startsWith(pieces[1])) {
										candidates.add(name);
									}
								}
							}
						}

						Collections.sort(candidates);
						return pieces[0].length() + 1;
					}
				}
				return 0;
			}
		});

		String line;
		final PrintWriter out = new PrintWriter(System.out);
		
		Runtime.getRuntime().addShutdownHook(new Thread("BdbAdminCleanupThread") {
			@Override
			public void run() {
				c.disconnect();
				out.write("\n>> Disconnected\n");
				out.flush();
			}
		});

		while ((line = reader.readLine("bdbadmin> ")) != null) {

			if (line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit")) {
				c.disconnect();
				break;
			}
			else if (line.equals("help")) {
				List<String> l = new ArrayList<String>();
				l.add("quit");
				l.add("exit");
				l.add("history");
				l.add("help");
				l.add("ls");
				l.add("rm");
				Collections.sort(l);
				for (String s : l) {
					out.write("    " + s + "\n");
				}
				out.flush();
			}
			else if (line.equals("clear")) {
				reader.clearScreen();
			}
			else if (line.equals("history")) {
				reader.flushConsole();
				for (String h : (List<String>)reader.getHistory().getHistoryList()) {
					out.write("    " + h + "\n");
				}
				out.flush();
			}
			else if ("ls".equals(line)) {
				for (String db : c.listDatabases()) {
					out.write("    " + db + "\n");
				}
				out.flush();
			}
			else if (line.startsWith("rm ")) {
				String[] pieces = line.split(" ");
				if (pieces.length != 2) {
					out.write(">> required syntax: rm [DATABASE NAME]\n");
					out.flush();
				}
				else {

					boolean found = false;
					for (String name : c.listDatabases()) {
						if (name.equals(pieces[1])) {
							found = true;
							break;
						}
					}

					if (!found) {
						out.write(">> database does not exist: " + pieces[1] + "\n");
						out.flush();
					}
					else {
						try {
							c.removeDatabase(pieces[1]);
						}
						catch (Exception ex) {
							out.write(">> error removing database: " + ex.getMessage() + "\n");
							out.flush();
						}
					}
				}
			}
			else {
				out.write(">> unknown command: " + line + "\n");
				out.flush();
			}
		}
	}

	private static void printHelp(OptionParser p) throws Exception {
		System.out.println("Usage: bdbadmin [OPTION]... [ENV_DIR]");
		System.out.println("Provides administrative access to BDB environments directly in the filesystem.");
		System.out.println();
		p.printHelpOn(System.out);
	}

	public static void main(String[] args) throws Exception {

		OptionParser p = new OptionParser();
		p.acceptsAll(Arrays.asList(new String[]{ "h", "help"}), "Prints this help menu.");
		p.acceptsAll(Arrays.asList(new String[]{ "r", "readonly"}), "Opens the environment as read-only.");

		OptionSet set = p.parse(args);
		if (set.has("help")) {
			printHelp(p);
			return;
		}

		File envDir;
		switch (set.nonOptionArguments().size()) {
		case 0:
			System.out.println("bdbadmin: no environment directory specified.");
			System.out.println("Try `bdbadmin --help' for more information.");
			return;
		default:
			envDir = new File(set.nonOptionArguments().get(0));
			break;
		}

		if (!envDir.exists()) {
			System.out.println("Directory does not exist: " + envDir);
			printHelp(p);
			return;
		}

		BdbConnection c = new BdbFileConnection(envDir, set.has("readonly"));
		BdbCLI cli = new BdbCLI(c);
		cli.start();
	}
}
