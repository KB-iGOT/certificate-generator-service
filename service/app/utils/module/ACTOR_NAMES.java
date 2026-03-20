package utils.module;


import org.sunbird.cert.actor.*;
import org.sunbird.health.actor.HealthActor;

public enum ACTOR_NAMES {
  HEALTH_ACTOR(HealthActor.class, "health-actor"),
  CERTIFICATE_GENERATOR_ACTOR(CertificateGeneratorActor.class, "certificate-generator_actor"),
  CERTIFICATE_BACKGROUND_ACTOR(CertBackgroundActor.class, "certificate_background_actor"),
  MILESTONE_ACHIEVEMENT_GENERATOR_ACTOR(MilestoneAchievementGeneratorActor.class, "milestone-achievement-generator_actor"),
  MILESTONE_ACHIEVEMENT_BACKGROUND_ACTOR(MilestoneAchievementBackgroundActor.class, "milestone_achievement_background_actor"),
  BADGE_GENERATOR_ACTOR(BadgeGeneratorActor.class, "badge_generator_actor"),
  BADGE_BACKGROUND_ACTOR(BadgeBackgroundActor.class, "badge_background_actor"),;


  ACTOR_NAMES(Class clazz, String name) {
    actorClass = clazz;
    actorName = name;
  }

  private Class actorClass;
  private String actorName;

  public Class getActorClass() {
    return actorClass;
  }

  public String getActorName() {
    return actorName;
  }
}
