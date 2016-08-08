package org.opensextant.tagger.regex;

import java.util.Comparator;

import org.opensextant.tagger.Match;

public class PositionComparator implements Comparator<Match> {

	public int compare(Match a1, Match a2) {
		int result;

		// compare start
		result = Long.compare(a1.getStart(), a2.getStart());

		// if starts are equal compare ends, longest first
		if (result == 0) {
			result = Long.compare(a2.getEnd(), a1.getEnd());
		}

		return result;
	}

}
