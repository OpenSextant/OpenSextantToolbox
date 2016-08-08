package org.opensextant.tagger.regex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.opensextant.tagger.Match;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DebugPostProcessor extends PostProcessorBase {

	/** Log object. */
	private static final Logger LOGGER = LoggerFactory.getLogger(DebugPostProcessor.class);

	@Override
	public Comparator<Match> getComparator() {
		return new PositionComparator();
	}

	@Override
	public List<Match> decide(List<Match> inters) {
		List<Match> keepers = new ArrayList<Match>();

		if (inters != null && !inters.isEmpty()) {
			// sort the annotations by document order
			Collections.sort(inters, getComparator());

			// build up a message showing the alternative annotations
			StringBuilder tmp = new StringBuilder();
			tmp.append("Must decide amongst these ").append(inters.size()).append(" annotations\n");
			for (Match a : inters) {
				tmp.append("\t").append(a).append("\n");
			}
			tmp.append("-------\n");
			LOGGER.debug(tmp.toString());

			// return them all
			keepers.addAll(inters);
		}
		return keepers;
	}

}