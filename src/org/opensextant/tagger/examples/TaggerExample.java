package org.opensextant.tagger.examples;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.opensextant.tagger.Document;
import org.opensextant.tagger.Match;
import org.opensextant.tagger.Tagger;
import org.opensextant.tagger.gate.GATETagger;

public class TaggerExample {

	public static void main(String[] args) {

		String taggerType = args[0];

		Properties props = null;

		try {
			props = new Properties();
			InputStream input = new FileInputStream(args[1]);
			// load properties file
			props.load(input);
		} catch (IOException e) {
			System.err.println("Could not load properties file from" + args[1]);
		}

		File docDir = new File(args[2]);
		File[] files = docDir.listFiles();
		int numDocs = files.length;

		// start time
		Long start = System.nanoTime();

		Tagger tagger = new GATETagger(taggerType, props);

		for (File f : files) {
			Document results = tagger.tag(f);
			List<? extends Match> matches = results.getMatchList();

			System.out.println(f.getName() + "has " + matches.size() + " matches");
		}

		// finish time
		Long end = System.nanoTime();

		// print some summary stats
		double totalDuration = (end - start) / 1000000000.0;
		double rate = numDocs / (totalDuration);

		System.out.println("\n\nDocument count=" + numDocs + "\n" + "Total time=" + totalDuration + "\n" + "Rate="
				+ rate + " documents/sec");

		tagger.cleanup();

	}

}
