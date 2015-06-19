/**
 * 
 */
package org.freya.analyser;


public class BacktrackEngine{
  public BacktrackEngine(int[] sizes){
    this.sizes = new int[sizes.length];
    System.arraycopy(sizes, 0, this.sizes, 0, sizes.length);
  }
  
  /**
   * returns the next set of indexes that follow the current set
   * @param currentIndexes
   * @return
   */
  public int[] nextIndexes(int[] currentIndexes){
    int[] result = new int[currentIndexes.length];
    if(sizes.length != currentIndexes.length) 
      throw new IllegalArgumentException("Index table and sizes table of different sizes!");
    //increment the highest index that we can increment
    int i = currentIndexes.length -1;
    while(i >= 0 && currentIndexes[i] >= sizes[i] -1){
      result[i] = 0;
      i--;
    }
    if(i < 0){
      //we finished the search space
      return null;
    }else{
      //increment the current index, copy all the previous ones
      result[i] = currentIndexes[i] + 1;
      for(i--; i >= 0; i--){
        result[i] = currentIndexes[i];
      }
    }
    return result;
  }
  
  
  /**
   * An array holding the sizes of the lists for each element
   */
  private int[] sizes;


  public int[] getSizes() {
    return sizes;
  }

  public void setSizes(int[] sizes) {
    this.sizes = sizes;
  }
}