package org.example;

public class RankerResults {
    String link;
    String title;
    String description;
    private double score; // Add this field
    public RankerResults()
    {
    }
    public RankerResults(String s,String f,String g)
    {
        link=s;
        title=f;
        description=g;
    }
    public void setDescription(String s){
        description=s;
    }
    public void setTitle(String s){
        title=s;
    }
    public void setLink(String s){
        link=s;
    }
    public void setScore(double s){
        score=s;
    }
    public double getScore(){
        return score;
    }

}