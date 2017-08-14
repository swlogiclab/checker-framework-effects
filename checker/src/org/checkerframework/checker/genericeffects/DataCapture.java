package org.checkerframework.checker.genericeffects;

import com.sun.source.tree.TypeCastTree;
import org.checkerframework.javacutil.InternalUtils;

import java.io.*;
import java.util.HashMap;

/**
 * Created by rishi on 7/25/2017.
 */
//this class is only temporary and will deleted later
public class DataCapture {
    File f;
    FileWriter w;
    FileReader r;
    HashMap<String, Integer> m;
    String p;
    public DataCapture(String path)
    {
        p = path;
        try {
            m = new HashMap<String, Integer>();
            //readData(f);
            f = new File(path);
            w = new FileWriter(f);
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * keys include
     * byte->int
     * float->double
     * follow similar pattern...
     */
    private void writeData()
    {
        try {
            w = new FileWriter(f);
            w.write(formatMap(m));
            w.flush();
            w.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    //only would write the last strings
    public void writeData(TypeCastTree node)
    {
        try {
            w = new FileWriter(f);
            w.write(node.toString() + "\n");
            w.flush();
            w.close();
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    public void createData(String castTo, String beingCast)
    {
        String combined = beingCast+"->"+castTo;
        int val = 0;
        if (m.containsKey(combined)) {
            val = m.get(combined) + 1;
        } else {
            val = 1;
        }
        if((beingCast.equals("byte") || beingCast.equals("short") || beingCast.equals("int") || beingCast.equals("long") || beingCast.equals("float") || beingCast.equals("double"))
                && (castTo.equals("byte") || castTo.equals("short") || castTo.equals("int") || castTo.equals("long") || castTo.equals("float") || castTo.equals("double")))
            m.put(combined, val);
        writeData();
    }


    public void createData(TypeCastTree node)
    {
        String castTo = InternalUtils.typeOf(node.getType()).toString();
        String beingCast = InternalUtils.typeOf(node.getExpression()).toString();
        String combined = beingCast+"->"+castTo;//+"\t"+node.toString();
        int val = 0;
        if (m.containsKey(combined)) {
            val = m.get(combined) + 1;
        } else {
            val = 1;
        }
        if((beingCast.equals("byte") || beingCast.equals("short") || beingCast.equals("int") || beingCast.equals("long") || beingCast.equals("float") || beingCast.equals("double"))
                && (castTo.equals("byte") || castTo.equals("short") || castTo.equals("int") || castTo.equals("long") || castTo.equals("float") || castTo.equals("double")))
            m.put(combined, val);
        writeData();
    }

    public String formatMap(HashMap<String, Integer> mp)
    {
        String full = "";
        for(String k : mp.keySet())
        {
            int value = mp.get(k);
            full += k + "=" + value + "\n";
        }
        return full;
    }

    public void readData(File fl)
    {
        try {
            // Open the file
            HashMap<String, Integer> temp = new HashMap<String, Integer>();
            FileInputStream fstream = new FileInputStream(p);
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

            String strLine;

            //Read File Line By Line
            while ((strLine = br.readLine()) != null) {
                // Print the content on the console
                strLine.trim();
                int split = strLine.indexOf('=');
                String strKey = strLine.substring(0, split);
                int strVal = Integer.parseInt(strLine.substring(split+1));
                m.put(strKey, strVal);
            }

            //Close the input stream
            br.close();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    public void createData(String msg)
    {
        try {
            w.write(msg + "\n");
            w.flush();
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }
}
