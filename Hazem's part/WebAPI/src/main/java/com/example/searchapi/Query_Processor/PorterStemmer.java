package com.example.searchapi.Query_Processor;


public class PorterStemmer {
    private char[] b;
    private int i, j;
    
    public String stem(String word) {
        if (word.length() > 1) {
            return stem(word.toCharArray(), word.length());
        }
        return word;
    }
    
    public String stem(char[] word, int length) {
        b = word;
        i = length;
        
        // Step 1a: Handle plurals and past participles
        if (i > 0) {
            step1a();
            step1b();
            step1c();
            step2();
            step3();
            step4();
            step5a();
            step5b();
        }
        
        return new String(b, 0, i);
    }
    
    private void step1a() {
        if (ends("sses")) {
            i -= 2;
        } else if (ends("ies")) {
            i--;
        } else if (ends("ss")) {
            // Do nothing
        } else if (ends("s")) {
            i--;
        }
    }
    
    private void step1b() {
        if (ends("eed")) {
            if (m() > 0) {
                i--;
            }
        } else if ((ends("ed") || ends("ing")) && vowelInStem()) {
            i = j;
            if (ends("at")) {
                setto("ate");
            } else if (ends("bl")) {
                setto("ble");
            } else if (ends("iz")) {
                setto("ize");
            } else if (doubleConsonant(i - 1)) {
                i--;
                char ch = b[i];
                if (ch == 'l' || ch == 's' || ch == 'z') {
                    i++;
                }
            } else if (m() == 1 && cvc(i - 1)) {
                setto("e");
            }
        }
    }
    
    private void step1c() {
        if (ends("y") && vowelInStem()) {
            b[i - 1] = 'i';
        }
    }
    
    private void step2() {
        if (i == 0) return;
        switch (b[i - 1]) {
            case 'a':
                if (ends("ational")) { r("ate"); break; }
                if (ends("tional")) { r("tion"); break; }
                break;
            case 'c':
                if (ends("enci")) { r("ence"); break; }
                if (ends("anci")) { r("ance"); break; }
                break;
            case 'e':
                if (ends("izer")) { r("ize"); break; }
                break;
            case 'l':
                if (ends("bli")) { r("ble"); break; }
                if (ends("alli")) { r("al"); break; }
                if (ends("entli")) { r("ent"); break; }
                if (ends("eli")) { r("e"); break; }
                if (ends("ousli")) { r("ous"); break; }
                break;
            case 'o':
                if (ends("ization")) { r("ize"); break; }
                if (ends("ation")) { r("ate"); break; }
                if (ends("ator")) { r("ate"); break; }
                break;
            case 's':
                if (ends("alism")) { r("al"); break; }
                if (ends("iveness")) { r("ive"); break; }
                if (ends("fulness")) { r("ful"); break; }
                if (ends("ousness")) { r("ous"); break; }
                break;
            case 't':
                if (ends("aliti")) { r("al"); break; }
                if (ends("iviti")) { r("ive"); break; }
                if (ends("biliti")) { r("ble"); break; }
                break;
            case 'g':
                if (ends("logi")) { r("log"); break; }
                break;
        }
    }
    
    private void step3() {
        switch (b[i - 1]) {
            case 'e':
                if (ends("icate")) { r("ic"); break; }
                if (ends("ative")) { r(""); break; }
                if (ends("alize")) { r("al"); break; }
                break;
            case 'i':
                if (ends("iciti")) { r("ic"); break; }
                break;
            case 'l':
                if (ends("ical")) { r("ic"); break; }
                if (ends("ful")) { r(""); break; }
                break;
            case 's':
                if (ends("ness")) { r(""); break; }
                break;
        }
    }
    
    private void step4() {
        if (i == 0) return;
        switch (b[i - 1]) {
            case 'a':
                if (ends("al")) break;
                return;
            case 'c':
                if (ends("ance")) break;
                if (ends("ence")) break;
                return;
            case 'e':
                if (ends("er")) break;
                return;
            case 'i':
                if (ends("ic")) break;
                return;
            case 'l':
                if (ends("able")) break;
                if (ends("ible")) break;
                return;
            case 'n':
                if (ends("ant")) break;
                if (ends("ement")) break;
                if (ends("ment")) break;
                if (ends("ent")) break;
                return;
            case 'o':
                if (ends("ion") && (b[j] == 's' || b[j] == 't')) break;
                if (ends("ou")) break;
                return;
            case 's':
                if (ends("ism")) break;
                return;
            case 't':
                if (ends("ate")) break;
                if (ends("iti")) break;
                return;
            case 'u':
                if (ends("ous")) break;
                return;
            case 'v':
                if (ends("ive")) break;
                return;
            case 'z':
                if (ends("ize")) break;
                return;
            default:
                return;
        }
        if (m() > 1) {
            i = j;
        }
    }
    
    private void step5a() {
        j = i;
        if (ends("e")) {
            if (m() > 1 || (m() == 1 && !cvc(i - 1))) {
                i--;
            }
        }
    }
    
    private void step5b() {
        if (ends("l") && doubleConsonant(i - 1) && m() > 1) {
            i--;
        }
    }
    
    // Helper methods
    
    private boolean ends(String s) {
        int l = s.length();
        if (l > i) return false;
        int o = i - l;
        for (int j = 0; j < l; j++) {
            if (b[o + j] != s.charAt(j)) {
                return false;
            }
        }
        this.j = o - 1;
        return true;
    }
    
    private void setto(String s) {
        int l = s.length();
        int o = j + 1;
        for (int k = 0; k < l; k++) {
            b[o + k] = s.charAt(k);
        }
        i = j + 1 + l;
    }
    
    private void r(String s) {
        if (m() > 0) {
            setto(s);
        }
    }
    
    private int m() {
        int n = 0;
        int i = this.j + 1;
        while (true) {
            if (i > this.i) {
                return n;
            }
            if (!isConsonant(i)) {
                break;
            }
            i++;
        }
        i++;
        while (true) {
            while (true) {
                if (i > this.i) {
                    return n;
                }
                if (isConsonant(i)) {
                    break;
                }
                i++;
            }
            i++;
            n++;
            while (true) {
                if (i > this.i) {
                    return n;
                }
                if (!isConsonant(i)) {
                    break;
                }
                i++;
            }
            i++;
        }
    }
    
    private boolean vowelInStem() {
        for (int k = 0; k <= j; k++) {
            if (!isConsonant(k)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isConsonant(int index) {
        if (index < 0 || index >= b.length) return false;
        char ch = b[index];
        if (ch == 'a' || ch == 'e' || ch == 'i' || ch == 'o' || ch == 'u') {
            return false;
        }
        return ch != 'y' || (index == 0 || !isConsonant(index - 1));
    }
    
    private boolean doubleConsonant(int index) {
        if (index < 1 || index >= b.length) return false;
        return b[index] == b[index - 1] && isConsonant(index);
    }
    
    private boolean cvc(int index) {
        if (index < 2 || !isConsonant(index) || isConsonant(index - 1) || !isConsonant(index - 2)) {
            return false;
        }
        char ch = b[index];
        return ch != 'w' && ch != 'x' && ch != 'y';
    }
}