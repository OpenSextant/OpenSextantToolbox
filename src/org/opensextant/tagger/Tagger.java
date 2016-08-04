package org.opensextant.tagger;

import java.io.File;
import java.net.URL;
import java.util.List;

public interface Tagger {

	Document tag(String content);

	Document tag(File content);

	Document tag(URL content);

	List<Match> match(String content);

	List<Match> match(File content);

	List<Match> match(URL content);

	void cleanup();

}