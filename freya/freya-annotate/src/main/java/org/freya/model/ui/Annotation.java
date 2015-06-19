package org.freya.model.ui;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Annotation {

    String text;
    Long startOffset;
    Long endOffset;
    Double score;
    String uri;
    String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Long getStartOffset() {
        return startOffset;
    }

    public void setStartOffset(Long startOffset) {
        this.startOffset = startOffset;
    }

    public Long getEndOffset() {
        return endOffset;
    }

    public void setEndOffset(Long endOffset) {
        this.endOffset = endOffset;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }
    
    @Override public String toString() {
        StringBuilder result = new StringBuilder();
        String NEW_LINE = System.getProperty("line.separator");

        result.append(" Annotation {" + NEW_LINE);
        result.append(" text: " + text + NEW_LINE);
        result.append(" start offset: " + startOffset + NEW_LINE);
        result.append(" end offset: " + endOffset + NEW_LINE );
        result.append(" URI: " + uri + NEW_LINE);
        result.append(" score: " + score + NEW_LINE);
        //Note that Collections and Maps also override toString
        result.append(" type: " + type + NEW_LINE);
        result.append("}");

        return result.toString();
      }
}
