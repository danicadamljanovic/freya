/**
 * 
 */
package org.freya.model;

import java.io.Serializable;

/**
 * @author danica
 * 
 */
public interface InterpretationElement  extends Serializable{

	public Object getData() ;

	public void setData(Object data) ;

	public Long getEndToken() ;

	public void setEndToken(Long endToken);

	public Long getStartToken() ;

	public void setStartToken(Long startToken) ;

	public String toString();
	public boolean equals(InterpretationElement anotherObject);
}
