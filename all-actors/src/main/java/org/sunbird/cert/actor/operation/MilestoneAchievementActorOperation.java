package org.sunbird.cert.actor.operation;

/**
 * All operation name related to milestone achievement service.
 * @author system
 *
 */
public enum MilestoneAchievementActorOperation {
	GENERATE_MILESTONE_ACHIEVEMENT("generateMilestoneAchievement");

	private String operation;

	MilestoneAchievementActorOperation(String operation) {
		this.operation = operation;
	}

	public String getOperation() {
		return this.operation;
	}

}

