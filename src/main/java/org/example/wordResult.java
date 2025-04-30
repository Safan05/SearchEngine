package org.example;
import java.util.ArrayList;
import java.util.List;

public class wordResult {


    List<String> Links = new ArrayList<String>();
    List<String> Titles = new ArrayList<String>();
    List<String> Discreption = new ArrayList<String>();
    List<Double> TF = new ArrayList<Double>();
    List<List<Integer>> Positions = new ArrayList<List<Integer>>();
    List<List<Boolean>> Headers = new ArrayList<List<Boolean>>();
    List<Double> ranks = new ArrayList<Double>();
    Double idf;
    public void addValue (String s)
    {

        this.Links.add(s);

    }
    public void addranks (Double i)
    {

        this.ranks.add(i);

    }
    public void addTitle (String s)
    {

        this.Titles.add(s);

    }
    public void addDisc (String s)
    {

        this.Discreption.add(s);

    }
    public void addTF (Double s)
    {

        this.TF.add(s);

    }
    public void addPosition (List<Integer> pos)
    {

        this.Positions.add(pos);

    }

    public void addHeaders (List<Boolean> h)
    {

        this.Headers.add(h);

    }

    public void addIDF (Double d)

    {
        this.idf = d;

    }
    public List<String> getLinks() {
        return Links;
    }
    public List<String> getTitles() {
        return Titles;
    }
    public List<String> getDescriptions() {
        return Discreption;
    }
    public List<Double> getTF() {
        return TF;
    }
    public List<List<Integer>> getPositions() {
        return Positions;
    }
    public List<List<Boolean>> getHeaders() {
        return Headers;
    }
    public List<Double> getRanks() {
        return ranks;
    }
    public Double getIdf() {
        return idf;
    }
}
