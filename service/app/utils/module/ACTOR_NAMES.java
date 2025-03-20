package utils.module;


import org.sunbird.cert.actor.CertificateGeneratorActor;
import org.sunbird.health.actor.HealthActor;

public enum ACTOR_NAMES {
  HEALTH_ACTOR(HealthActor.class, "health-actor"),
  CERTIFICATE_GENERATOR_ACTOR(CertificateGeneratorActor.class, "certificate-generator_actor");


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
