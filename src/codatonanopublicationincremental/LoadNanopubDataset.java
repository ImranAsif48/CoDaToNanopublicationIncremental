/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package codatonanopublicationincremental;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphMaker;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.impl.GraphMatcher;
import org.apache.jena.graph.impl.SimpleGraphMaker;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleStatement;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.op.topic.DefaultTopics;

/**
 *
 * @author IMRAN ASIF
 */
public class LoadNanopubDataset {
    
   public static Map<Nanopub, Graph[]> assertionProvGraphs = new HashMap();
   //public static Map<Nanopub, Graph> provenanceGraphs = new HashMap();
    
   public static Map CompareNanopubGraphs(Nanopub currentNanopub, String CurrentObservation)
   {
        GraphMaker graph = new SimpleGraphMaker();
        Graph currentAssertGraph = graph.createGraph();
        Graph currentProvGraph = graph.createGraph();

        for (Statement st : currentNanopub.getAssertion()) {
                Resource subj = st.getSubject();
                Resource pred = st.getPredicate();
                Value obj = st.getObject();

                Node s = NodeFactory.createLiteral(subj.stringValue());
                Node p = NodeFactory.createLiteral(pred.stringValue());
                Node o = NodeFactory.createLiteral(obj.stringValue());

                Triple triple = new Triple(s, p, o);
                currentAssertGraph.add(triple);
        }

        for (Statement st : currentNanopub.getProvenance()) {
                Resource subj = st.getSubject();
                Resource pred = st.getPredicate();
                Value obj = st.getObject();

                Node s;
                if(subj.stringValue().contains("#assertion"))
                {
                    s = NodeFactory.createLiteral("np_assertion");
                }
                else
                {
                    s = NodeFactory.createLiteral(subj.stringValue());
                }

                Node p = NodeFactory.createLiteral(pred.stringValue());
                Node o = NodeFactory.createLiteral(obj.stringValue());

                Triple triple = new Triple(s, p, o);
                currentProvGraph.add(triple);
        }
        
        DefaultTopics nanoTopic = new DefaultTopics("");
        Map<Nanopub, String> returnMap = new HashMap();
        
        for (Map.Entry<Nanopub, Graph[]> entry : assertionProvGraphs.entrySet()) {
            Nanopub np = entry.getKey();
            Graph[] g = entry.getValue();
            
            //nanoTopic.getTopic(np);
            // Compare assertions graphs
            if(GraphMatcher.equals(currentAssertGraph, g[0]) && GraphMatcher.equals(currentProvGraph, g[1]))
            {
                returnMap.put(np, "match");
                return returnMap;
            }
            else if(nanoTopic.getTopic(np).equals(CurrentObservation))
            {
                returnMap.put(np, "supersedes");
                return returnMap;
            }
         }
       
       returnMap.put(currentNanopub, "new");
       return returnMap;
   }
   
   public void SplitNanoIntoSeparateFiles(String trigFile) throws IOException, MalformedNanopubException {
        File tempFile = new File("dataset/"+trigFile);
        if(tempFile.exists())
        {
            String nanopubs = new String(Files.readAllBytes(Paths.get("dataset/"+trigFile))); 
            String[] splitNanos = nanopubs.split("@prefix this:");
            int count = 1;
            
            //////////////////////////////////////////////////////////
            // Delete all small trig files from trig_files directory
            File nanopubsDir = new File("trig_files");
//            if(nanopubsDir.list().length>0){
//                for (File file : nanopubsDir.listFiles())
//                {
//                   // Delete each file
//                   if (!file.delete())
//                   {
//                       // Failed to delete file
//                       System.out.println("Failed to delete "+file);
//                   }
//                } 
//            }
//            
//            //////////////////////////////////////////////////////////////////
//            // Generate new trig files from the big trig file
//            for (String nanopub : splitNanos)
//            {
//                if(!nanopub.equals(""))
//                {
//                    FileWriter myWriter = new FileWriter("trig_files/nano"+count+".trig");
//                    myWriter.write("@prefix this:" + nanopub);
//                    myWriter.close();
//                    count++;
//                }
//            }
            
            ////////////////////////////////////////////////////////////////////
            // Extract the nanopublication from the trig file
            System.out.println("Loading nanopubs dataset...");
            for (File npFile : nanopubsDir.listFiles()) {
                Nanopub np = new NanopubImpl(npFile);
                GraphMaker graph = new SimpleGraphMaker();
                Graph assertGraph = graph.createGraph();
                Graph provGraph = graph.createGraph();
                
                for (Statement st : np.getAssertion()) {
			Resource subj = st.getSubject();
			Resource pred = st.getPredicate();
                        Value obj = st.getObject();
                        
                        Node s = NodeFactory.createLiteral(subj.stringValue());
                        Node p = NodeFactory.createLiteral(pred.stringValue());
                        Node o;
                        
                        if(isDouble(obj.stringValue()))
                        {
                            o = NodeFactory.createLiteral(String.valueOf(Double.parseDouble(obj.stringValue())));
                        }
                        else
                        {
                            o = NodeFactory.createLiteral(obj.stringValue());
                        }

                        Triple triple = new Triple(s, p, o);
                        assertGraph.add(triple);
		}
                
                for (Statement st : np.getProvenance()) {
			Resource subj = st.getSubject();
			Resource pred = st.getPredicate();
                        Value obj = st.getObject();
                        
                        Node s;
                        if(subj.stringValue().contains("#assertion"))
                        {
                            s = NodeFactory.createLiteral("np_assertion");
                        }
                        else
                        {
                            s = NodeFactory.createLiteral(subj.stringValue());
                        }
                        
                        Node p = NodeFactory.createLiteral(pred.stringValue());
                        Node o;
                        
                        if(isInteger(obj.stringValue()))
                        {
                            o = NodeFactory.createLiteral(String.valueOf(Integer.parseInt(obj.stringValue())));
                        }
                        else if(isDouble(obj.stringValue()))
                        {
                            o = NodeFactory.createLiteral(String.valueOf(Double.parseDouble(obj.stringValue())));
                        }
                        else
                        {
                            o = NodeFactory.createLiteral(obj.stringValue());
                        }

                        Triple triple = new Triple(s, p, o);
                        provGraph.add(triple);
		}
                
                ////////////////////////////////////
                Graph[] graphs = new Graph[2];
                graphs[0] = assertGraph;
                graphs[1] = provGraph;
                
                assertionProvGraphs.put(np, graphs);
            }
            
            System.out.println("Successfully loaded previous version of CoDa nanopubs.");
        }
        else
        {
            System.out.println(trigFile + " file is not available in dataset directory.");
        }
    }
   
   private boolean isDouble(String str)
   {
       try {  
        Double.parseDouble(str);  
        return true;
      } catch(NumberFormatException e){  
        return false;  
      }  
   }
   
   private boolean isInteger(String str)
   {
       try {  
        Integer.parseInt(str);  
        return true;
      } catch(NumberFormatException e){  
        return false;  
      }  
   }
}
