package org.freya.model;

import java.io.Serializable;

/**
 * This is a copy of the class gate.creole.ontology.URI with NO other
 * functionalities then just implementing Serializable; extending it wouldn't
 * work because all hierarchy still needs to be serializable..
 * 
 * @author danica
 */
public class SerializableURI implements Serializable {
  /**
   * Namespace for this URI (in current version - a value before the last
   * occurance of '#' or '/')
   */
  protected String namespace;

  /**
   * A Resource name (in current version - a value after the last occurance of
   * '#' or '/')
   */
  protected String aResourceName;

  /**
   * String representation of the URI
   */
  protected String uri;

  /**
   * Denotes whether the OResource this URI belongs to is an anonymous or not.
   */
  protected boolean isAnonymousResource;

  /**
   * Constructor
   * 
   * @param uri
   * @param isAnonymousResource
   * @throws Exception
   */
  public SerializableURI(String uri, boolean isAnonymousResource)
          throws RuntimeException {
    this.isAnonymousResource = isAnonymousResource;
    if(!this.isAnonymousResource) {
      int index = uri.lastIndexOf('#');
      if(index < 0) {
        index = uri.lastIndexOf('/');
        if(index < 0) throw new RuntimeException("Invalid URI :" + uri);
        // well in dbpedia they can end with // or / so comment out this for now
        // if (index + 2 > uri.length())
        // throw new Exception("Invalid URI :" + uri);
        this.uri = uri;
        this.namespace = uri.substring(0, index + 1);
        this.aResourceName = uri.substring(index + 1, uri.length());
        if(this.aResourceName == null || "".equals(this.aResourceName)) {
          // then remove / until you find text
          boolean localNameEmpty = true;
          while(localNameEmpty) {
            index = this.namespace.lastIndexOf('/');
            if(index > -1) {
              String newLocalName =
                      this.namespace.substring(index + 1, this.namespace
                              .length());
              if(newLocalName != null || !"".equals(newLocalName)) {
                localNameEmpty = false;
                this.namespace = uri.substring(0, index + 1);
                this.aResourceName = newLocalName;
                
                
              }
            } else {
              throw new RuntimeException("Invalid URI :" + uri);
            }
          }
        }
      } else {
        this.uri = uri;
        this.namespace = uri.substring(0, index + 1);
        this.aResourceName = uri.substring(index + 1, uri.length());
      }
    } else {
      this.uri = uri;
      this.namespace = "";
      this.aResourceName = "[" + uri + "]";
    }
  }

  /**
   * Retrieves the name space part from the URI. In this implementation it
   * retrieves the string that appears before the last occurance of '#' or '/'.
   * 
   * @return
   */
  public String getNameSpace() {
    return this.namespace;
  }

  /**
   * Retrieves the resource name from the given URI. In this implementation it
   * retrieves the string that appears after the last occurance of '#' or '/'.
   * 
   * @return
   */
  public String getResourceName() {
    return this.aResourceName;
  }

  /**
   * Returns the string representation of the uri. In case of anonymous class,
   * it returns the '[' + resourcename + ']'.
   */
  public String toString() {
    return this.uri;
  }

  /**
   * Indicates whether the URI refers to an anonymous resource
   * 
   * @return
   */
  public boolean isAnonymousResource() {
    return this.isAnonymousResource;
  }

  public boolean equals(Object other) {
    if(other instanceof SerializableURI) {
      return uri.equals(((SerializableURI)other).uri);
    } else {
      return false;
    }
  }

  public int hashCode() {
    return uri.hashCode();
  }

  public String toTurtle() {
    if(isAnonymousResource()) {
      if(uri.startsWith("_:")) {
        return uri;
      } else {
        return "_:" + uri;
      }
    } else {
      return "<" + uri + ">";
    }
  }

  public void validate() {
    throw new UnsupportedOperationException("Method not implemented");
  }

  public String toDisplayString() {
    throw new UnsupportedOperationException("Method not implemented");
  }

  public String toASCIIString() {
    throw new UnsupportedOperationException("Method not implemented");
  }
}
