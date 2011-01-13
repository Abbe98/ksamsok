package se.raa.ksamsok.api.util;

/**
 * Enkel b�nklass f�r att h�lla info om relationer.
 * Obs att informationsk�lla (source) ej �r k�llan i relationen utan var relationen kom ifr�n,
 * tex null om den h�mtades direkt fr�n k�llobjektet eller "deduced" om den h�rleddes via
 * relationsinvers (mha originalRelationType). "K�llan" i relationen f�ruts�tts vara k�nd av
 * anv�ndaren av denna b�na.
 */
public class Relation {

	private String relationType;
	private String targetUri;
	private String source;
	private String originalRelationType;

	/**
	 * Skapa ny instans.
	 * @param relationType typ av relation
	 * @param targetUri uri som relationen g�r till
	 * @param source relationens informationsk�lla eller null
	 * @param originalRelationType
	 */
	public Relation(String relationType, String targetUri, String source, String originalRelationType) {
		this.relationType = relationType;
		this.targetUri = targetUri;
		this.source = source;
		this.originalRelationType = originalRelationType;
	}

	/**
	 * Ger relationstypens namn.
	 * @return relationstyp
	 */
	public String getRelationType() {
		return relationType;
	}

	/**
	 * Ger uri f�r objekt p� andra sidan av relationen.
	 * @return objekt-uri
	 */
	public String getTargetUri() {
		return targetUri;
	}

	/**
	 * Ger informationsk�lla.
	 * @return informationsk�lla
	 */
	public String getSource() {
		return source;
	}

	/**
	 * Ger ev orginalrelationstyp tex om relationen har h�rletts eller h�mtats fr�n manuellt inmatad info.
	 * @return orginalrelationstyp
	 */
	public String getOriginalRelationType() {
		return originalRelationType;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		// genererad av eclipse fr�n uri och relation
		// OBS bara f�r uri och relation, �vriga �r ointressanta vid denna j�mf�relse
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((relationType == null) ? 0 : relationType.hashCode());
		result = prime * result
				+ ((targetUri == null) ? 0 : targetUri.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		// genererad av eclipse fr�n uri och relation
		// OBS bara f�r uri och relation, �vriga �r ointressanta vid denna j�mf�relse
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Relation)) {
			return false;
		}
		Relation other = (Relation) obj;
		if (relationType == null) {
			if (other.relationType != null) {
				return false;
			}
		} else if (!relationType.equals(other.relationType)) {
			return false;
		}
		if (targetUri == null) {
			if (other.targetUri != null) {
				return false;
			}
		} else if (!targetUri.equals(other.targetUri)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "relation of type " + relationType + " to " + targetUri +
			" (source " + source + ", org type " + originalRelationType + ")";
	}
}
