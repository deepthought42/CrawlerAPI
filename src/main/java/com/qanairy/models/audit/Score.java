package com.qanairy.models.audit;

import java.util.Set;

/**
 * Represents score as a combination of a score achieved and max possible score. This object also contains a set of
 * {@link Observation observations} that were experienced while generating score
 */
public class Score {

	private int points_achieved;
	private int max_possible_points;
	private Set<Observation> observations;
	
	public Score(int points, int max_points, Set<Observation> observations) {
		setPointsAchieved(points);
		setMaxPossiblePoints(max_points);
		setObservations(observations);
	}
	
	public int getPointsAchieved() {
		return points_achieved;
	}
	public void setPointsAchieved(int points_achieved) {
		this.points_achieved = points_achieved;
	}
	public int getMaxPossiblePoints() {
		return max_possible_points;
	}
	public void setMaxPossiblePoints(int max_possible_points) {
		this.max_possible_points = max_possible_points;
	}

	public Set<Observation> getObservations() {
		return observations;
	}

	public void setObservations(Set<Observation> observations) {
		this.observations = observations;
	}
}
