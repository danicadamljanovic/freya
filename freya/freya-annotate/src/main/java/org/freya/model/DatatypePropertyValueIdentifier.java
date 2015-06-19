package org.freya.model;

import java.io.Serializable;
import java.util.List;

/**
 * identifier for the datatype property value
 * 
 * @author danica
 * 
 */
public class DatatypePropertyValueIdentifier implements Serializable{

	private List<SerializableURI> instanceURIs;
	private String propertyUri;
	private String propertyValue;

	public List<SerializableURI> getInstanceURIs() {
		return instanceURIs;
	}

	public void setInstanceURIs(List<SerializableURI> instanceURIs) {
		this.instanceURIs = instanceURIs;
	}

	public String getPropertyUri() {
		return propertyUri;
	}

	public void setPropertyUri(String propertyName) {
		this.propertyUri = propertyName;
	}

	public String getPropertyValue() {
		return propertyValue;
	}

	public void setPropertyValue(String propertyValue) {
		this.propertyValue = propertyValue;
	}

	/**
	 * 
	 */
	public String toString() {
		StringBuffer result = new StringBuffer("");

		result.append(" Datatype property identifier: ").append(
				"\nInstanceURI: ").append(this.instanceURIs.toString()).append(
				"\nProperty URI: ").append(this.propertyUri).append(
				" \nProperty value: ").append(this.propertyValue).toString();
		return result.toString();

	}

	public boolean equals(DatatypePropertyValueIdentifier anotherObject) {
		if ((this.propertyValue != null)
				&& (anotherObject != null)
				&& (this.propertyValue == anotherObject.getPropertyValue())
				&& (this.propertyUri != null && this.propertyUri
						.equals(anotherObject.getPropertyUri()))
				&& (this.instanceURIs != null && this.instanceURIs != null 
						&& this.instanceURIs
						.toString().equals(
								anotherObject.getInstanceURIs().toString())))
			return true;
		else
			return false;
	}

}
