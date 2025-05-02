package org.example;

public class RankerResults {
    String link;
    String title;
    String description;
    String snippet;
    String term;
    String id;
    private double score; // Add this field
    double pageRankScore;

    public RankerResults()
    {
    }
    public RankerResults(String s,String f,String g)
    {
        link=s;
        title=f;
        description=g;
    }
    public String getLink(){return link;}
    public String getTitle(){return title;}
    public String getDescription(){return description;}
    public String getSnippet(){return snippet;}
    public String getTerm(){return term;}
    public String getId(){return id;}

    public void setSnippet(String snippet){this.snippet = snippet;}

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
    public void setPageRankScore(double p){
        pageRankScore=p;
    }
}