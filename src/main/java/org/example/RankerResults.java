package org.example;
import java.util.*;
public class RankerResults {
    String link;
    String title;
    String description;
    List<String> snippets;
    String term;
    String id;
    private double score; // Add this field
    double pageRankScore;

    public RankerResults()
    {
    }
    public RankerResults(String s,String f,String g,List<String> ss)
    {
        link=s;
        title=f;
        description=g;
        this.snippets=ss;
    }
    public String getLink(){return link;}
    public String getTitle(){return title;}
    public String getDescription(){return description;}
    public List<String> getSnippets(){return snippets;}
    public String getTerm(){return term;}
    public String getId(){return id;}

    public void setSnippet(List<String> snippets){this.snippets = snippets;}

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