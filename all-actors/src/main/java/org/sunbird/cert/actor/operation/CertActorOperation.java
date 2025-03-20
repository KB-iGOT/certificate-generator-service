package org.sunbird.cert.actor.operation;

/**
 * All operation name related to certs service.
 * @author manzarul
 *
 */
public enum CertActorOperation {
	GENERATE_CERTIFICATE("generateCert");

	private String operation;

	CertActorOperation(String operation) {
		this.operation = operation;
	}

	public String getOperation() {
		return this.operation;
	}

}
