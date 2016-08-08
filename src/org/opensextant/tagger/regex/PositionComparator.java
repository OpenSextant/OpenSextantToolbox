package org.opensextant.tagger.regex;

import java.util.Comparator;

public class PositionComparator implements Comparator<RegexMatch> {

	public int compare(RegexMatch a1, RegexMatch a2) {
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
