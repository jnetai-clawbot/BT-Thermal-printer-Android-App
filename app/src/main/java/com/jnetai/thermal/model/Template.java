package com.jnetai.thermal.model;

import java.util.ArrayList;
import java.util.List;

public class Template {
    public String name;
    public String title = "";
    public int widthMm = 58;
    public boolean isLabel = false;
    public int labelWidthMm = 58;
    public int labelHeightMm = 40;
    public boolean logoEnabled = false;
    public String logoFile = "";
    public int feedBefore = 2;
    public int feedAfter = 4;
    public List<TemplateLine> lines = new ArrayList<>();

    public static class TemplateLine {
        public String kind = "text"; // text | items | spacer
        public String text = "";
        public int align = 0;        // 0 left 1 center 2 right
        public int size = 1;         // 1 normal 2 large 3 xl 4 xxl
        public boolean bold = false;
        public boolean underline = false;
        public boolean dash = false;
        public int spacerCount = 1;

        public TemplateLine cloneLine() {
            TemplateLine l = new TemplateLine();
            l.kind = kind;
            l.text = text;
            l.align = align;
            l.size = size;
            l.bold = bold;
            l.underline = underline;
            l.dash = dash;
            l.spacerCount = spacerCount;
            return l;
        }
    }

    public Template cloneTemplate() {
        Template t = new Template();
        t.name = name;
        t.title = title;
        t.widthMm = widthMm;
        t.isLabel = isLabel;
        t.labelWidthMm = labelWidthMm;
        t.labelHeightMm = labelHeightMm;
        t.logoEnabled = logoEnabled;
        t.logoFile = logoFile;
        t.feedBefore = feedBefore;
        t.feedAfter = feedAfter;
        for (TemplateLine l : lines) {
            t.lines.add(l.cloneLine());
        }
        return t;
    }
}