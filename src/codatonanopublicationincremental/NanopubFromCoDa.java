/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package codatonanopublicationincremental;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.Map;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.Nanopub2Html;

import org.nanopub.NanopubCreator;
import org.nanopub.NanopubUtils;

/**
 *
 * @author IMRAN ASIF
 */
public class NanopubFromCoDa {
    
      IRI studyOneShot = null;
      IRI studyGroupSize = null;
      IRI studyMatchingProtocol = null;
      IRI studyGameIncentive = null;
      IRI studyExperimentalSetting = null;
      IRI studyNumberOfChoices = null;
      IRI studyKindex = null;
      IRI studyMPCR = null;
      IRI maleProportion = null;
      IRI yearOfDataCollection = null;
      
      
      String sparqlEndpoint = "";
      
    ////////////////////////////////////////////////////////////////////////////////////////////
    //
    public void GenerateNanopubsForMeasure(String measureType, String sparqlEndpoint) throws IOException, MalformedNanopubException
    {
        String measureTypeIRI = "";
        
        if(measureType.toLowerCase().equals("r"))
        {
            measureTypeIRI = "https://data.cooperationdatabank.org/id/esmeasure/r";
        }
        else if(measureType.toLowerCase().equals("d"))
        {
            measureTypeIRI = "https://data.cooperationdatabank.org/id/esmeasure/d";
        }
        else
        {
            return;
        }
        
        this.sparqlEndpoint = sparqlEndpoint;
        
        DateFormat dateFormat = new SimpleDateFormat("dd_MM_yyyy");
        Date current_date = new Date();
        String today = dateFormat.format(current_date);
        
        ArrayList<Nanopub> nanopublications = new ArrayList<Nanopub>();
        ArrayList<Nanopub> onlyNewNanopublications = new ArrayList<Nanopub>();
         
        int number = 1;
        int limit = 300;
        //for(int offset = 0; offset <= 8000; offset+=limit)
        for(int offset = 0; offset < 300; offset+=limit)
        {
            String query = "PREFIX dct: <http://purl.org/dc/terms/>\n" +
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                        "PREFIX cc: <https://data.cooperationdatabank.org/vocab/class/>\n" +
                        "PREFIX cp: <https://data.cooperationdatabank.org/vocab/prop/>\n" +
                        "PREFIX cr: <https://data.cooperationdatabank.org/id/>\n" +
                        "\n" +
                        "select *\n" +
                        "WHERE {\n" +
                        " ?study cp:reportsEffect ?obs.\n" +
                        "  ?obs a cc:Observation .\n" +
                        "  ?obs cp:eSEstimate ?effectsizeVal .\n" +
                        "  ?obs cp:effectSizeVariance ?var.\n" +
                        "  ?obs cp:eSmeasure <"+measureTypeIRI+"> .\n" +
                        "  ?obs cp:dependentVariable ?dv .\n" +
                        "  ?dv rdfs:label ?dep.\n" +
                        "  OPTIONAL {\n" +
                            " ?obs cp:effectSizeSampleSize ?sampleSize .\n" +
                            "  ?paper  cp:study ?study ;\n" +
                            "  cp:doi ?doi .\n" +
                        "  } " +
                        "}\n"
                  + "OFFSET "+ offset +"\n"
                  + "LIMIT "+ limit;
                 
            Repository repo = new SPARQLRepository(sparqlEndpoint);
            repo.initialize();
            
            System.out.println("Fetching RDF data from the CoDa SPARQL end point...");
            
            try (RepositoryConnection conn = repo.getConnection()) {
               TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
               try (TupleQueryResult result = tupleQuery.evaluate()) {
                   System.out.println("Successfully get all rdf data from the coda.");
                  while (result.hasNext()) {  // iterate over the result
                     try{
                      NanopubCreator npCreator = new NanopubCreator();
                      
                      NanopubCreator tempNpCreator = new NanopubCreator();

                       /// Add all prefixes for the nanopublication
                       String nanoName = new SimpleDateFormat("yyyy-MM-dd-HHmmss_SSS").format(new Date());
                       npCreator.addNamespace("this", "https://data.cooperationdatabank.org/coda/nanopub/nano"+number+"_"+nanoName+"#");
                       npCreator.addNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
                       npCreator.addNamespace("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
                       npCreator.addNamespace("orcid", "https://orcid.org/");
                       npCreator.addNamespace("prov", "http://www.w3.org/ns/prov#");
                       npCreator.addNamespace("dct", "http://purl.org/dc/terms/");
                       npCreator.addNamespace("pav", "http://purl.org/pav/");
                       npCreator.addNamespace("np", "http://www.nanopub.org/nschema#");
                       npCreator.addNamespace("xsd", "http://www.w3.org/2001/XMLSchema#");
                       npCreator.addNamespace("cc", "https://data.cooperationdatabank.org/vocab/class/");
                       npCreator.addNamespace("cp", "https://data.cooperationdatabank.org/vocab/prop/");
                       npCreator.addNamespace("cr", "https://data.cooperationdatabank.org/id/");
                       npCreator.addNamespace("npx", "http://purl.org/nanopub/x/");
                       
                       tempNpCreator.addNamespace("this", "https://data.cooperationdatabank.org/coda/nanopub/nano"+number+"_"+nanoName+"#");
                       tempNpCreator.addNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
                       tempNpCreator.addNamespace("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
                       tempNpCreator.addNamespace("orcid", "https://orcid.org/");
                       tempNpCreator.addNamespace("prov", "http://www.w3.org/ns/prov#");
                       tempNpCreator.addNamespace("dct", "http://purl.org/dc/terms/");
                       tempNpCreator.addNamespace("pav", "http://purl.org/pav/");
                       tempNpCreator.addNamespace("np", "http://www.nanopub.org/nschema#");
                       tempNpCreator.addNamespace("xsd", "http://www.w3.org/2001/XMLSchema#");
                       tempNpCreator.addNamespace("cc", "https://data.cooperationdatabank.org/vocab/class/");
                       tempNpCreator.addNamespace("cp", "https://data.cooperationdatabank.org/vocab/prop/");
                       tempNpCreator.addNamespace("cr", "https://data.cooperationdatabank.org/id/");
                       tempNpCreator.addNamespace("npx", "http://purl.org/nanopub/x/");


                      /////////////////////////////////////////////////
                      ValueFactory factory = SimpleValueFactory.getInstance();
                      IRI hasEffectValue = factory.createIRI("https://data.cooperationdatabank.org/vocab/prop/","eSEstimate");
                      IRI hasVarience = factory.createIRI("https://data.cooperationdatabank.org/vocab/prop/","effectSizeVariance");
                      IRI hasStdErr = factory.createIRI("https://data.cooperationdatabank.org/vocab/prop/","has-stderr");
                      IRI hasSampleSize = factory.createIRI("https://data.cooperationdatabank.org/vocab/prop/","effectSizeSampleSize");
                      IRI wasDerivedFrom = factory.createIRI("http://www.w3.org/ns/prov#","wasDerivedFrom");
                      //IRI dctDescription = factory.createIRI("http://purl.org/dc/terms/","description");
                      IRI paperDOI = factory.createIRI("https://data.cooperationdatabank.org/vocab/prop/","doi");
                      IRI createdBy = factory.createIRI("http://purl.org/pav/","createdBy");
                      IRI createdWith = factory.createIRI("http://purl.org/pav/","createdWith");
                      IRI lisence = factory.createIRI("http://purl.org/dc/terms/","lisence");
                      IRI createdOn = factory.createIRI("http://purl.org/pav/","createdOn");
                      IRI importedFrom = factory.createIRI("http://purl.org/pav/","importedFrom");
                      IRI thisNano = factory.createIRI("https://data.cooperationdatabank.org/coda/nanopub/nano"+number+"_"+nanoName+"#");
                      IRI assertionIRI = factory.createIRI(thisNano.stringValue(),"assertion");
                      IRI claimIndependentVariable = factory.createIRI("https://data.cooperationdatabank.org/vocab/prop/", "independentVariable");
                      IRI claimDependentVariable = factory.createIRI("https://data.cooperationdatabank.org/vocab/prop/", "dependentVariable");
                      IRI doiCreator = factory.createIRI("http://purl.org/dc/terms/", "creator");
                      IRI doiDate = factory.createIRI("http://purl.org/dc/terms/", "date");
                      IRI rdfsLabel = factory.createIRI("http://www.w3.org/2000/01/rdf-schema#", "label");
                      IRI supersedes = factory.createIRI("http://purl.org/nanopub/x/", "supersedes");
                      
                      /////////////////////////////////////////////////////////////////////////
                      /////////  More Provenance Info create predicate
                      studyOneShot = factory.createIRI("https://data.cooperationdatabank.org/vocab/prop/", "studyOneShot");
                      studyGroupSize = factory.createIRI("https://data.cooperationdatabank.org/vocab/prop/", "studyGroupSize");
                      studyMatchingProtocol = factory.createIRI("https://data.cooperationdatabank.org/vocab/prop/", "studyMatchingProtocol");
                      studyGameIncentive = factory.createIRI("https://data.cooperationdatabank.org/vocab/prop/", "studyGameIncentive");
                      studyExperimentalSetting = factory.createIRI("https://data.cooperationdatabank.org/vocab/prop/", "studyExperimentalSetting");
                      studyNumberOfChoices = factory.createIRI("https://data.cooperationdatabank.org/vocab/prop/", "studyNumberOfChoices");
                      studyKindex = factory.createIRI("https://data.cooperationdatabank.org/vocab/prop/", "studyKindex");
                      studyMPCR = factory.createIRI("https://data.cooperationdatabank.org/vocab/prop/", "studyMPCR");
                      maleProportion = factory.createIRI("https://data.cooperationdatabank.org/vocab/prop/", "maleProportion");
                      yearOfDataCollection = factory.createIRI("https://data.cooperationdatabank.org/vocab/prop/", "yearOfDataCollection");
                     
                      ///////////////////////////////////////////////////////////////////////
                     BindingSet bindingSet = result.next();
                     Value study = bindingSet.getValue("study");
                     Value paper = bindingSet.getValue("paper");
                     Value observation = bindingSet.getValue("obs");
                     Value doi = bindingSet.getValue("doi");
                     String effectVal = bindingSet.getValue("effectsizeVal").stringValue();
                     if(bindingSet.getValue("var").stringValue().toLowerCase().equals("inf"))
                        continue;
                     
                     double var = Double.parseDouble(bindingSet.getValue("var").stringValue());
                     double sampleSize = 0;
                     double standardErr = 0;
                     if(bindingSet.getValue("sampleSize")!=null)
                     {
                         sampleSize = Double.parseDouble(bindingSet.getValue("sampleSize").stringValue());
                         standardErr = Math.sqrt(var) / Math.sqrt(sampleSize);
                         Formatter fmt = new Formatter();
                         standardErr = Double.parseDouble(fmt.format("%.7f", standardErr).toString());
                     }

                     /////////////////////////////////////////////////////////////////////////
                     //// Getting independent variable from treatments.
                     String queryIV = "PREFIX cp: <https://data.cooperationdatabank.org/vocab/prop/>\n" +
                                       "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                                       "Select DISTINCT (group_concat(distinct ?iv; separator=\",\") as ?ivs) \n" +
                                       "WHERE {\n" +
                                       "  <"+ observation +"> cp:treatment ?t1, ?t2 .\n" +
                                       "  ?t1 ?iv ?o1. \n" +
                                       "  ?t2 ?iv ?o2.\n" +
                                       "  ?iv rdfs:subPropertyOf ?i. #only fetch IVs \n" +
                                       "  Filter (?o1 != ?o2).\n" +
                                       "  Filter (?t1 != ?t2). # filters duplicates\n" +
                                       "} ";
                     Value claimIVs = null;
                     Repository repoIV = new SPARQLRepository(sparqlEndpoint);
                     repoIV.initialize();
                     try (RepositoryConnection connIV = repoIV.getConnection()) {
                      TupleQuery tupleQueryIV = connIV.prepareTupleQuery(QueryLanguage.SPARQL, queryIV);
                       try (TupleQueryResult resultIV = tupleQueryIV.evaluate()) {
                          while (resultIV.hasNext()) {  // iterate over the result
                               BindingSet bindingSetIV = resultIV.next();
                               claimIVs = bindingSetIV.getValue("ivs");
                         }
                       }
                     }catch(Exception ex)
                     {
                         System.out.println(ex.getMessage() + "\n\n" + number );
                     }
                     repoIV.shutDown();

                     if(!claimIVs.stringValue().equals(""))
                     {
                         ///////////////////////////////////////////////////////
                           // Getting the provenance
                           Value creators = null;
                           Value date = null;
                           if(doi!=null)
                           {
                                   String queryProv = "PREFIX dct: <http://purl.org/dc/terms/>\n" +
                                   "\n" +
                                   "Select DISTINCT (group_concat(distinct ?name;separator=',')  as ?creators) ?date\n" +
                                   "WHERE {\n" +
                                   "   <"+ doi.toString() +"> dct:date ?date .\n" +
                                   "   <"+ doi.toString() +">  dct:creator ?creator .\n" +
                                   "   ?creator <http://xmlns.com/foaf/0.1/name> ?name .\n" +
                                   "}";


                                   Repository repoProv = new SPARQLRepository(sparqlEndpoint);
                                   repoProv.initialize();
                                  try (RepositoryConnection connProv = repoProv.getConnection()) {
                                    TupleQuery tupleQueryProv = connProv.prepareTupleQuery(QueryLanguage.SPARQL, queryProv);
                                    try (TupleQueryResult resultProv = tupleQueryProv.evaluate()) {
                                       while (resultProv.hasNext()) {  // iterate over the result
                                            BindingSet bindingSetProv = resultProv.next();
                                            creators = bindingSetProv.getValue("creators");
                                            date = bindingSetProv.getValue("date");
                                      }
                                    }
                                  }
                                  catch(Exception ex)
                                  {
                                      System.out.println(ex.getMessage() + "\n\n" + number );
                                      repoProv.shutDown();
                                  }
                                  repoProv.shutDown();
                           }

                         ////////////////////////////////////////////////////////////////////
                         Value claimDV  = bindingSet.getValue("dv");

                         //////////////////////////////////////////////////////////////////
                         Value effectSizeVal = factory.createLiteral(Double.parseDouble(effectVal));
                         Value varience = factory.createLiteral(var);
                         Value effectSampleSize, stderr;


                          //Now add the assertion
                         npCreator.addAssertionStatement((Resource)observation, hasEffectValue, effectSizeVal);
                         npCreator.addAssertionStatement((Resource)observation, hasVarience, varience);
                         
                         tempNpCreator.addAssertionStatement((Resource)observation, hasEffectValue, effectSizeVal);
                         tempNpCreator.addAssertionStatement((Resource)observation, hasVarience, varience);
                         if(bindingSet.getValue("sampleSize")!=null)
                         {
                             effectSampleSize = factory.createLiteral(sampleSize);
                             stderr = factory.createLiteral(standardErr);
                             npCreator.addAssertionStatement((Resource)observation, hasSampleSize, effectSampleSize);
                             npCreator.addAssertionStatement((Resource)observation, hasStdErr, stderr);
                             
                             tempNpCreator.addAssertionStatement((Resource)observation, hasSampleSize, effectSampleSize);
                             tempNpCreator.addAssertionStatement((Resource)observation, hasStdErr, stderr);
                         }

                         for(String iv : claimIVs.stringValue().split(","))
                         {
                             Value claimIV = null;
                             claimIV = factory.createLiteral(iv);

                             npCreator.addAssertionStatement((Resource)observation, claimIndependentVariable, claimIV);
                             
                             tempNpCreator.addAssertionStatement((Resource)observation, claimIndependentVariable, claimIV);
                         }

                         //Value claimDV = factory.createIRI(depententVariable);
                         npCreator.addAssertionStatement((Resource)observation, claimDependentVariable, claimDV);
                         
                         tempNpCreator.addAssertionStatement((Resource)observation, claimDependentVariable, claimDV);
                         //npCreator.addAssertionStatement((Resource)observation, dctDescription, description);

                         npCreator.setAssertionUri(assertionIRI);
                         tempNpCreator.setAssertionUri(assertionIRI);

                         //Provenace
                         npCreator.addProvenanceStatement(wasDerivedFrom, study);
                         tempNpCreator.addProvenanceStatement(wasDerivedFrom, study);

                         if(doi!=null)
                         {
                           npCreator.addProvenanceStatement(paperDOI, doi);
                           tempNpCreator.addProvenanceStatement(paperDOI, doi);
                         }
                         if(creators!=null)
                         {
                             if(!creators.stringValue().equals(""))
                             {
                                 for(String creator : creators.stringValue().split(","))
                                 {
                                    npCreator.addProvenanceStatement(doiCreator, factory.createLiteral(creator));
                                    tempNpCreator.addProvenanceStatement(doiCreator, factory.createLiteral(creator));
                                 }
                             }
                         }
                         if(date!=null)
                         {
                             npCreator.addProvenanceStatement(doiDate, date);
                             tempNpCreator.addProvenanceStatement(doiDate, date);
                         }
                         
                         /////////////////////////////////////////////////////////////////////////
                         // Provenance Graph info
                        //// Getting independent variable from treatments.
                        
                        populateProvGraph(npCreator, tempNpCreator, study, factory, rdfsLabel);

                        //////////////////////////////////////////////////////////////////////////
                        //Publication Information
                          npCreator.setNanopubUri(thisNano);
                          npCreator.addPubinfoStatement(createdOn, factory.createLiteral(new Date()));
                          npCreator.addPubinfoStatement(createdBy, factory.createIRI("http://orcid.org/0000-0002-1144-6265"));
                          npCreator.addPubinfoStatement(createdWith, factory.createIRI("https://github.com/ImranAsif48/CodaToNanopub"));
                          npCreator.addPubinfoStatement(lisence, factory.createIRI("https://creativecommons.org/licenses/by-sa/3.0/"));
                          npCreator.addPubinfoStatement(importedFrom, factory.createLiteral("CoDa Version", XMLSchema.STRING));
                          
                          tempNpCreator.setNanopubUri(thisNano);
                          tempNpCreator.addPubinfoStatement(createdOn, factory.createLiteral(new Date()));
                          tempNpCreator.addPubinfoStatement(createdBy, factory.createIRI("http://orcid.org/0000-0002-1144-6265"));
                          tempNpCreator.addPubinfoStatement(createdWith, factory.createIRI("https://github.com/ImranAsif48/CodaToNanopub"));
                          tempNpCreator.addPubinfoStatement(lisence, factory.createIRI("https://creativecommons.org/licenses/by-sa/3.0/"));
                          tempNpCreator.addPubinfoStatement(importedFrom, factory.createLiteral("CoDa Version", XMLSchema.STRING));

                          /////////////////
                          // Add to list
                          // Compare the assertion and provenance graphs
//                          Nanopub np_current = tempNpCreator.finalizeNanopub();
//                          Map<Nanopub, String> returnMap = LoadNanopubDataset.CompareNanopubGraphs(np_current, observation.stringValue());
//                          Map.Entry<Nanopub, String> entry = returnMap.entrySet().iterator().next();
//                          if(entry.getValue().equals("match"))
//                          {
//                              nanopublications.add(entry.getKey());
//                          }
//                          else if(entry.getValue().equals("supersedes"))
//                          {
//                              Nanopub old_np = entry.getKey();
//                              
//                              npCreator.addPubinfoStatement(supersedes, old_np.getUri());
//                              nanopublications.add(npCreator.finalizeNanopub());
//                              onlyNewNanopublications.add(npCreator.finalizeNanopub());
//                          }
//                          if(entry.getValue().equals("new"))
//                          {
//                              nanopublications.add(npCreator.finalizeNanopub());
//                              onlyNewNanopublications.add(npCreator.finalizeNanopub());
//                          }
                         
                          nanopublications.add(npCreator.finalizeNanopub());
                          
                          
//                          if(number == 505)
//                          {
//                              int zzz = 0;
//                          }

                          number++;
                     }
                     else{
                         //System.out.println(observation.stringValue());
                     }
                   }
                     catch(Exception ex)
                     {
                         System.out.println(ex.getMessage() + "\n\n" + number );
                     }
                  }
               }
            }catch(Exception ex)
            {
                System.out.println(ex.getMessage() + "\n\n" + number );
                repo.shutDown();
            }
            repo.shutDown();
        }
        
            ////////////////////////////////////////////////////////////
           // Now all Nanopublication in trig file
           int counter = 1;
           String fileName = "dataset/nanopubs_"+measureType+"_full_"+today+".trig";
           String folderName = "np_html_"+measureType+"_full_"+today+"/";
           
           Path p = Paths.get(folderName);
           Files.createDirectory(p);
           OutputStream out = new FileOutputStream(fileName);
           
           System.out.println("Now saving all " + nanopublications.size() +" nanopublications to " + fileName + " file...");
           for(Nanopub np : nanopublications)
           {
                //Nanopub np = creator.finalizeNanopub();
                NanopubUtils.writeToStream(np, out, RDFFormat.TRIG);

                if(counter % 1000 == 0)
                {
                    System.out.println(counter+" nanopublications have been processed out of " + nanopublications.size());
                }
                ////////////////////////////////////////////////////////////////////////////////////////
                // The following code is used to generate the HTML format of nanopublications
                String[] splitIRI = np.getUri().stringValue().split("/");
                String nanoFileName = splitIRI[splitIRI.length - 1];
                OutputStream outHTML = new FileOutputStream(folderName+nanoFileName+".html");
                Nanopub2Html.createHtml(np, outHTML, true, true);
                outHTML.close();
               ///////////////////////////////////////////////////////////////////////////////////////////
               counter++;
           }
           out.close();
           
            // Now only new Nanopublications in trig file
           counter = 1;
           fileName = "dataset/nanopubs_"+measureType+"_new_"+today+".trig";
           folderName = "np_html_"+measureType+"_new_"+today+"/";
           
           Path pNew = Paths.get(folderName);
           Files.createDirectory(pNew);
           out = new FileOutputStream(fileName);
           
           System.out.println("Now saving only new " + onlyNewNanopublications.size() +" nanopublications to " + fileName + " file...");
           for(Nanopub np : onlyNewNanopublications)
           {
                //Nanopub np = creator.finalizeNanopub();
                NanopubUtils.writeToStream(np, out, RDFFormat.TRIG);

                if(counter % 1000 == 0)
                {
                    System.out.println(counter+" nanopublications have been processed out of " + nanopublications.size());
                }
                ////////////////////////////////////////////////////////////////////////////////////////
                // The following code is used to generate the HTML format of nanopublications
                String[] splitIRI = np.getUri().stringValue().split("/");
                String nanoFileName = splitIRI[splitIRI.length - 1];
                OutputStream outHTML = new FileOutputStream(folderName+nanoFileName+".html");
                Nanopub2Html.createHtml(np, outHTML, true, true);
                outHTML.close();
               ///////////////////////////////////////////////////////////////////////////////////////////
               counter++;
           }
           out.close();
           System.out.println("\n\nAll nanopublications have been processed.");
    }

    private void populateProvGraph(NanopubCreator npCreator, NanopubCreator tempNpCreator, Value study, ValueFactory factory, IRI rdfsLabel) {
        String queryProv = "PREFIX cp: <https://data.cooperationdatabank.org/vocab/prop/>\n" +
                            "PREFIX cr: <https://data.cooperationdatabank.org/id/>\n" +
                            "\n" +
                            "select ?oneShot ?oneShotLabel (group_concat(distinct ?groupSize;separator=',')  as ?groupSize) "
                            + "?matching ?matchingLabel\n" +
                            "?gameIncentive ?gameIncentiveLabel ?Experimental ?ExperimentalLabel ?numberOfChoices "
                            + "?kIndex (group_concat(distinct ?mpcr;separator=',')  as ?mpcr) \n" +
                            " ?maleProportion ?yearOfDataCollection\n" +
                            " ?country ?countryLabel \n" +
                            " ?deception \n" +
                            " ?discussion ?discussionLabel \n" +
                            " ?meanAge ?numberOfObservations ?overallMeanContributions ?overallMeanWithdrawal \n" +
                            " ?overallN ?overallPercentageEndowmentContributed ?overallProportionCooperation \n" +
                            " ?overallStandardDeviation \n" +
                            " ?publicationStatus ?publicationStatusLabel \n" +
                            " ?recruitmentMethod ?recruitmentMethodLabel \n" +
                            " (group_concat(distinct ?replenishmentRate;separator=',') as ?replenishmentRates ) \n" +
                            " ?sanction \n" +
                            " ?studyAcademicDiscipline ?studyAcademicDisciplineLabel \n" +
                            " ?studyAcquaintance ?studyAcquaintanceLabel \n" +
                            " ?studyContinuousPGG ?studyContinuousPGGLabel \n" +
                            " ?studyDilemmaType ?studyDilemmaTypeLabel \n" +
                            " ?studyKnownEndgame ?studyOneShotRepeated \n" +
                            " (group_concat(distinct ?studyPGDThreshold;separator=',') as ?studyPGDThresholds ) \n" +
                            " ?studyRealPartner ?studyRealPartnerLabel \n" +
                            " ?studySequentiality ?studySequentialityLabel \n" +
                            " ?studyShowUpFee ?studyShowUpFeeLabel  \n" +
                            " ?studyStudentSample ?studySymmetric ?studyOtherDilemmaType \n" +
                            " ?studyTrialOfCooperation ?studyTrialOfCooperationLabel \n" +
                                "WHERE {\n" +
                                "  OPTIONAL { <"+study+"> cp:studyOneShot ?oneShot . \n"
                                + " ?oneShot  rdfs:label ?oneShotLabel .}\n" +
                                "  OPTIONAL { <"+study+"> cp:studyGroupSize ?groupSize . }\n" +
                                "  OPTIONAL { <"+study+"> cp:studyMatchingProtocol ?matching . \n"
                                + " ?matching  rdfs:label ?matchingLabel .}\n" +
                                "  OPTIONAL { <"+study+"> cp:studyGameIncentive ?gameIncentive . \n"
                                + " ?gameIncentive  rdfs:label ?gameIncentiveLabel .}\n" +
                                "  OPTIONAL { <"+study+"> cp:studyExperimentalSetting ?Experimental . \n"
                                + " ?Experimental  rdfs:label ?ExperimentalLabel .}\n" +
                                "  OPTIONAL { <"+study+"> cp:studyNumberOfChoices ?numberOfChoices . }\n" +
                                "  OPTIONAL { <"+study+"> cp:studyKindex ?kIndex . }\n" +
                                "  OPTIONAL { <"+study+"> cp:studyMPCR ?mpcr . }\n" +
                                "  OPTIONAL { <"+study+"> cp:maleProportion ?maleProportion . } \n" +
                                "  OPTIONAL { <"+study+"> cp:yearOfDataCollection ?yearOfDataCollection . }\n" +
                                "  OPTIONAL { <"+study+"> cp:country ?country . ?country  rdfs:label ?countryLabel . \n" +
                                "  OPTIONAL { <"+study+"> cp:deception ?deception . \n" +
                                "  OPTIONAL { <"+study+"> cp:discussion ?discussion . ?discussion  rdfs:label ?discussionLabel . \n" +
                                "  OPTIONAL { <"+study+"> cp:meanAge ?meanAge . \n" +
                                "  OPTIONAL { <"+study+"> cp:numberOfObservations ?numberOfObservations . \n" +
                                "  OPTIONAL { <"+study+"> cp:overallMeanContributions ?overallMeanContributions . \n" +
                                "  OPTIONAL { <"+study+"> cp:overallMeanWithdrawal ?overallMeanWithdrawal . \n" +
                                "  OPTIONAL { <"+study+"> cp:overallN ?overallN . \n" +
                                "  OPTIONAL { <"+study+"> cp:overallPercentageEndowmentContributed ?overallPercentageEndowmentContributed . \n" +
                                "  OPTIONAL { <"+study+"> cp:overallProportionCooperation ?overallProportionCooperation . \n" +
                                "  OPTIONAL { <"+study+"> cp:overallStandardDeviation ?overallStandardDeviation . \n" +
                                "  OPTIONAL { <"+study+"> cp:publicationStatus ?publicationStatus . ?publicationStatus  rdfs:label ?publicationStatusLabel . \n" +
                                "  OPTIONAL { <"+study+"> cp:recruitmentMethod ?recruitmentMethod . ?recruitmentMethod  rdfs:label ?recruitmentMethodLabel . \n" +
                                "  OPTIONAL { <"+study+"> cp:replenishmentRate ?replenishmentRate . \n" +
                                "  OPTIONAL { <"+study+"> cp:sanction ?sanction . \n" +
                                "  OPTIONAL { <"+study+"> cp:studyAcademicDiscipline ?studyAcademicDiscipline . ?studyAcademicDiscipline  rdfs:label ?studyAcademicDisciplineLabel . \n" +
                                "  OPTIONAL { <"+study+"> cp:studyAcquaintance ?studyAcquaintance . ?studyAcquaintance  rdfs:label ?studyAcquaintanceLabel . \n" +
                                "  OPTIONAL { <"+study+"> cp:studyContinuousPGG ?studyContinuousPGG . ?studyContinuousPGG  rdfs:label ?studyContinuousPGGLabel . \n" +
                                "  OPTIONAL { <"+study+"> cp:studyDilemmaType ?studyDilemmaType . ?studyDilemmaType  rdfs:label ?studyDilemmaTypeLabel . \n" +
                                "  OPTIONAL { <"+study+"> cp:studyKnownEndgame ?studyKnownEndgame . \n" +
                                "  OPTIONAL { <"+study+"> cp:studyOneShotRepeated ?studyOneShotRepeated . \n" +
                                "  OPTIONAL { <"+study+"> cp:studyPGDThreshold ?studyPGDThreshold . \n" +
                                "  OPTIONAL { <"+study+"> cp:studyRealPartner ?studyRealPartner . ?studyRealPartner rdfs:label ?studyRealPartnerLabel . \n" +
                                "  OPTIONAL { <"+study+"> cp:studySequentiality ?studySequentiality . ?studySequentiality rdfs:label ?studySequentialityLabel . \n" +
                                "  OPTIONAL { <"+study+"> cp:studyShowUpFee ?studyShowUpFee . ?studyShowUpFee rdfs:label ?studyShowUpFeeLabel . \n" +
                                "  OPTIONAL { <"+study+"> cp:studyStudentSample ?studyStudentSample . \n" +
                                "  OPTIONAL { <"+study+"> cp:studySymmetric ?studySymmetric . \n" +
                                "  OPTIONAL { <"+study+"> cp:studyTrialOfCooperation ?studyTrialOfCooperation . ?studyTrialOfCooperation rdfs:label ?studyTrialOfCooperationLabel . \n" +
                                "  OPTIONAL { <"+study+"> cp:studyOtherDilemmaType ?studyOtherDilemmaType . \n" +
                            "} ";
        
        Repository repoProv = new SPARQLRepository(sparqlEndpoint);
        repoProv.initialize();
        try (RepositoryConnection connProv = repoProv.getConnection()) {
         TupleQuery tupleQueryProv = connProv.prepareTupleQuery(QueryLanguage.SPARQL, queryProv);
          try (TupleQueryResult resultProv = tupleQueryProv.evaluate()) {
             while (resultProv.hasNext()) {  // iterate over the result
                  BindingSet bindingSetProv = resultProv.next();
                  if(bindingSetProv.getValue("oneShot") != null)
                  {
                      npCreator.addProvenanceStatement(studyOneShot, bindingSetProv.getValue("oneShot"));
                      npCreator.addProvenanceStatement((Resource)bindingSetProv.getValue("oneShot"), rdfsLabel, bindingSetProv.getValue("oneShotLabel"));

                      tempNpCreator.addProvenanceStatement(studyOneShot, bindingSetProv.getValue("oneShot"));
                      tempNpCreator.addProvenanceStatement((Resource)bindingSetProv.getValue("oneShot"), rdfsLabel, bindingSetProv.getValue("oneShotLabel"));
                  }


                  if(bindingSetProv.getValue("groupSize") != null)
                  {
                      if(!bindingSetProv.getValue("groupSize").stringValue().equals(""))
                      {
                          for(String groupSize : bindingSetProv.getValue("groupSize").stringValue().split(","))
                          {
                            npCreator.addProvenanceStatement(studyGroupSize, factory.createLiteral(Integer.parseInt(groupSize.trim())));
                            tempNpCreator.addProvenanceStatement(studyGroupSize, factory.createLiteral(Integer.parseInt(groupSize.trim())));
                          }
                      }
                  }

                  if(bindingSetProv.getValue("matching") != null)
                  {
                      npCreator.addProvenanceStatement(studyMatchingProtocol, bindingSetProv.getValue("matching"));
                      npCreator.addProvenanceStatement((Resource)bindingSetProv.getValue("matching"), rdfsLabel, bindingSetProv.getValue("matchingLabel"));

                      tempNpCreator.addProvenanceStatement(studyMatchingProtocol, bindingSetProv.getValue("matching"));
                      tempNpCreator.addProvenanceStatement((Resource)bindingSetProv.getValue("matching"), rdfsLabel, bindingSetProv.getValue("matchingLabel"));
                  }

                  if(bindingSetProv.getValue("gameIncentive") != null)
                  {
                      npCreator.addProvenanceStatement(studyGameIncentive, bindingSetProv.getValue("gameIncentive"));
                      npCreator.addProvenanceStatement((Resource)bindingSetProv.getValue("gameIncentive"), rdfsLabel, bindingSetProv.getValue("gameIncentiveLabel"));

                      tempNpCreator.addProvenanceStatement(studyGameIncentive, bindingSetProv.getValue("gameIncentive"));
                      tempNpCreator.addProvenanceStatement((Resource)bindingSetProv.getValue("gameIncentive"), rdfsLabel, bindingSetProv.getValue("gameIncentiveLabel"));
                  }

                  if(bindingSetProv.getValue("Experimental") != null)
                  {
                    npCreator.addProvenanceStatement(studyExperimentalSetting, bindingSetProv.getValue("Experimental"));
                    npCreator.addProvenanceStatement((Resource)bindingSetProv.getValue("Experimental"), rdfsLabel, bindingSetProv.getValue("ExperimentalLabel"));

                    tempNpCreator.addProvenanceStatement(studyExperimentalSetting, bindingSetProv.getValue("Experimental"));
                    tempNpCreator.addProvenanceStatement((Resource)bindingSetProv.getValue("Experimental"), rdfsLabel, bindingSetProv.getValue("ExperimentalLabel"));
                  }


                  if(bindingSetProv.getValue("numberOfChoices") != null)
                  {
                    npCreator.addProvenanceStatement(studyNumberOfChoices, factory.createLiteral(Integer.parseInt(bindingSetProv.getValue("numberOfChoices").stringValue())));
                    tempNpCreator.addProvenanceStatement(studyNumberOfChoices, factory.createLiteral(Integer.parseInt(bindingSetProv.getValue("numberOfChoices").stringValue())));
                  }



                  if(bindingSetProv.getValue("kIndex") != null)
                  {
                      npCreator.addProvenanceStatement(studyKindex, factory.createLiteral(Double.parseDouble(bindingSetProv.getValue("kIndex").stringValue())));
                      tempNpCreator.addProvenanceStatement(studyKindex, factory.createLiteral(Double.parseDouble(bindingSetProv.getValue("kIndex").stringValue())));
                  }


                  if(bindingSetProv.getValue("mpcr") != null)
                      if(!bindingSetProv.getValue("mpcr").stringValue().equals(""))
                      {
                          for(String mpcr : bindingSetProv.getValue("mpcr").stringValue().split(","))
                          {
                             npCreator.addProvenanceStatement(studyMPCR, factory.createLiteral(Double.parseDouble(mpcr.trim())));
                             tempNpCreator.addProvenanceStatement(studyMPCR, factory.createLiteral(Double.parseDouble(mpcr.trim())));
                          }
                      }


                  if(bindingSetProv.getValue("maleProportion") != null)
                  {
                    npCreator.addProvenanceStatement(maleProportion,factory.createLiteral(Double.parseDouble(bindingSetProv.getValue("maleProportion").stringValue())));
                    tempNpCreator.addProvenanceStatement(maleProportion,factory.createLiteral(Double.parseDouble(bindingSetProv.getValue("maleProportion").stringValue())));
                  }


                  if(bindingSetProv.getValue("yearOfDataCollection") != null)
                  {
                    npCreator.addProvenanceStatement(yearOfDataCollection,factory.createLiteral(Integer.parseInt(bindingSetProv.getValue("yearOfDataCollection").stringValue())));
                    tempNpCreator.addProvenanceStatement(yearOfDataCollection,factory.createLiteral(Integer.parseInt(bindingSetProv.getValue("yearOfDataCollection").stringValue())));
                  }

            }
          }
        }catch(Exception ex)
        {
            System.out.println(ex.getMessage() + "\n\n" );
            repoProv.shutDown();
        }
        repoProv.shutDown();
    }
}
